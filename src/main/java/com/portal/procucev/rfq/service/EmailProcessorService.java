package com.portal.procucev.rfq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.FailedRfqRequest;
import com.portal.procucev.rfq.dto.ProcessingStats;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.entity.RfqItemRecord;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProcessorService {

    private final EmailReaderService emailReaderService;
    private final AIExtractionService aiExtractionService;
    private final ValidationService validationService;
    private final BuyerVerificationService buyerVerificationService;
    private final RFQBuilderService rfqBuilderService;
    private final RFQApiService rfqApiService;
    private final CategoryClassificationService categoryClassificationService;
    private final AcknowledgementEmailService acknowledgementEmailService;
    private final RFQRepository rfqRepository;
    private final EmailTransactionRepository emailTransactionRepository;
    private final RfqItemRecordRepository rfqItemRecordRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.mail.processed-folder:Processed}")
    private String processedFolder;

    @Value("${app.mail.error-folder:Error}")
    private String errorFolder;

    public ProcessingStats processUnreadEmails() {
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        int errorCount = 0;
        int successCount = 0;

        log.info("========== STARTING EMAIL RFQ PROCESSING JOB (p2pservices_v1) ==========");

        List<EmailData> unreadEmails = emailReaderService.fetchUnreadEmails();
        if (unreadEmails == null) {
            unreadEmails = Collections.emptyList();
        }
        log.info("Processing {} unread email(s).", unreadEmails.size());


        for (EmailData email : unreadEmails) {
            processedCount++;
            try {
                String status = processSingleEmail(email);
                if (isSuccessfulStatus(status)) {
                    successCount++;
                } else if (isErrorStatus(status)) {
                    errorCount++;
                }
            } catch (Exception e) {
                errorCount++;
                log.error("Unhandled error processing email subject: '{}'", email.getSubject(), e);
            } finally {
                deleteTemporaryAttachments(email);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("========== COMPLETED EMAIL RFQ PROCESSING JOB ==========");

        return ProcessingStats.builder()
                .status(errorCount == 0 ? "SUCCESS" : "COMPLETED_WITH_ERRORS")
                .emailsProcessed(processedCount)
                .rfqsCreated(successCount)
                .errors(errorCount)
                .executionTime((endTime - startTime) / 1000 + " sec")
                .build();
    }

    @Transactional
    public String processSingleEmail(EmailData email) {
        log.info("--------------------------------------------------");
        String rawSender = email.getSenderEmail() != null ? email.getSenderEmail().trim() : "";
        String normalizedSender = rawSender.toLowerCase();
        log.info("Email received from: {}", normalizedSender);
        log.info("Processing Email Received: Message-ID [{}], Subject: '{}'", email.getMessageId(), email.getSubject());

        if (normalizedSender.contains("notification")
                || normalizedSender.equalsIgnoreCase("rfq@procucev.com")
                || normalizedSender.contains("invitations@procucev.com")
                || normalizedSender.contains("gmtrfq@procucev.com")
                || normalizedSender.contains("no-reply")
                || normalizedSender.contains("noreply")) {
            log.info("System notification sender email detected: '{}'. Ignored to prevent processing loops.", normalizedSender);
            emailReaderService.moveMessageToFolder(email.getMessageId(), processedFolder);
            return "SKIPPED_SYSTEM_EMAIL";
        }

        if (emailTransactionRepository.findByMessageId(email.getMessageId()).isPresent()) {
            log.warn("Duplicate Email detected (Message-ID: {}). Skipping.", email.getMessageId());
            emailReaderService.moveMessageToFolder(email.getMessageId(), processedFolder);
            return "SKIPPED";
        }

        EmailTransaction transaction = EmailTransaction.builder()
                .messageId(email.getMessageId())
                .subject(email.getSubject())
                .senderEmail(normalizedSender)
                .status("RECEIVED")
                .build();
        emailTransactionRepository.save(transaction);

        try {
            // STEP 1: VALIDATE BUYER FIRST BEFORE CALLING GEMINI AI
            log.info("Validating buyer for sender email: {}", normalizedSender);
            transaction.setStatus("VALIDATING_BUYER");
            emailTransactionRepository.save(transaction);

            Buyer buyer = buyerVerificationService.verifyAndGetBuyer(normalizedSender);

            if (buyer == null || !buyer.isVerified()) {
                log.warn("Buyer validation failed: sender email {} is not registered", normalizedSender);
                log.warn("Skipping email because sender is not a valid buyer");
                log.info("No Gemini AI call will be executed for unregistered sender.");

                acknowledgementEmailService.sendUnregisteredBuyerAcknowledgement(normalizedSender);
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);

                transaction.setStatus("INVALID_BUYER");
                transaction.setErrorMessage("Sender email is not registered as a buyer");
                emailTransactionRepository.save(transaction);
                return "INVALID_BUYER";
            }

            log.info("Buyer validation successful: buyerId={}, email={}", buyer.getUserId(), buyer.getEmail());

            // STEP 2: GEMINI AI EXTRACTION FOR VALIDATED BUYER
            log.info("Starting AI extraction for validated buyer: {}", buyer.getEmail());
            transaction.setStatus("AI_PROCESSING");
            emailTransactionRepository.save(transaction);

            ExtractedRFQ extractedRFQ;
            try {
                extractedRFQ = aiExtractionService.extractRFQFromEmail(email);
                log.info("AI extraction successful");
            } catch (Exception e) {
                log.error("AI extraction failed for validated buyer {}: {}", buyer.getEmail(), e.getMessage());
                log.warn("Email moved to Error folder because AI processing failed");

                FailedRfqRequest failedReq = buildFailedRequestFromEmail(email, null, "AI Extraction failed: " + e.getMessage());
                acknowledgementEmailService.sendFailureAcknowledgement(failedReq, buyer);
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);

                transaction.setStatus("AI_FAILED");
                transaction.setErrorMessage("AI Extraction failed: " + e.getMessage());
                emailTransactionRepository.save(transaction);
                return "AI_FAILED";
            }

            if (extractedRFQ == null) {
                log.error("AI extraction returned null for validated buyer {}", buyer.getEmail());
                FailedRfqRequest failedReq = buildFailedRequestFromEmail(email, null, "AI Extraction returned null");
                acknowledgementEmailService.sendFailureAcknowledgement(failedReq, buyer);
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);

                transaction.setStatus("AI_FAILED");
                transaction.setErrorMessage("AI Extraction returned null");
                emailTransactionRepository.save(transaction);
                return "AI_FAILED";
            }

            // STEP 3: VALIDATE EXTRACTED RFQ DATA
            extractedRFQ.setBuyerEmail(buyer.getEmail());
            ValidationService.ValidationResult valResult = validationService.validateWithDetails(extractedRFQ);

            if (!valResult.isValid()) {
                log.warn("RFQ validation failed for extracted data: {}", valResult.getFailureReason());

                if (valResult.isMissingQuantity()) {
                    acknowledgementEmailService.sendMissingQuantityAcknowledgement(buyer.getEmail(), buyer.getName(), valResult.getMissingItems());
                    transaction.setStatus("VALIDATION_FAILED");
                } else {
                    FailedRfqRequest failedReq = buildFailedRequestFromEmail(email, extractedRFQ, valResult.getFailureReason());
                    acknowledgementEmailService.sendFailureAcknowledgement(failedReq, buyer);
                    transaction.setStatus("VALIDATION_FAILED");
                }

                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);
                transaction.setErrorMessage(valResult.getFailureReason());
                emailTransactionRepository.save(transaction);
                return "VALIDATION_FAILED";
            }

            // STEP 4: LINE ITEM DEDUPLICATION WITHIN SAME EMAIL PAYLOAD
            if (extractedRFQ.getItems() == null || extractedRFQ.getItems().isEmpty()) {
                log.warn("RFQ validation failed: No valid line items remaining.");
                FailedRfqRequest failedReq = buildFailedRequestFromEmail(email, extractedRFQ, "No valid line items remaining for RFQ creation.");
                acknowledgementEmailService.sendFailureAcknowledgement(failedReq, buyer);
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);

                transaction.setStatus("VALIDATION_FAILED");
                transaction.setErrorMessage("No valid items extracted");
                emailTransactionRepository.save(transaction);
                return "VALIDATION_FAILED";
            }

            List<RFQItem> validItems = new ArrayList<>();
            Set<String> seenInEmailKeys = new HashSet<>();
            String topDeliveryDate = extractedRFQ.getDeliveryDate() != null ? extractedRFQ.getDeliveryDate().trim() : "";
            String topDeliveryLocation = extractedRFQ.getDeliveryLocation() != null ? extractedRFQ.getDeliveryLocation().trim() : "";

            for (RFQItem item : extractedRFQ.getItems()) {

                if (item == null || item.getItemDescription() == null || item.getItemDescription().isBlank()) {
                    continue;
                }
                String desc = item.getItemDescription().trim();
                String key = buildDeduplicationKey(item, buyer.getEmail(), topDeliveryDate, topDeliveryLocation);

                if (seenInEmailKeys.contains(key)) {
                    log.info("Duplicate RFQ Item within same email payload detected, combining line: {}", desc);
                    continue;
                }
                seenInEmailKeys.add(key);
                item.setItemDescription(desc);
                validItems.add(item);
            }

            if (validItems.isEmpty()) {
                log.warn("RFQ validation failed: No valid line items remaining.");
                FailedRfqRequest failedReq = buildFailedRequestFromEmail(email, extractedRFQ, "No valid line items remaining for RFQ creation.");
                acknowledgementEmailService.sendFailureAcknowledgement(failedReq, buyer);
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);

                transaction.setStatus("VALIDATION_FAILED");
                transaction.setErrorMessage("No valid items extracted");
                emailTransactionRepository.save(transaction);
                return "VALIDATION_FAILED";
            }

            // STEP 5: ITEM GROUPING, CLASSIFICATION & RFQ CREATION FOR VALIDATED BUYER
            Map<String, List<RFQItem>> itemGroups = groupItemsByLocationAndDate(validItems, topDeliveryLocation, topDeliveryDate);
            boolean atLeastOneSuccess = false;
            boolean hasFailures = false;

            for (Map.Entry<String, List<RFQItem>> groupEntry : itemGroups.entrySet()) {
                List<RFQItem> groupItems = groupEntry.getValue();

                String groupLocation = groupItems.get(0).getDeliveryLocation() != null
                        ? groupItems.get(0).getDeliveryLocation() : topDeliveryLocation;
                String groupDate = groupItems.get(0).getDeliveryDate() != null
                        ? groupItems.get(0).getDeliveryDate() : topDeliveryDate;

                ExtractedRFQ groupExtractedRFQ = ExtractedRFQ.builder()
                        .buyerEmail(buyer.getEmail())
                        .deliveryLocation(groupLocation)
                        .deliveryCity(extractedRFQ.getDeliveryCity())
                        .deliveryState(extractedRFQ.getDeliveryState())
                        .deliveryPincode(extractedRFQ.getDeliveryPincode())
                        .deliveryDate(groupDate)
                        .category(extractedRFQ.getCategory())
                        .items(groupItems)
                        .build();

                categoryClassificationService.classifyItems(groupItems, extractedRFQ.getCategory());
                for (RFQItem item : groupItems) {
                    log.info("Category assigned to item '{}': '{}' (Division: '{}')",
                            item.getItemDescription(), item.getCategory(), item.getDivision());
                }

                RFQRequest rfqRequest = rfqBuilderService.buildRFQRequest(groupExtractedRFQ, buyer, email.getSubject(), email.getAttachments());
                String generatedRfqNumber = rfqRequest.getRfqNumber();

                log.info("Creating RFQ for buyerId={}", buyer.getUserId());
                RFQResponse apiResponse = rfqApiService.submitRFQ(rfqRequest);

                if ("SUCCESS".equalsIgnoreCase(apiResponse.getStatus())) {
                    atLeastOneSuccess = true;
                    log.info("RFQ created successfully: rfqId={}", generatedRfqNumber);

                    RFQEntity rfqEntity = RFQEntity.builder()
                            .rfqNumber(generatedRfqNumber)
                            .buyerEmail(buyer.getEmail())
                            .status("SUCCESS")
                            .rawSubject(email.getSubject())
                            .itemsJson(objectMapper.writeValueAsString(groupItems))
                            .deliveryLocation(groupLocation)
                            .deliveryDate(rfqRequest.getDeliveryDate())
                            .build();

                    RFQEntity savedRfq = rfqRepository.save(rfqEntity);

                    for (RFQItem item : groupItems) {
                        try {
                            RfqItemRecord record = RfqItemRecord.builder()
                                    .buyerEmail(buyer.getEmail())
                                    .itemDescription(item.getItemDescription())
                                    .deliveryDate(groupDate)
                                    .rfqNumber(savedRfq.getRfqNumber())
                                    .category(item.getCategory())
                                    .division(item.getDivision())
                                    .categoryConfidence(item.getCategoryConfidence())
                                    .classificationStatus(item.getClassificationStatus())
                                    .build();
                            rfqItemRecordRepository.save(record);
                        } catch (Exception ex) {
                            log.warn("Could not save RFQ item record: {}", ex.getMessage());
                        }
                    }

                    acknowledgementEmailService.sendSuccessAcknowledgement(savedRfq, buyer);
                } else {
                    hasFailures = true;
                    log.warn("RFQ creation failed for RFQ Number: {}", generatedRfqNumber);
                    FailedRfqRequest failedReq = buildFailedRequestFromEmail(email, groupExtractedRFQ, "RFQ Creation error: " + apiResponse.getMessage());
                    acknowledgementEmailService.sendFailureAcknowledgement(failedReq, buyer);
                }
            }

            if (atLeastOneSuccess && !hasFailures) {
                emailReaderService.moveMessageToFolder(email.getMessageId(), processedFolder);
                transaction.setStatus("RFQ_CREATED");
                emailTransactionRepository.save(transaction);
                return "RFQ_CREATED";
            } else if (atLeastOneSuccess) {
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);
                transaction.setStatus("PARTIAL_FAILURE");
                transaction.setErrorMessage("One or more grouped RFQs failed after at least one RFQ was created.");
                emailTransactionRepository.save(transaction);
                return "PARTIAL_FAILURE";
            } else {
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);
                transaction.setStatus("FAILED");
                transaction.setErrorMessage("RFQ Creation failed");
                emailTransactionRepository.save(transaction);
                return "FAILED";
            }

        } catch (Exception e) {
            log.error("Job Failed for email '{}': {}", email.getSubject(), e.getMessage(), e);
            transaction.setStatus("FAILED");
            transaction.setErrorMessage(e.getMessage());
            emailTransactionRepository.save(transaction);
            try {
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);
            } catch (Exception ex) {
                log.error("Failed to move email to error folder: {}", ex.getMessage());
            }
            return "FAILED";
        }
    }

    private boolean isSuccessfulStatus(String status) {
        return "RFQ_CREATED".equalsIgnoreCase(status);
    }

    private boolean isErrorStatus(String status) {
        return Set.of("FAILED", "PARTIAL_FAILURE", "AI_FAILED", "VALIDATION_FAILED", "INVALID_BUYER")
                .contains(status);
    }

    private void deleteTemporaryAttachments(EmailData email) {
        if (email.getAttachments() == null) {
            return;
        }
        for (java.io.File attachment : email.getAttachments()) {
            try {
                java.nio.file.Files.deleteIfExists(attachment.toPath());
            } catch (Exception e) {
                log.warn("Could not delete temporary attachment {}: {}", attachment, e.getMessage());
            }
        }
    }

    private String buildDeduplicationKey(RFQItem item, String buyerEmail, String defaultDate, String defaultLocation) {
        return String.join("|",
                normalizeValue(buyerEmail),
                normalizeValue(item.getItemDescription()),
                normalizeValue(item.getDeliveryDate() != null ? item.getDeliveryDate() : defaultDate),
                normalizeValue(item.getDeliveryLocation() != null ? item.getDeliveryLocation() : defaultLocation),
                normalizeValue(item.getUom()),
                normalizeValue(item.getEffectivePartNumber()),
                normalizeValue(item.getSpecification()),
                normalizeValue(item.getBrand()),
                item.getQuantity() != null ? item.getQuantity().toString() : "");
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private Map<String, List<RFQItem>> groupItemsByLocationAndDate(List<RFQItem> items, String defaultLoc, String defaultDate) {
        Map<String, List<RFQItem>> groups = new LinkedHashMap<>();
        for (RFQItem item : items) {
            String loc = item.getDeliveryLocation() != null && !item.getDeliveryLocation().isBlank()
                    ? item.getDeliveryLocation().trim() : defaultLoc;
            String date = item.getDeliveryDate() != null && !item.getDeliveryDate().isBlank()
                    ? item.getDeliveryDate().trim() : defaultDate;

            String groupKey = loc.toLowerCase() + "|" + date.toLowerCase();
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    private FailedRfqRequest buildFailedRequestFromEmail(EmailData email, ExtractedRFQ extractedRFQ, String reason) {
        String desc = "Not Provided";
        String partCode = "Not Provided";
        String spec = "Not Provided";
        String brand = "Not Provided";
        String qty = "Not Provided";
        String uom = "Not Provided";
        String loc = "Not Provided";
        String date = "Not Provided";

        if (extractedRFQ != null) {
            if (extractedRFQ.getDeliveryLocation() != null && !extractedRFQ.getDeliveryLocation().isBlank()) {
                loc = extractedRFQ.getDeliveryLocation();
            }
            if (extractedRFQ.getDeliveryDate() != null && !extractedRFQ.getDeliveryDate().isBlank()) {
                date = extractedRFQ.getDeliveryDate();
            }
            if (extractedRFQ.getItems() != null && !extractedRFQ.getItems().isEmpty()) {
                RFQItem first = extractedRFQ.getItems().get(0);
                if (first.getItemDescription() != null && !first.getItemDescription().isBlank()) desc = first.getItemDescription();
                if (first.getEffectivePartNumber() != null && !first.getEffectivePartNumber().isBlank()) partCode = first.getEffectivePartNumber();
                if (first.getSpecification() != null && !first.getSpecification().isBlank()) spec = first.getSpecification();
                if (first.getBrand() != null && !first.getBrand().isBlank()) brand = first.getBrand();
                if (first.getQuantity() != null && first.getQuantity() > 0) qty = String.valueOf(first.getQuantity().intValue());
                if (first.getUom() != null && !first.getUom().isBlank()) uom = first.getUom();
            }
        }

        return FailedRfqRequest.builder()
                .buyerEmail(extractedRFQ != null && extractedRFQ.getBuyerEmail() != null ? extractedRFQ.getBuyerEmail() : email.getSenderEmail())
                .buyerName("Valued Buyer")
                .rawSubject(email.getSubject())
                .description(desc)
                .partNumber(partCode)
                .specification(spec)
                .brand(brand)
                .quantity(qty)
                .uom(uom)
                .deliveryLocation(loc)
                .deliveryDate(date)
                .reasonForFailure(reason)
                .processingReference(email.getMessageId())
                .build();
    }
}
