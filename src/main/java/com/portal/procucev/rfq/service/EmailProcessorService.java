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
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import com.portal.procucev.rfq.util.QuantityNormalizer;
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
    private final DateParser dateParser;
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
                || normalizedSender.equalsIgnoreCase("rfqprocucev@gmail.com")
                || normalizedSender.equalsIgnoreCase("veerababu.v@procucev.com")
                || normalizedSender.contains("invitations@procucev.com")
                || normalizedSender.contains("gmtrfq@procucev.com")
                || normalizedSender.contains("no-reply")
                || normalizedSender.contains("noreply")) {
            log.info("System notification sender email detected: '{}'. Ignored to prevent processing loops.", normalizedSender);
            emailReaderService.moveMessageToFolder(email.getMessageId(), processedFolder);
            return "SKIPPED_SYSTEM_EMAIL";
        }

        EmailTransaction transaction = emailTransactionRepository.findByMessageId(email.getMessageId())
                .orElseGet(() -> EmailTransaction.builder()
                        .messageId(email.getMessageId())
                        .build());
        transaction.setSubject(email.getSubject());
        transaction.setSenderEmail(normalizedSender);
        transaction.setStatus("RECEIVED");
        transaction.setErrorMessage(null);
        emailTransactionRepository.save(transaction);

        if (email.isFileSizeExceeded()) {
            log.warn("Email attachment size limit exceeded for sender '{}', messageId=[{}]: {}",
                    normalizedSender, email.getMessageId(), email.getErrorMessage());
            transaction.setStatus("FILE_SIZE_EXCEEDED");
            transaction.setErrorMessage(email.getErrorMessage());
            emailTransactionRepository.save(transaction);

            Buyer buyer = null;
            try {
                buyer = buyerVerificationService.verifyAndGetBuyer(normalizedSender);
            } catch (Exception ignored) {}

            String buyerName = buyer != null ? buyer.getName() : null;
            acknowledgementEmailService.sendFileSizeExceededAcknowledgement(
                    normalizedSender, buyerName, email.getFailedAttachmentName(), 26214400L);
            emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);
            return "FILE_SIZE_EXCEEDED";
        }

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
                extractedRFQ = mergeThreadContext(email, extractedRFQ);
                transaction.setExtractionJson(objectMapper.writeValueAsString(extractedRFQ));
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

            // STEP 3: VALIDATE & CLASSIFY ITEMS INDEPENDENTLY
            if (extractedRFQ.getItems() == null || extractedRFQ.getItems().isEmpty()) {
                String fallbackDesc = extractProductFromSubject(email.getSubject());
                if (!fallbackDesc.isBlank()) {
                    log.info("Attempting Subject-based Fallback extraction for subject: '{}'", email.getSubject());
                    RFQItem fallbackItem = RFQItem.builder()
                            .itemDescription(fallbackDesc)
                            .quantity(parseQuantityFromText(email.getBody()))
                            .deliveryLocation(extractedRFQ.getDeliveryLocation())
                            .deliveryDate(extractedRFQ.getDeliveryDate())
                            .build();
                    extractedRFQ.setItems(new ArrayList<>(List.of(fallbackItem)));
                }
            }

            if (extractedRFQ.getItems() == null || extractedRFQ.getItems().isEmpty()) {
                log.warn("RFQ validation failed: No line items extracted.");
                FailedRfqRequest failedReq = buildFailedRequestFromEmail(email, extractedRFQ, "No line items extracted from email.");
                acknowledgementEmailService.sendFailureAcknowledgement(failedReq, buyer);
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);

                transaction.setStatus("VALIDATION_FAILED");
                transaction.setErrorMessage("No line items extracted");
                emailTransactionRepository.save(transaction);
                return "VALIDATION_FAILED";
            }

            List<RFQItem> validItems = new ArrayList<>();
            List<String> failedItemsList = new ArrayList<>();
            Set<String> seenInEmailKeys = new HashSet<>();
            String topDeliveryDate = extractedRFQ.getDeliveryDate() != null ? extractedRFQ.getDeliveryDate().trim() : "";
            String topDeliveryLocation = extractedRFQ.getDeliveryLocation() != null ? extractedRFQ.getDeliveryLocation().trim() : "";

            // Calculate default date (Current Date + 5 Days) if missing
            String defaultDate = (topDeliveryDate != null && !topDeliveryDate.isBlank() && !topDeliveryDate.equalsIgnoreCase("null") && !topDeliveryDate.equalsIgnoreCase("Not Specified"))
                    ? topDeliveryDate
                    : java.time.LocalDate.now().plusDays(5).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Recover a stated delivery location before falling back to the buyer's registered
            // address. Without this, a location supplied only in an attachment was silently
            // replaced by the profile address whenever the model returned no top-level value.
            if (topDeliveryLocation.isBlank() || topDeliveryLocation.equalsIgnoreCase("Not Specified")
                    || topDeliveryLocation.equalsIgnoreCase("null")
                    || topDeliveryLocation.equalsIgnoreCase("Registered Profile Address")) {
                String scannedTopLoc = scanFieldFromEmail(email, "(?i)(?:delivery\\s+location|delivery\\s+address|ship\\s+to|deliver\\s+to|destination)[:\\s=]*([^\\r\\n]+)");
                if (scannedTopLoc != null && !scannedTopLoc.isBlank()) {
                    log.info("Top-level delivery location recovered by fallback scan: '{}'", scannedTopLoc);
                    topDeliveryLocation = scannedTopLoc;
                    extractedRFQ.setDeliveryLocation(scannedTopLoc);
                }
            }

            String defaultLocation = resolveDeliveryLocation(topDeliveryLocation, buyer);
            log.info("Resolved delivery location: source={}, top-level='{}', effective default='{}'",
                    topDeliveryLocation.isBlank() ? "BUYER_PROFILE" : "EMAIL", topDeliveryLocation, defaultLocation);

            // Delivery location is NOT mandatory. When the buyer states one we use it; when they
            // do not, the RFQ is still created against their registered profile location, which
            // resolveDeliveryLocation and RFQBuilderService fall back to. This is recorded only so
            // the log shows which source supplied the location.
            boolean hasLocationInEmail = hasExplicitLocationInPayload(extractedRFQ, email);
            if (!hasLocationInEmail) {
                log.info("No delivery location stated in the email; falling back to the buyer's registered profile location.");
            }

            for (RFQItem item : extractedRFQ.getItems()) {
                if (item == null) {
                    continue;
                }
                String desc = item.getItemDescription() != null && !item.getItemDescription().isBlank()
                        ? item.getItemDescription().trim() : extractProductFromSubject(email.getSubject());

                if (desc.isBlank() || desc.equalsIgnoreCase("RFQ Procurement Item")) {
                    String scannedProduct = stripTrailingLabels(scanFieldFromEmail(email, PRODUCT_LABEL_REGEX));
                    if (scannedProduct != null && !scannedProduct.isBlank()) {
                        desc = scannedProduct.trim();
                    }
                }
                if (desc.isBlank()) {
                    log.warn("Item has no description in payload, subject, or email body. Skipping item.");
                    failedItemsList.add("Item has no description");
                    continue;
                }
                item.setItemDescription(desc);

                boolean isMultiItemPayload = extractedRFQ.getItems().size() > 1;

                // Repair mis-filed extraction: the model sometimes drops the whole requirement
                // sentence into "brand" and leaves "specification" empty. Run this before the
                // fallback scans so the salvaged text counts as a present specification.
                salvageSpecificationFromBrand(item, desc);

                // Fallback scan for specifications if missing (only for single item or item-specific text)
                if (item.getSpecification() == null || item.getSpecification().isBlank() || item.getSpecification().equalsIgnoreCase("Not Specified")) {
                    if (!isMultiItemPayload) {
                        String scannedSpec = scanFieldFromEmail(email, "(?i)\\b(?:technical\\s+specifications?|specifications?|specs|configuration|config)\\b[:\\s=]*([^\\r\\n]+)");
                        // A line-tail capture after the label often runs straight into the next
                        // field, e.g. "specification. Delivery Location: Bangalore". Reject that
                        // rather than persist another field's label as the specification.
                        if (scannedSpec != null && !scannedSpec.isBlank() && isPlausibleSpecification(scannedSpec)) {
                            item.setSpecification(scannedSpec.trim());
                        }
                    }
                }

                // Fallback scan for brand if missing (only for single item or item-specific text)
                if (item.getBrand() == null || item.getBrand().isBlank() || item.getBrand().equalsIgnoreCase("Not Specified")) {
                    if (!isMultiItemPayload) {
                        String scannedBrand = scanFieldFromEmail(email, "(?i)\\b(?:brand|make|manufacturer)\\b[:\\s=]*([^\\r\\n]+)");
                        // Only accept a scanned value that actually reads like a brand name; the
                        // line-tail capture otherwise pulls in whole sentences.
                        if (scannedBrand != null && !scannedBrand.isBlank() && looksLikeBrandName(scannedBrand)) {
                            item.setBrand(scannedBrand.trim());
                        }
                    }
                }

                // Fallback scan for delivery location if missing
                if (item.getDeliveryLocation() == null || item.getDeliveryLocation().isBlank()
                        || item.getDeliveryLocation().equalsIgnoreCase("Not Specified")
                        || item.getDeliveryLocation().equalsIgnoreCase("null")) {
                    String scannedLoc = scanFieldFromEmail(email, "(?i)(?:delivery\\s+location|delivery\\s+address|ship\\s+to|deliver\\s+to|location|plant|warehouse|address)[:\\s=]*([^\\r\\n]+)");
                    if (scannedLoc != null && !scannedLoc.isBlank()) {
                        log.info("Delivery location recovered by fallback scan for item '{}': '{}'", desc, scannedLoc);
                        item.setDeliveryLocation(scannedLoc);
                    }
                }

                // Fallback scan for delivery date if missing
                if (item.getDeliveryDate() == null || item.getDeliveryDate().isBlank() || item.getDeliveryDate().equalsIgnoreCase("Not Specified")) {
                    String scannedDate = scanFieldFromEmail(email, "(?i)(?:required\\s+delivery\\s+date|delivery\\s+date|deliverydate|date)[:\\s=]*([^\\r\\n]+)");
                    if (scannedDate != null && !scannedDate.isBlank()) {
                        item.setDeliveryDate(scannedDate.trim());
                    }
                }

                log.info("DEBUG Thread Processing: Source Email MsgID={}, ThreadID={}, Latest Reply MsgID={}", email.getMessageId(), email.getInReplyTo(), email.getMessageId());
                log.info("DEBUG Extracted Data: Item='{}', Quantity={}, UOM='{}', Specification='{}', Brand='{}', PartCode='{}', Raw Location='{}', Raw Date='{}'",
                        desc, item.getQuantity(), item.getUom(), item.getSpecification(), item.getBrand(),
                        item.getEffectivePartNumber(), item.getDeliveryLocation(), item.getDeliveryDate());

                // Recover a quantity the buyer wrote directly beside THIS item's name, e.g. a
                // run-on requirement sentence or table row such as "Plain Washers M10 - 1,000 Nos".
                // The model reads the unit but intermittently drops the number in that layout, and
                // because the match is anchored on the item's own name this is safe to run for
                // multi-item payloads too - which is where the whole-email scan below cannot help.
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    QuantityNormalizer.QuantityMatch perItemQty = scanQuantityForItem(email, desc);
                    if (perItemQty != null) {
                        log.info("Quantity recovered beside item name for item '{}': {} {}",
                                desc, perItemQty.quantity(), perItemQty.uom() != null ? perItemQty.uom() : "");
                        item.setQuantity(perItemQty.quantity());
                        if (perItemQty.uom() != null && (item.getUom() == null || item.getUom().isBlank())) {
                            item.setUom(perItemQty.uom());
                        }
                    }
                }

                // Recover an explicitly stated quantity the model failed to return. A spec-dense
                // email ("16 GB RAM, 512 GB SSD, 21.5-inch monitor") can push the model into
                // returning null even when the buyer wrote "a quantity of 15 Nos". Single-item
                // payloads only: one quantity in the covering note must not be applied to every
                // row of a multi-item requirement sheet.
                if ((item.getQuantity() == null || item.getQuantity() <= 0) && !isMultiItemPayload) {
                    Double scannedQty = scanQuantityFromEmail(email);
                    if (scannedQty != null && scannedQty > 0) {
                        log.info("Quantity recovered by fallback scan for item '{}': {}", desc, scannedQty);
                        item.setQuantity(scannedQty);
                    }
                }

                // Default quantity to 1.0 (and UOM to "Nos") if missing after extraction and recovery
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    log.info("No explicit quantity stated for item '{}'. Defaulting quantity to 1.0.", desc);
                    item.setQuantity(1.0);
                }
                if (item.getUom() == null || item.getUom().isBlank() || item.getUom().equalsIgnoreCase("null") || item.getUom().equalsIgnoreCase("Not Specified")) {
                    item.setUom("Nos");
                }

                // Resolve item-level location & date fallbacks with ISO yyyy-MM-dd normalization
                String itemLoc = (item.getDeliveryLocation() != null && !item.getDeliveryLocation().isBlank() && !item.getDeliveryLocation().equalsIgnoreCase("Not Specified") && !item.getDeliveryLocation().equalsIgnoreCase("null"))
                        ? item.getDeliveryLocation().trim() : defaultLocation;
                item.setDeliveryLocation(itemLoc);

                String rawItemDate = (item.getDeliveryDate() != null && !item.getDeliveryDate().isBlank() && !item.getDeliveryDate().equalsIgnoreCase("Not Specified") && !item.getDeliveryDate().equalsIgnoreCase("null"))
                        ? item.getDeliveryDate().trim() : defaultDate;
                String itemDate = dateParser.parseDateString(rawItemDate);
                item.setDeliveryDate(itemDate);

                String key = buildDeduplicationKey(item, buyer.getEmail(), defaultDate, defaultLocation);
                if (seenInEmailKeys.contains(key)) {
                    log.info("Duplicate RFQ Item within same email payload detected, skipping line: {} (dedup key='{}')",
                            desc, key);
                    continue;
                }
                seenInEmailKeys.add(key);

                // The values that will actually reach the RFQ, after every fallback and recovery.
                // Compare this against the AI FINAL item[..] line to see what this service changed.
                log.info("RESOLVED item '{}': qty={}, uom='{}', spec='{}', brand='{}', partCode='{}', location='{}', date='{}'",
                        desc, item.getQuantity(), item.getUom(), item.getSpecification(), item.getBrand(),
                        item.getEffectivePartNumber(), item.getDeliveryLocation(), item.getDeliveryDate());
                validItems.add(item);
            }

            log.info("Item validation complete: {} of {} extracted item(s) are valid.",
                    validItems.size(), extractedRFQ.getItems().size());

            if (validItems.isEmpty()) {
                log.warn("RFQ validation failed: All items in email were invalid.");
                acknowledgementEmailService.sendConsolidatedAcknowledgement(Collections.emptyList(), List.of("No valid items extracted from email"), buyer, email.getSubject());
                emailReaderService.moveMessageToFolder(email.getMessageId(), errorFolder);

                transaction.setStatus("VALIDATION_FAILED");
                transaction.setErrorMessage("No valid items extracted");
                emailTransactionRepository.save(transaction);
                return "VALIDATION_FAILED";
            }

            // STEP 4: CLASSIFY ITEMS INDEPENDENTLY (WITHOUT forcing top-level category onto all items)
            categoryClassificationService.classifyItems(validItems);
            for (RFQItem item : validItems) {
                log.info("Category assigned independently to item '{}': '{}' (Division: '{}')",
                        item.getItemDescription(), item.getCategory(), item.getDivision());
            }

            // STEP 5: GROUP ITEMS BY CATEGORY, LOCATION & DATE
            Map<String, List<RFQItem>> itemGroups = groupItemsByCategoryLocationAndDate(validItems, defaultLocation, defaultDate);

            // One RFQ is created per group, so a group split is the difference between "one RFQ with
            // four lines" and "four RFQs with one line each". Log the split explicitly: it is the
            // only way to tell a grouping split apart from a downstream loss of line items.
            log.info("Grouped {} valid item(s) into {} RFQ group(s) by delivery location and date: {}",
                    validItems.size(), itemGroups.size(),
                    itemGroups.entrySet().stream()
                            .map(e -> "[" + e.getKey() + "] = " + e.getValue().size() + " item(s)")
                            .collect(java.util.stream.Collectors.joining(", ")));
            if (itemGroups.size() > 1) {
                log.warn("This email will produce {} separate RFQs because its items do not share one "
                        + "delivery location and date. Items expected on a single RFQ must agree on both.",
                        itemGroups.size());
            }

            List<RFQEntity> createdRfqs = new ArrayList<>();
            boolean hasFailures = false;

            for (Map.Entry<String, List<RFQItem>> groupEntry : itemGroups.entrySet()) {
                List<RFQItem> groupItems = groupEntry.getValue();
                String groupCategory = groupItems.get(0).getCategory();
                String groupLocation = groupItems.get(0).getDeliveryLocation();
                String groupDate = groupItems.get(0).getDeliveryDate();

                String[] parsedLoc = parseCityStatePincodeFromLocation(groupLocation, extractedRFQ.getDeliveryCity(), extractedRFQ.getDeliveryState(), extractedRFQ.getDeliveryPincode(), buyer);

                ExtractedRFQ groupExtractedRFQ = ExtractedRFQ.builder()
                        .buyerEmail(buyer.getEmail())
                        .deliveryLocation(groupLocation)
                        .deliveryCity(parsedLoc[0])
                        .deliveryState(parsedLoc[1])
                        .deliveryPincode(parsedLoc[2])
                        .deliveryDate(groupDate)
                        .category(groupCategory)
                        .items(groupItems)
                        .build();

                RFQRequest rfqRequest = rfqBuilderService.buildRFQRequest(groupExtractedRFQ, buyer, email.getSubject(), email.getAttachments());
                String generatedRfqNumber = rfqRequest.getRfqNumber();

                log.info("Creating RFQ for buyerId={}, Category='{}', Location='{}', Date='{}'", buyer.getUserId(), groupCategory, groupLocation, groupDate);
                RFQResponse apiResponse = rfqApiService.submitRFQ(rfqRequest);

                if ("SUCCESS".equalsIgnoreCase(apiResponse.getStatus())) {
                    log.info("RFQ created successfully: rfqId={}", generatedRfqNumber);

                    RFQEntity rfqEntity = RFQEntity.builder()
                            .rfqNumber(generatedRfqNumber)
                            .buyerEmail(buyer.getEmail())
                            .status("SUCCESS")
                            .rawSubject(email.getSubject())
                            .itemsJson(objectMapper.writeValueAsString(groupItems))
                            .deliveryLocation(groupLocation)
                            .deliveryDate(rfqRequest.getDeliveryDate() != null ? rfqRequest.getDeliveryDate() : groupDate)
                            .build();

                    RFQEntity savedRfq = rfqRepository.save(rfqEntity);
                    createdRfqs.add(savedRfq);

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
                } else {
                    hasFailures = true;
                    log.warn("RFQ creation failed for RFQ Number: {}", generatedRfqNumber);
                    failedItemsList.add("Group (" + groupCategory + ") | Reason: RFQ Creation error: " + apiResponse.getMessage());
                }
            }

            // SEND EXACTLY ONE CONSOLIDATED ACKNOWLEDGEMENT EMAIL PER INCOMING EMAIL
            acknowledgementEmailService.sendConsolidatedAcknowledgement(createdRfqs, failedItemsList, buyer, email.getSubject());

            boolean atLeastOneSuccess = !createdRfqs.isEmpty();

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
                acknowledgementEmailService.sendProcessingFailureAcknowledgement(
                        normalizedSender,
                        "Valued Customer",
                        "Unexpected system error: " + e.getMessage()
                );
            } catch (Exception mailEx) {
                log.error("Failed to send failure email in catch block: {}", mailEx.getMessage());
            }
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
        return Set.of("FAILED", "PARTIAL_FAILURE", "AI_FAILED", "VALIDATION_FAILED", "INVALID_BUYER", "FILE_SIZE_EXCEEDED")
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

    private Map<String, List<RFQItem>> groupItemsByCategoryLocationAndDate(List<RFQItem> items, String defaultLoc, String defaultDate) {
        Map<String, List<RFQItem>> groups = new LinkedHashMap<>();
        for (RFQItem item : items) {
            String loc = item.getDeliveryLocation() != null && !item.getDeliveryLocation().isBlank()
                    ? item.getDeliveryLocation().trim() : defaultLoc;
            String rawDate = item.getDeliveryDate() != null && !item.getDeliveryDate().isBlank()
                    ? item.getDeliveryDate().trim() : defaultDate;
            String date = dateParser.parseDateString(rawDate);
            item.setDeliveryDate(date);

            String groupKey = loc.toLowerCase() + "|" + date.toLowerCase();
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    /**
     * Labels a buyer uses to name the product. Shared so the thread-merge guard and the
     * description fallback scan agree on what counts as "this email names its own product".
     *
     * <p>Anchored to the start of a line and requiring a colon or equals, because these words also
     * occur in ordinary prose. Without the anchor, "Original branded material, warranty certificate
     * required." matched on the word "material" and yielded "warranty certificate required." as the
     * product name. A leading pipe or bullet is allowed so a flattened spreadsheet cell still matches.
     */
    private static final String PRODUCT_LABEL_REGEX =
            "(?im)^[\\s|\\-*]*(?:product|item|material|description)\\s*[:=]\\s*([^\\r\\n]+)";

    /** Labels that mark where a value ends when a whole requirement block is collapsed onto one line. */
    private static final java.util.regex.Pattern NEXT_FIELD_LABEL_PATTERN = java.util.regex.Pattern.compile(
            "(?i)\\s+(?:quantity|qty|uom|unit|specifications?|specs|configuration|brand|make|manufacturer|"
            + "delivery\\s+location|location|ship\\s+to|city|state|pincode|pin|remarks|notes|"
            + "delivery\\s+date|required\\s+by|date)\\s*[:=]");

    /**
     * Cuts a scanned value at the next field label. A label block that lost its line breaks turns
     * "Description: Laptop Quantity: 25 UOM: Nos" into one line, and the line-tail capture would
     * otherwise take every following field as part of the product name.
     */
    private String stripTrailingLabels(String value) {
        if (value == null) {
            return null;
        }
        java.util.regex.Matcher matcher = NEXT_FIELD_LABEL_PATTERN.matcher(value);
        return matcher.find() ? value.substring(0, matcher.start()).trim() : value.trim();
    }

    private ExtractedRFQ mergeThreadContext(EmailData email, ExtractedRFQ extracted) {
        if (extracted == null || (email.getInReplyTo() == null && email.getReferences() == null)) {
            return extracted;
        }
        List<String> messageIds = new ArrayList<>();
        if (email.getInReplyTo() != null) {
            messageIds.add(email.getInReplyTo().trim());
        }
        if (email.getReferences() != null) {
            messageIds.addAll(Arrays.asList(email.getReferences().trim().split("\\s+")));
        }
        for (String messageId : messageIds) {
            Optional<EmailTransaction> prior = emailTransactionRepository.findByMessageId(messageId);
            if (prior.isEmpty() || prior.get().getExtractionJson() == null) {
                continue;
            }
            try {
                ExtractedRFQ historical = objectMapper.readValue(prior.get().getExtractionJson(), ExtractedRFQ.class);

                // The current email always outranks thread history. Inheriting an item identity
                // from an earlier message is only correct when this email names no product of its
                // own - the buyer replying "quantity is 25" to our clarification mail. When the
                // buyer instead starts a NEW requirement inside an old Gmail thread, inheriting
                // would retarget the RFQ at the previous thread's product: an order for a Laptop
                // silently became an order for MS Hex Bolts. Getting the wrong product onto an RFQ
                // is worse than asking the buyer to resend.
                String statedProduct = stripTrailingLabels(scanFieldFromEmail(email, PRODUCT_LABEL_REGEX));
                boolean namesItsOwnProduct = statedProduct != null && !statedProduct.isBlank();
                if (namesItsOwnProduct) {
                    log.info("Thread history found for this email, but it names its own product ('{}'). "
                            + "Item identity will NOT be inherited from the earlier message.", statedProduct.trim());
                }

                if (historical.getItems() != null && !historical.getItems().isEmpty()
                        && extracted.getItems() != null && !extracted.getItems().isEmpty()) {
                    for (int i = 0; i < extracted.getItems().size(); i++) {
                        RFQItem current = extracted.getItems().get(i);
                        RFQItem original = i < historical.getItems().size() ? historical.getItems().get(i) : historical.getItems().get(0);
                        if (current.getItemDescription() == null || current.getItemDescription().isBlank()) {
                            current.setItemDescription(namesItsOwnProduct ? statedProduct.trim() : original.getItemDescription());
                        }
                        // Specification, brand and category describe a specific product, so they
                        // may only be carried over while we are still discussing the same one.
                        if (!namesItsOwnProduct) {
                            if (current.getSpecification() == null || current.getSpecification().isBlank()) current.setSpecification(original.getSpecification());
                            if (current.getBrand() == null || current.getBrand().isBlank()) current.setBrand(original.getBrand());
                            if (current.getCategory() == null || current.getCategory().isBlank()) current.setCategory(original.getCategory());
                        }
                    }
                }
                if (isBlank(extracted.getDeliveryLocation())) extracted.setDeliveryLocation(historical.getDeliveryLocation());
                if (isBlank(extracted.getDeliveryDate())) extracted.setDeliveryDate(historical.getDeliveryDate());
            } catch (Exception e) {
                log.warn("Could not merge historical thread extraction for message {}: {}", messageId, e.getMessage());
            }
            break;
        }
        return extracted;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank() || value.equalsIgnoreCase("Not Specified");
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

    private String resolveDeliveryLocation(String extractedLoc, Buyer buyer) {
        if (extractedLoc != null && !extractedLoc.isBlank() && !extractedLoc.equalsIgnoreCase("Not Specified")) {
            return extractedLoc.trim();
        }
        List<String> parts = new ArrayList<>();
        if (buyer != null) {
            if (buyer.getAddress() != null && !buyer.getAddress().isBlank()) parts.add(buyer.getAddress().trim());
            if (buyer.getCity() != null && !buyer.getCity().isBlank()) parts.add(buyer.getCity().trim());
            if (buyer.getState() != null && !buyer.getState().isBlank()) parts.add(buyer.getState().trim());
            if (buyer.getPincode() != null && !buyer.getPincode().isBlank()) parts.add(buyer.getPincode().trim());
        }
        if (!parts.isEmpty()) {
            return String.join(", ", parts);
        }
        return "Registered Profile Address";
    }

    private String extractProductFromSubject(String subject) {
        if (subject == null || subject.isBlank()) return "";
        String clean = subject.replaceAll("(?i)^(?:re|fwd|rfq|request for quotation|inquiry for|inquiry)[:\\-–—\\s]+", "").trim();
        if (clean.equalsIgnoreCase("rfq") || clean.equalsIgnoreCase("request for quotation")
                || clean.equalsIgnoreCase("inquiry") || clean.equalsIgnoreCase("(no subject)")
                || clean.equalsIgnoreCase("no subject")) {
            return "";
        }
        return clean;
    }

    private Double parseQuantityFromText(String text) {
        if (text == null || text.isBlank()) return null;
        return com.portal.procucev.rfq.util.QuantityNormalizer.normalize(text);
    }

    /** Tokens that mark a string as technical detail rather than a brand or make name. */
    private static final java.util.regex.Pattern SPEC_PROSE_PATTERN = java.util.regex.Pattern.compile(
            "(?i)\\b(?:processor|ram|ssd|hdd|display|screen|webcam|bluetooth|wi-?fi|hdmi|usb|"
            + "windows|operating\\s+system|resolution|battery|warranty|graphics|ethernet|"
            + "keyboard|touchpad|capacity|voltage|material|grade|thickness|diameter|"
            + "\\d+\\s*(?:gb|tb|mb|ghz|mhz|inch|inches|mm|cm|kg|w|watt|volt|v))\\b");

    /** Filler openers that a real brand name never starts with. */
    private static final java.util.regex.Pattern BRAND_FILLER_START_PATTERN = java.util.regex.Pattern.compile(
            "(?i)^(?:new|with|the|and|for|our|we|please|require|required|need|needed|"
            + "quote|quotation|supply|following|below|above|as\\s+per)\\b");

    private static final int MAX_BRAND_NAME_LENGTH = 60;
    private static final int MAX_BRAND_NAME_TOKENS = 6;

    /**
     * Decides whether a value is plausibly a brand / make name rather than requirement prose.
     *
     * <p>A brand is a short proper noun, optionally a slash-separated list such as
     * "Dell / HP / Lenovo". Requirement text like "new laptops with Intel Core i5, 16 GB RAM,
     * 512 GB SSD" is not, and must not be persisted as the brand.
     */
    private boolean looksLikeBrandName(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_BRAND_NAME_LENGTH) {
            return false;
        }
        if (SPEC_PROSE_PATTERN.matcher(trimmed).find()) {
            return false;
        }
        if (BRAND_FILLER_START_PATTERN.matcher(trimmed).find()) {
            return false;
        }
        // Count real words, treating slashes and commas as brand separators.
        String[] tokens = trimmed.split("[\\s/,&+]+");
        int words = 0;
        for (String token : tokens) {
            if (!token.isBlank()) {
                words++;
            }
        }
        return words <= MAX_BRAND_NAME_TOKENS;
    }

    /** Field labels that must never be accepted as the body of a specification. */
    private static final java.util.regex.Pattern OTHER_FIELD_LABEL_START_PATTERN = java.util.regex.Pattern.compile(
            "(?i)^(?:delivery|location|address|ship\\s+to|deliver\\s+to|destination|plant|warehouse|"
            + "quantity|qty|date|due|brand|make|manufacturer|uom|unit|category|pincode|zipcode)\\b");

    /**
     * Rejects a scanned specification that is really the next field's label, or too short to carry
     * any technical meaning.
     */
    private boolean isPlausibleSpecification(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 4) {
            return false;
        }
        return !OTHER_FIELD_LABEL_START_PATTERN.matcher(trimmed).find();
    }

    /**
     * Moves requirement prose out of the brand field. If the specification is empty the prose is
     * used as the specification, since that is what the buyer actually wrote; otherwise it is
     * discarded so the brand falls back to "Not Specified".
     */
    private void salvageSpecificationFromBrand(RFQItem item, String desc) {
        String rawBrand = item.getBrand();
        if (rawBrand == null || rawBrand.isBlank()
                || rawBrand.equalsIgnoreCase("null")
                || rawBrand.equalsIgnoreCase("Not Specified")) {
            return;
        }
        if (looksLikeBrandName(rawBrand)) {
            return;
        }

        boolean specMissing = item.getSpecification() == null
                || item.getSpecification().isBlank()
                || item.getSpecification().equalsIgnoreCase("Not Specified")
                || item.getSpecification().equalsIgnoreCase("null");

        if (specMissing) {
            String salvaged = rawBrand.trim();
            log.warn("Brand field for item '{}' held requirement text, not a brand name. Moving it to specification: '{}'",
                    desc, salvaged);
            item.setSpecification(salvaged);
        } else {
            log.warn("Discarding non-brand text from the brand field for item '{}': '{}'", desc, rawBrand.trim());
        }
        item.setBrand(null);
    }

    /**
     * An explicit quantity statement: the word quantity or qty, optionally followed by "of", then
     * the value. Requiring the keyword is what keeps specification numbers such as "16 GB RAM" or
     * "21.5-inch monitor" out of the match.
     */
    private static final java.util.regex.Pattern QUANTITY_STATEMENT_PATTERN = java.util.regex.Pattern.compile(
            "(?i)\\b(?:required\\s+)?(?:quantity|qty)\\b\\s*(?:of\\s+)?[:=]?\\s*([^\\r\\n]{1,40})");

    /**
     * Finds a quantity written directly beside one item's name, searching body, attachment text and
     * subject in that order. Anchoring on the name is what makes this usable on a multi-item
     * requirement, where {@link #scanQuantityFromEmail} would apply one number to every row.
     */
    private QuantityNormalizer.QuantityMatch scanQuantityForItem(EmailData email, String description) {
        if (email == null || description == null || description.isBlank()) {
            return null;
        }
        String[] sources = {email.getBody(), email.getAttachmentText(), email.getSubject()};
        for (String source : sources) {
            QuantityNormalizer.QuantityMatch match = QuantityNormalizer.findQuantityForItem(source, description);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * Last-resort recovery of a quantity the model failed to return, scanning body, attachment and
     * subject. The captured tail is handed to {@link QuantityNormalizer} behind a "Quantity:"
     * prefix so it reuses the existing digit, digit-plus-unit and number-word parsing.
     */
    private Double scanQuantityFromEmail(EmailData email) {
        if (email == null) {
            return null;
        }
        String[] sources = {email.getBody(), email.getAttachmentText(), email.getSubject()};
        for (String source : sources) {
            if (source == null || source.isBlank()) {
                continue;
            }
            java.util.regex.Matcher matcher = QUANTITY_STATEMENT_PATTERN.matcher(source);
            while (matcher.find()) {
                String tail = matcher.group(1).trim();
                if (tail.isEmpty()) {
                    continue;
                }
                Double parsed = com.portal.procucev.rfq.util.QuantityNormalizer.normalize("Quantity: " + tail);
                if (parsed != null && parsed > 0) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private String extractFieldByPattern(String text, String regex) {
        if (text == null || text.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Regex safety net for a labelled field, searching the body first and then the attachment text.
     *
     * <p>These scans previously read only {@code email.getBody()}. For a requirement supplied as a
     * spreadsheet the body is often just a covering note, so a "Delivery Location" stated inside
     * the attachment could not be recovered when the model failed to pick it up.
     */
    private String scanFieldFromEmail(EmailData email, String regex) {
        if (email == null) {
            return null;
        }
        String fromBody = cleanScannedValue(extractFieldByPattern(email.getBody(), regex));
        if (fromBody != null && !fromBody.isBlank()) {
            return fromBody;
        }
        return cleanScannedValue(extractFieldByPattern(email.getAttachmentText(), regex));
    }

    /**
     * Normalises delimiter noise in a scanned value.
     *
     * <p>Pipes arrive from two places: flattened spreadsheet cells, and the HTML-to-text
     * conversion in {@code EmailReaderService}, which emits a pipe per tag boundary. Segments are
     * joined with ", " rather than truncated at the first pipe, because a specification written as
     * an HTML bullet list becomes one pipe-separated segment per bullet and keeping only the first
     * would drop most of the buyer's detail.
     */
    private String cleanScannedValue(String raw) {
        if (raw == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        for (String segment : raw.split("\\|")) {
            String candidate = segment.replaceAll("\\s+", " ").trim();
            // Drop separator-only fragments left behind by empty cells or adjacent tags.
            candidate = candidate.replaceAll("^[,;:.\\-\\u2013\\u2014]+", "").replaceAll("[,;:]+$", "").trim();
            if (!candidate.isEmpty()) {
                segments.add(candidate);
            }
        }
        if (segments.isEmpty()) {
            return null;
        }
        return String.join(", ", segments);
    }

    /**
     * Detects whether the buyer stated a purchase quantity anywhere in the request.
     *
     * <p>Attachment text is included deliberately. When it was omitted, an RFQ whose line items
     * live in a spreadsheet and whose body only says something like "PFA our requirement" had no
     * quantity keyword in subject or body, so every item was treated as quantity-less and the
     * whole email was rejected even though the attachment listed quantities for every row.
     */
    private boolean hasExplicitPurchaseQuantityInText(String subject, String body, String attachmentText) {
        String combined = ((subject != null ? subject : "") + " "
                + (body != null ? body : "") + " "
                + (attachmentText != null ? attachmentText : "")).toLowerCase();
        
        if (java.util.regex.Pattern.compile("(?i)(?:quantity|qty|required\\s+quantity|required\\s+units|units\\s+required|pieces\\s+required|nos\\s+required|number\\s+of\\s+units)\\s*[:=]?\\s*([a-z0-9,\\-\\s]+)").matcher(combined).find()) {
            return true;
        }
        if (java.util.regex.Pattern.compile("(?i)\\b(?:we\\s+require|require|we\\s+need|need|please\\s+quote|quote\\s+for|purchase)\\s+(?:\\d+|one|two|three|four|five|six|seven|eight|nine|ten|twenty|fifty|hundred|thousand|lakh|lacs)\\b").matcher(combined).find()) {
            return true;
        }
        if (java.util.regex.Pattern.compile("(?i)\\b\\d+\\s*(?:units?|nos?|pieces?|pcs?|laptops?|machines?|systems?|sets?|bags?|meters?|mtr|kgs?|boxes|box|rolls?|sheets?|pairs?|dozens?|packets?|pkts?|packs?|bundles?|cartons?|reams?|tubes?|cans?|drums?|coils?)\\b").matcher(combined).find()) {
            return true;
        }
        return false;
    }

    private String[] parseCityStatePincodeFromLocation(String locationStr, String defaultCity, String defaultState, String defaultPincode, Buyer buyer) {
        String city = defaultCity != null ? defaultCity : "";
        String state = defaultState != null ? defaultState : "";
        String pincode = defaultPincode != null ? defaultPincode : "";

        if (locationStr != null && !locationStr.isBlank() && !locationStr.equalsIgnoreCase("Not Specified") && !locationStr.equalsIgnoreCase("Registered Profile Address")) {
            String cleanLoc = locationStr.replaceAll("[^\\x00-\\x7F]", " ");
            
            java.util.regex.Matcher pinMatcher = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(cleanLoc);
            if (pinMatcher.find()) {
                pincode = pinMatcher.group(1);
            }

            String lower = cleanLoc.toLowerCase();
            if (lower.contains("bangalore") || lower.contains("bengaluru")) {
                city = "Bangalore";
                state = "Karnataka";
            } else if (lower.contains("hyderabad")) {
                city = "Hyderabad";
                state = "Telangana";
            } else if (lower.contains("chennai")) {
                city = "Chennai";
                state = "Tamil Nadu";
            } else if (lower.contains("mumbai")) {
                city = "Mumbai";
                state = "Maharashtra";
            } else if (lower.contains("delhi")) {
                city = "Delhi";
                state = "Delhi";
            } else if (lower.contains("kolkata")) {
                city = "Kolkata";
                state = "West Bengal";
            } else if (lower.contains("pune")) {
                city = "Pune";
                state = "Maharashtra";
            } else if (lower.contains("ahmedabad")) {
                city = "Ahmedabad";
                state = "Gujarat";
            } else if (lower.contains("kakinada")) {
                city = "Kakinada";
                state = "Andhra Pradesh";
            }

            if (state.isBlank()) {
                if (lower.contains("karnataka")) state = "Karnataka";
                else if (lower.contains("telangana")) state = "Telangana";
                else if (lower.contains("andhra")) state = "Andhra Pradesh";
                else if (lower.contains("maharashtra")) state = "Maharashtra";
                else if (lower.contains("tamil nadu") || lower.contains("tamilnadu")) state = "Tamil Nadu";
                else if (lower.contains("west bengal")) state = "West Bengal";
                else if (lower.contains("gujarat")) state = "Gujarat";
                else if (lower.contains("rajasthan")) state = "Rajasthan";
            }
        }

        if (buyer != null) {
            if (city.isBlank() && buyer.getCity() != null && !buyer.getCity().isBlank()) {
                city = buyer.getCity().trim();
            }
            if (state.isBlank() && buyer.getState() != null && !buyer.getState().isBlank()) {
                state = buyer.getState().trim();
            }
            if (pincode.isBlank() && buyer.getPincode() != null && !buyer.getPincode().isBlank()) {
                pincode = buyer.getPincode().trim();
            }
        }

        return new String[]{city, state, pincode};
    }

    private boolean hasExplicitLocationInPayload(ExtractedRFQ extractedRFQ, EmailData email) {
        if (extractedRFQ != null) {
            if (extractedRFQ.getDeliveryLocation() != null 
                    && !extractedRFQ.getDeliveryLocation().isBlank() 
                    && !extractedRFQ.getDeliveryLocation().equalsIgnoreCase("Not Specified")
                    && !extractedRFQ.getDeliveryLocation().equalsIgnoreCase("null")
                    && !extractedRFQ.getDeliveryLocation().equalsIgnoreCase("Registered Profile Address")) {
                return true;
            }
            if (extractedRFQ.getDeliveryCity() != null && !extractedRFQ.getDeliveryCity().isBlank() && !extractedRFQ.getDeliveryCity().equalsIgnoreCase("Not Specified")) {
                return true;
            }
            if (extractedRFQ.getDeliveryState() != null && !extractedRFQ.getDeliveryState().isBlank() && !extractedRFQ.getDeliveryState().equalsIgnoreCase("Not Specified")) {
                return true;
            }
            if (extractedRFQ.getDeliveryPincode() != null && !extractedRFQ.getDeliveryPincode().isBlank() && !extractedRFQ.getDeliveryPincode().equalsIgnoreCase("Not Specified")) {
                return true;
            }
            if (extractedRFQ.getItems() != null) {
                for (RFQItem item : extractedRFQ.getItems()) {
                    if (item != null && item.getDeliveryLocation() != null 
                            && !item.getDeliveryLocation().isBlank() 
                            && !item.getDeliveryLocation().equalsIgnoreCase("Not Specified")
                            && !item.getDeliveryLocation().equalsIgnoreCase("null")
                            && !item.getDeliveryLocation().equalsIgnoreCase("Registered Profile Address")) {
                        return true;
                    }
                }
            }
        }
        
        // Attachment text is included: a delivery location stated only inside a spreadsheet is
        // still an explicitly stated location, and excluding it rejected valid requirements.
        String combined = ((email != null && email.getSubject() != null ? email.getSubject() : "") + " "
                + (email != null && email.getBody() != null ? email.getBody() : "") + " "
                + (email != null && email.getAttachmentText() != null ? email.getAttachmentText() : "")).toLowerCase();
        if (java.util.regex.Pattern.compile("(?i)(?:delivery\\s+location|delivery\\s+address|location|plant|warehouse|address|ship\\s+to|deliver\\s+to|destination|pincode|zipcode)\\s*[:=]?\\s*([a-z0-9,\\-\\s]+)").matcher(combined).find()) {
            return true;
        }
        
        return false;
    }
}
