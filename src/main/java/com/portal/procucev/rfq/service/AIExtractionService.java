package com.portal.procucev.rfq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.InlineImage;
import com.portal.procucev.rfq.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIExtractionService {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gemini.max-inline-images:5}")
    private int maxInlineImages = 5;

    @Value("${app.gemini.max-inline-image-bytes:5242880}")
    private long maxInlineImageBytes = 5242880L;

    @Value("${app.gemini.max-inline-image-total-bytes:15728640}")
    private long maxInlineImageTotalBytes = 15728640L;

    public ExtractedRFQ extractRFQFromEmail(EmailData email) {
        String subj = email.getSubject() != null ? email.getSubject() : "(No Subject)";
        String body = email.getBody() != null ? email.getBody() : "";
        String attText = email.getAttachmentText() != null ? email.getAttachmentText() : "";

        log.info("========== AI EXTRACTION START ==========");
        log.info("EMAIL SUBJECT: {}", subj);
        log.info("EMAIL BODY LENGTH: {}", body.length());
        log.info("EXTRACTION INPUT ATTACHMENT TEXT LENGTH: {}", attText.length());

        String prompt;
        try {
            String promptTemplate = loadPromptTemplate();
            prompt = promptTemplate
                    .replace("${subject}", subj)
                    .replace("${body}", body)
                    .replace("${attachmentText}", attText);
        } catch (Exception e) {
            log.error("Failed to load prompt template: {}", e.getMessage());
            throw new ApplicationException("AI extraction failed to load prompt template: " + e.getMessage(), e);
        }

        // Image attachments have no text layer, so they contribute nothing to attachmentText.
        // They are sent alongside the prompt so a requirement supplied as a screenshot or a
        // photographed purchase note is still read instead of being reported as detail-less.
        List<InlineImage> inlineImages = collectInlineImages(email);

        ExtractedRFQ extractedRFQ = null;
        Exception firstException = null;

        // First attempt
        try {
            String jsonResponse = geminiApiClient.generateContent(prompt, inlineImages);
            extractedRFQ = parseAndValidateJson(jsonResponse, email.getSenderEmail());
        } catch (Exception e) {
            firstException = e;
            log.warn("Primary AI extraction attempt failed for subject '{}': {}. Retrying once with strict JSON extraction...", subj, e.getMessage());
        }

        // Retry attempt if first attempt failed or yielded null
        if (extractedRFQ == null) {
            try {
                String retryPrompt = prompt + "\n\nCRITICAL: Return STRICT JSON ONLY. Do not include markdown code block syntax.";
                String jsonResponse = geminiApiClient.generateContent(retryPrompt, inlineImages);
                extractedRFQ = parseAndValidateJson(jsonResponse, email.getSenderEmail());
            } catch (Exception e) {
                log.error("Retry AI extraction attempt also failed for subject '{}': {}", subj, e.getMessage());
                String rootCauseMsg = firstException != null ? firstException.getMessage() : e.getMessage();
                throw new ApplicationException("AI extraction failed: " + rootCauseMsg, e);
            }
        }

        log.info("AI Extraction complete. Buyer: {}, Items Extracted: {}",
                extractedRFQ.getBuyerEmail(),
                extractedRFQ.getItems() != null ? extractedRFQ.getItems().size() : 0);

        return extractedRFQ;
    }

    /**
     * Gathers the image attachments worth sending inline, newest limits first.
     *
     * <p>Bounded on purpose: the request carries the images Base64 encoded, so an unbounded photo
     * album would blow the request size limit and fail an extraction that would otherwise have
     * succeeded on the body alone.
     */
    private List<InlineImage> collectInlineImages(EmailData email) {
        List<InlineImage> images = new ArrayList<>();
        if (email.getAttachments() == null || email.getAttachments().isEmpty()) {
            return images;
        }

        long totalBytes = 0L;
        for (File attachment : email.getAttachments()) {
            if (images.size() >= maxInlineImages) {
                log.warn("Reached the inline image limit of {}; further image attachments are not sent for extraction.",
                        maxInlineImages);
                break;
            }
            String mimeType = FileUtil.visionImageMimeType(attachment);
            if (mimeType == null) {
                continue;
            }
            long length = attachment.length();
            if (length > maxInlineImageBytes) {
                log.warn("Image attachment '{}' is {} bytes, above the {} byte inline limit. Skipping it.",
                        attachment.getName(), length, maxInlineImageBytes);
                continue;
            }
            if (totalBytes + length > maxInlineImageTotalBytes) {
                log.warn("Adding image attachment '{}' would exceed the {} byte inline budget. Skipping it.",
                        attachment.getName(), maxInlineImageTotalBytes);
                continue;
            }
            String base64 = FileUtil.readAsBase64(attachment);
            if (base64 == null) {
                continue;
            }
            totalBytes += length;
            images.add(new InlineImage(attachment.getName(), mimeType, base64));
        }

        if (!images.isEmpty()) {
            log.info("Sending {} image attachment(s) to the vision model for extraction: {}",
                    images.size(), images.stream().map(InlineImage::fileName).toList());
        }
        return images;
    }

    private ExtractedRFQ parseAndValidateJson(String jsonResponse, String fallbackSenderEmail) throws Exception {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            throw new ApplicationException("AI model returned an empty response");
        }

        String cleanedJson = sanitizeJsonOutput(jsonResponse);
        ExtractedRFQ extracted = objectMapper.readValue(cleanedJson, ExtractedRFQ.class);

        if (extracted == null) {
            throw new ApplicationException("Parsed JSON produced null ExtractedRFQ object");
        }

        if (extracted.getBuyerEmail() == null || extracted.getBuyerEmail().isBlank()) {
            extracted.setBuyerEmail(fallbackSenderEmail);
        }

        return extracted;
    }

    private String loadPromptTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource("prompts/rfq_prompt.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String sanitizeJsonOutput(String raw) {
        if (raw == null) return "{}";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        // Extract substring between first '{' and last '}' if extra text surrounds JSON
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1).trim();
        }

        return cleaned;
    }
}
