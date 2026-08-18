package com.portal.procucev.rfq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.exception.ApplicationException;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.InlineImage;
import com.portal.procucev.rfq.model.RFQItem;
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

    /**
     * Total extraction attempts. The model is probabilistic, so one answer leaving a quantity out is
     * not evidence the buyer omitted it. Re-asking, naming the gap, recovers most of them.
     */
    @Value("${app.gemini.extraction-max-attempts:3}")
    private int extractionMaxAttempts = 3;

    @Value("${app.gemini.max-logged-response-chars:4000}")
    private int maxLoggedResponseChars = 4000;

    public ExtractedRFQ extractRFQFromEmail(EmailData email) {
        String subj = email.getSubject() != null ? email.getSubject() : "(No Subject)";
        String body = email.getBody() != null ? email.getBody() : "";
        String attText = email.getAttachmentText() != null ? email.getAttachmentText() : "";

        log.info("========== AI EXTRACTION START ==========");
        log.info("EMAIL SUBJECT: {}", subj);
        log.info("EMAIL BODY LENGTH: {}", body.length());
        log.info("EXTRACTION INPUT ATTACHMENT TEXT LENGTH: {}", attText.length());
        log.info("EXTRACTION INPUT BODY: {}", abbreviate(body));
        if (!attText.isBlank()) {
            log.info("EXTRACTION INPUT ATTACHMENT TEXT: {}", abbreviate(attText));
        }

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

        int maxAttempts = Math.max(1, extractionMaxAttempts);
        ExtractedRFQ best = null;
        List<String> gaps = new ArrayList<>();
        Exception lastFailure = null;
        boolean previousAttemptThrew = false;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String attemptPrompt = prompt;
            if (attempt > 1) {
                // A parse failure needs a formatting nudge; an incomplete answer needs to be told
                // exactly which details it left out. Re-asking blindly tends to reproduce the same
                // omission, because the model has no idea it missed anything.
                attemptPrompt += previousAttemptThrew
                        ? "\n\nCRITICAL: Return STRICT JSON ONLY. Do not include markdown code block syntax."
                        : buildCorrectiveInstruction(gaps);
            }

            try {
                log.info("AI extraction attempt {}/{} for subject '{}'...", attempt, maxAttempts, subj);
                String jsonResponse = geminiApiClient.generateContent(attemptPrompt, inlineImages);
                logRawModelResponse(attempt, jsonResponse);
                ExtractedRFQ candidate = parseAndValidateJson(jsonResponse, email.getSenderEmail());
                logAttemptResult(attempt, candidate);
                best = best == null ? candidate : mergeBetterResult(best, candidate, attempt);
                previousAttemptThrew = false;
            } catch (Exception e) {
                lastFailure = e;
                previousAttemptThrew = true;
                log.warn("AI extraction attempt {}/{} failed for subject '{}': {}", attempt, maxAttempts, subj, e.getMessage());
                continue;
            }

            gaps = describeGaps(best);
            if (gaps.isEmpty()) {
                log.info("AI extraction attempt {}/{} produced a complete result. No further attempts needed.", attempt, maxAttempts);
                break;
            }
            if (attempt < maxAttempts) {
                log.warn("AI extraction attempt {}/{} left {} mandatory detail(s) unresolved: {}. Re-asking the model for these specifically.",
                        attempt, maxAttempts, gaps.size(), gaps);
            } else {
                log.warn("After {} attempt(s) these mandatory detail(s) are still unresolved and are treated as genuinely absent from the email: {}",
                        maxAttempts, gaps);
            }
        }

        if (best == null) {
            String rootCause = lastFailure != null ? lastFailure.getMessage() : "no parseable response";
            log.error("AI extraction failed after {} attempt(s) for subject '{}': {}", maxAttempts, subj, rootCause);
            throw new ApplicationException("AI extraction failed: " + rootCause, lastFailure);
        }

        logExtractionSummary(best);
        return best;
    }

    /**
     * Lists the mandatory details the model has still not supplied.
     *
     * <p>Only quantity and the item name are mandatory. Delivery location and date both have a
     * buyer-profile fallback, so re-asking for them would burn attempts on details that never block
     * an RFQ.
     */
    private List<String> describeGaps(ExtractedRFQ extracted) {
        List<String> gaps = new ArrayList<>();
        if (extracted.getItems() == null || extracted.getItems().isEmpty()) {
            gaps.add("no line items were extracted from the email");
            return gaps;
        }
        for (int i = 0; i < extracted.getItems().size(); i++) {
            RFQItem item = extracted.getItems().get(i);
            if (item == null) {
                gaps.add("item " + (i + 1) + ": the whole item is null");
                continue;
            }
            String label = "item " + (i + 1)
                    + (isBlank(item.getItemDescription()) ? "" : " (" + item.getItemDescription().trim() + ")");
            if (isBlank(item.getItemDescription())) {
                gaps.add(label + ": itemDescription");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                gaps.add(label + ": quantity");
            }
        }
        return gaps;
    }

    /** The follow-up instruction that names the exact gaps and the layouts they usually hide in. */
    private String buildCorrectiveInstruction(List<String> gaps) {
        return "\n\n========================================================\n"
                + "RE-READ: YOUR PREVIOUS ANSWER TO THIS EMAIL WAS INCOMPLETE\n"
                + "========================================================\n"
                + "You already answered for this exact email and left these mandatory details unresolved:\n"
                + "  - " + String.join("\n  - ", gaps) + "\n\n"
                + "Read the email again and concentrate on those. A detail you skipped is usually present but\n"
                + "written in a layout you passed over. Check every one of these:\n"
                + "  - a number after a dash or en dash: \"Plain Washers M10 - 1,000 Nos\"\n"
                + "  - a labelled field block: \"Description: Laptop\" / \"Quantity: 25\" / \"UOM: Nos\"\n"
                + "  - a label block collapsed onto one line: \"Description: Laptop Quantity: 25 UOM: Nos\"\n"
                + "  - a table row or column, where the quantity sits in its own cell\n"
                + "  - a number BEFORE the item name: \"500 Nos of Plain Washers M10\", \"we require 10 laptops\"\n"
                + "  - text inside an attached image\n"
                + "A thousands separator means a LARGE number: \"1,000\" is 1000, never 1.\n"
                + "A unit with no number is impossible: if you returned uom \"Nos\" you read \"<number> Nos\",\n"
                + "so go back and find that number.\n\n"
                + "Return the COMPLETE JSON object for the whole email again, with the same items in the same\n"
                + "order. Keep every value you already had. Leave a detail null ONLY if it is genuinely absent.";
    }

    /**
     * Combines a fresh answer with the best one so far, so a value found on any attempt is kept.
     *
     * <p>A dropped line item is silent data loss, whereas a missing quantity is at least reported to
     * the buyer, so an answer with more line items always wins outright.
     */
    private ExtractedRFQ mergeBetterResult(ExtractedRFQ best, ExtractedRFQ candidate, int attempt) {
        int bestCount = best.getItems() != null ? best.getItems().size() : 0;
        int candidateCount = candidate.getItems() != null ? candidate.getItems().size() : 0;

        if (candidateCount > bestCount) {
            log.info("Attempt {} returned {} line item(s) against {} previously, so it replaces the earlier answer.",
                    attempt, candidateCount, bestCount);
            fillMissingTopLevel(candidate, best);
            return candidate;
        }
        if (candidateCount < bestCount) {
            log.warn("Attempt {} returned fewer line item(s) ({} against {}). Keeping the earlier answer; per-item "
                    + "values are not merged because the lists cannot be aligned safely.",
                    attempt, candidateCount, bestCount);
            fillMissingTopLevel(best, candidate);
            return best;
        }

        for (int i = 0; i < bestCount; i++) {
            mergeMissingItemFields(best.getItems().get(i), candidate.getItems().get(i), attempt);
        }
        fillMissingTopLevel(best, candidate);
        return best;
    }

    /** Fills gaps in one item from a later answer, refusing to merge across two different items. */
    private void mergeMissingItemFields(RFQItem target, RFQItem source, int attempt) {
        if (target == null || source == null) {
            return;
        }
        if (isBlank(target.getItemDescription()) && !isBlank(source.getItemDescription())) {
            log.info("Attempt {} supplied a missing itemDescription: '{}'", attempt, source.getItemDescription());
            target.setItemDescription(source.getItemDescription());
        } else if (!sameItem(target.getItemDescription(), source.getItemDescription())) {
            // Guard against a re-ordered answer moving one product's quantity onto another.
            log.warn("Attempt {} returned item '{}' where the previous answer had '{}'. Values are NOT merged "
                    + "between differing items.", attempt, source.getItemDescription(), target.getItemDescription());
            return;
        }

        if ((target.getQuantity() == null || target.getQuantity() <= 0)
                && source.getQuantity() != null && source.getQuantity() > 0) {
            log.info("Attempt {} recovered the missing quantity for item '{}': {}",
                    attempt, target.getItemDescription(), source.getQuantity());
            target.setQuantity(source.getQuantity());
        }
        if (isBlank(target.getUom()) && !isBlank(source.getUom())) {
            target.setUom(source.getUom());
        }
        if (isBlank(target.getSpecification()) && !isBlank(source.getSpecification())) {
            target.setSpecification(source.getSpecification());
        }
        if (isBlank(target.getBrand()) && !isBlank(source.getBrand())) {
            target.setBrand(source.getBrand());
        }
        if (isBlank(target.getPartCode()) && !isBlank(source.getPartCode())) {
            target.setPartCode(source.getPartCode());
        }
        if (isBlank(target.getDeliveryLocation()) && !isBlank(source.getDeliveryLocation())) {
            target.setDeliveryLocation(source.getDeliveryLocation());
        }
        if (isBlank(target.getDeliveryDate()) && !isBlank(source.getDeliveryDate())) {
            target.setDeliveryDate(source.getDeliveryDate());
        }
    }

    private void fillMissingTopLevel(ExtractedRFQ target, ExtractedRFQ source) {
        if (isBlank(target.getDeliveryLocation()) && !isBlank(source.getDeliveryLocation())) {
            target.setDeliveryLocation(source.getDeliveryLocation());
        }
        if (isBlank(target.getDeliveryCity()) && !isBlank(source.getDeliveryCity())) {
            target.setDeliveryCity(source.getDeliveryCity());
        }
        if (isBlank(target.getDeliveryState()) && !isBlank(source.getDeliveryState())) {
            target.setDeliveryState(source.getDeliveryState());
        }
        if (isBlank(target.getDeliveryPincode()) && !isBlank(source.getDeliveryPincode())) {
            target.setDeliveryPincode(source.getDeliveryPincode());
        }
        if (isBlank(target.getDeliveryDate()) && !isBlank(source.getDeliveryDate())) {
            target.setDeliveryDate(source.getDeliveryDate());
        }
        if (isBlank(target.getCategory()) && !isBlank(source.getCategory())) {
            target.setCategory(source.getCategory());
        }
        if (isBlank(target.getBuyerEmail()) && !isBlank(source.getBuyerEmail())) {
            target.setBuyerEmail(source.getBuyerEmail());
        }
    }

    private boolean sameItem(String left, String right) {
        if (isBlank(left) || isBlank(right)) {
            return false;
        }
        return normaliseForComparison(left).equals(normaliseForComparison(right));
    }

    private String normaliseForComparison(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value)
                || "Not Specified".equalsIgnoreCase(value);
    }

    /**
     * Logs the model's answer verbatim.
     *
     * <p>Only the post-processed object was ever recorded, so when an RFQ came out wrong there was
     * no way to tell a model misread from this pipeline mangling a correct answer.
     */
    private void logRawModelResponse(int attempt, String raw) {
        if (raw == null) {
            log.warn("AI extraction attempt {}: model returned a null response body.", attempt);
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= maxLoggedResponseChars) {
            log.info("AI RAW RESPONSE (attempt {}, {} chars): {}", attempt, trimmed.length(), trimmed);
        } else {
            log.info("AI RAW RESPONSE (attempt {}, {} chars, first {} shown): {}",
                    attempt, trimmed.length(), maxLoggedResponseChars, trimmed.substring(0, maxLoggedResponseChars));
        }
    }

    private void logAttemptResult(int attempt, ExtractedRFQ candidate) {
        int itemCount = candidate.getItems() != null ? candidate.getItems().size() : 0;
        log.info("AI attempt {} parsed successfully: {} item(s), location='{}', date='{}'",
                attempt, itemCount, candidate.getDeliveryLocation(), candidate.getDeliveryDate());
    }

    /** The model's answer as finally accepted, before any downstream recovery or defaulting. */
    private void logExtractionSummary(ExtractedRFQ extracted) {
        int itemCount = extracted.getItems() != null ? extracted.getItems().size() : 0;
        log.info("AI Extraction complete. Buyer: {}, Items Extracted: {}", extracted.getBuyerEmail(), itemCount);
        log.info("AI FINAL top-level: location='{}', city='{}', state='{}', pincode='{}', date='{}', category='{}'",
                extracted.getDeliveryLocation(), extracted.getDeliveryCity(), extracted.getDeliveryState(),
                extracted.getDeliveryPincode(), extracted.getDeliveryDate(), extracted.getCategory());
        for (int i = 0; i < itemCount; i++) {
            RFQItem item = extracted.getItems().get(i);
            if (item == null) {
                log.warn("AI FINAL item[{}]: null", i + 1);
                continue;
            }
            log.info("AI FINAL item[{}]: desc='{}', qty={}, uom='{}', spec='{}', brand='{}', partCode='{}', loc='{}', date='{}'",
                    i + 1, item.getItemDescription(), item.getQuantity(), item.getUom(), item.getSpecification(),
                    item.getBrand(), item.getEffectivePartNumber(), item.getDeliveryLocation(), item.getDeliveryDate());
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String oneLine = value.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= maxLoggedResponseChars
                ? oneLine
                : oneLine.substring(0, maxLoggedResponseChars) + "...[truncated]";
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
