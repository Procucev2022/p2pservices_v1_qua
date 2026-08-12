package com.portal.procucev.rfq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIExtractionService {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExtractedRFQ extractRFQFromEmail(EmailData email) {
        log.info("Extracting RFQ data using Gemini AI for email subject: '{}'", email.getSubject());

        try {
            String promptTemplate = loadPromptTemplate();

            String prompt = promptTemplate
                    .replace("${subject}", email.getSubject() != null ? email.getSubject() : "")
                    .replace("${body}", email.getBody() != null ? email.getBody() : "")
                    .replace("${attachmentText}", email.getAttachmentText() != null ? email.getAttachmentText() : "");

            String jsonResponse = geminiApiClient.generateContent(prompt);
            log.debug("Raw Gemini AI Response: {}", jsonResponse);

            String cleanedJson = sanitizeJsonOutput(jsonResponse);
            ExtractedRFQ extractedRFQ = objectMapper.readValue(cleanedJson, ExtractedRFQ.class);

            if (extractedRFQ.getBuyerEmail() == null || extractedRFQ.getBuyerEmail().isBlank()) {
                extractedRFQ.setBuyerEmail(email.getSenderEmail());
            }

            log.info("AI Extraction complete. Buyer: {}, Items Extracted: {}",
                    extractedRFQ.getBuyerEmail(),
                    extractedRFQ.getItems() != null ? extractedRFQ.getItems().size() : 0);

            return extractedRFQ;

        } catch (Exception e) {
            log.error("Failed to extract RFQ from email '{}': {}", email.getSubject(), e.getMessage(), e);
            throw new ApplicationException("AI Extraction process failed", e);
        }
    }

    private String loadPromptTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource("prompts/rfq_prompt.txt");
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String sanitizeJsonOutput(String raw) {
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
        return cleaned.trim();
    }
}
