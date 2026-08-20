package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.dto.ProcessingStats;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import com.portal.procucev.rfq.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class EmailProcessorServiceTest {

    private EmailReaderService emailReaderService;
    private AIExtractionService aiExtractionService;
    private ValidationService validationService;
    private BuyerVerificationService buyerVerificationService;
    private RFQBuilderService rfqBuilderService;
    private RFQApiService rfqApiService;
    private CategoryClassificationService categoryClassificationService;
    private AcknowledgementEmailService acknowledgementEmailService;
    private RFQRepository rfqRepository;
    private EmailTransactionRepository emailTransactionRepository;
    private RfqItemRecordRepository rfqItemRecordRepository;

    private EmailProcessorService emailProcessorService;

    @BeforeEach
    void setUp() {
        emailReaderService = Mockito.mock(EmailReaderService.class);
        aiExtractionService = Mockito.mock(AIExtractionService.class);
        validationService = Mockito.mock(ValidationService.class);
        buyerVerificationService = Mockito.mock(BuyerVerificationService.class);
        rfqBuilderService = Mockito.mock(RFQBuilderService.class);
        rfqApiService = Mockito.mock(RFQApiService.class);
        categoryClassificationService = Mockito.mock(CategoryClassificationService.class);
        acknowledgementEmailService = Mockito.mock(AcknowledgementEmailService.class);
        rfqRepository = Mockito.mock(RFQRepository.class);
        emailTransactionRepository = Mockito.mock(EmailTransactionRepository.class);
        rfqItemRecordRepository = Mockito.mock(RfqItemRecordRepository.class);

        emailProcessorService = new EmailProcessorService(
                emailReaderService,
                aiExtractionService,
                validationService,
                buyerVerificationService,
                rfqBuilderService,
                rfqApiService,
                categoryClassificationService,
                acknowledgementEmailService,
                rfqRepository,
                emailTransactionRepository,
                rfqItemRecordRepository,
                new DateParser(),
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Test processUnreadEmails when no unread emails exist")
    void testProcessUnreadEmailsEmpty() {
        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(List.of());

        ProcessingStats stats = emailProcessorService.processUnreadEmails();

        assertNotNull(stats);
        assertEquals(0, stats.getEmailsProcessed());
        assertEquals(0, stats.getRfqsCreated());
        assertEquals(0, stats.getErrors());
    }

    @Test
    @DisplayName("Test processSingleEmail skips system sender emails")
    void testProcessSingleEmailSystemSender() {
        EmailData email = EmailData.builder()
                .messageId("MSG-SYS")
                .senderEmail("noreply@procucev.com")
                .subject("System Notification")
                .build();

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("SKIPPED_SYSTEM_EMAIL", result);
        Mockito.verify(emailReaderService).moveMessageToFolder(eq("MSG-SYS"), any());
    }

    @Test
    @DisplayName("Test processSingleEmail reprocesses previously seen email instead of skipping as duplicate")
    void testProcessSingleEmailDuplicate() {
        EmailData email = EmailData.builder()
                .messageId("MSG-DUP")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-DUP"))
                .thenReturn(Optional.of(EmailTransaction.builder().messageId("MSG-DUP").status("FAILED").build()));

        Buyer unverifiedBuyer = Buyer.builder().email("buyer@test.com").verified(false).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(unverifiedBuyer);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("INVALID_BUYER", result);
        Mockito.verify(buyerVerificationService).verifyAndGetBuyer("buyer@test.com");
    }

    @Test
    @DisplayName("Test processSingleEmail with unregistered buyer")
    void testProcessSingleEmailUnregisteredBuyer() {
        EmailData email = EmailData.builder()
                .messageId("MSG-UNREG")
                .senderEmail("unregistered@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-UNREG")).thenReturn(Optional.empty());

        Buyer unverifiedBuyer = Buyer.builder().email("unregistered@test.com").verified(false).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("unregistered@test.com")).thenReturn(unverifiedBuyer);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("INVALID_BUYER", result);
        Mockito.verify(acknowledgementEmailService).sendUnregisteredBuyerAcknowledgement("unregistered@test.com");
    }

    @Test
    @DisplayName("Test processSingleEmail AI extraction failure")
    void testProcessSingleEmailAiFailed() {
        EmailData email = EmailData.builder()
                .messageId("MSG-AI-FAIL")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-AI-FAIL")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenThrow(new RuntimeException("AI failure"));

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("AI_FAILED", result);
        Mockito.verify(acknowledgementEmailService).sendFailureAcknowledgement(any(), eq(verifiedBuyer));
    }

    @Test
    @DisplayName("Test processSingleEmail validation failure (missing quantity)")
    void testProcessSingleEmailValidationFailedMissingQuantity() {
        EmailData email = EmailData.builder()
                .messageId("MSG-VAL-FAIL")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-VAL-FAIL")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(null).build()))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(false, true, List.of("Laptop"), "Missing Qty");
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("VALIDATION_FAILED", result);
        Mockito.verify(acknowledgementEmailService).sendConsolidatedAcknowledgement(anyList(), anyList(), any(), any());
    }

    @Test
    @DisplayName("Test processSingleEmail success creation")
    void testProcessSingleEmailSuccess() {
        EmailData email = EmailData.builder()
                .messageId("MSG-SUCCESS")
                .senderEmail("buyer@test.com")
                .subject("Need Laptops")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-SUCCESS")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        List<RFQItem> items = List.of(
                RFQItem.builder().itemDescription("Dell Laptop").quantity(5.0).uom("NOS").category("IT").build()
        );
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .deliveryLocation("Bangalore")
                .deliveryDate("2026-08-25")
                .items(items)
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-999").deliveryDate("2026-08-25").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        RFQResponse apiResponse = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-999").build();
        Mockito.when(rfqApiService.submitRFQ(request)).thenReturn(apiResponse);

        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-999").buyerEmail("buyer@test.com").build();
        Mockito.when(rfqRepository.save(any())).thenReturn(savedEntity);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        Mockito.verify(acknowledgementEmailService).sendConsolidatedAcknowledgement(anyList(), anyList(), eq(verifiedBuyer), anyString());
    }

    @Test
    @DisplayName("Test processSingleEmail deduplication and item grouping")
    void testProcessSingleEmailDeduplicationAndGrouping() {
        EmailData email = EmailData.builder()
                .messageId("MSG-DEDUP")
                .senderEmail("buyer@test.com")
                .subject("Need Items")
                .body("Quantity: 5 units, Delivery Location: Loc1")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-DEDUP")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        List<RFQItem> items = List.of(
                RFQItem.builder().itemDescription("Dell Laptop").quantity(5.0).deliveryLocation("Loc1").deliveryDate("2026-08-25").build(),
                RFQItem.builder().itemDescription("Dell Laptop").quantity(5.0).deliveryLocation("Loc1").deliveryDate("2026-08-25").build(), // duplicate
                RFQItem.builder().itemDescription("Item3").quantity(5.0).deliveryLocation("Loc1").deliveryDate("2026-08-25").build(),
                RFQItem.builder().itemDescription("Monitor").quantity(2.0).deliveryLocation("Loc2").deliveryDate("2026-08-30").build()
        );

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .deliveryLocation("Loc1")
                .items(items)
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-100").deliveryDate("2026-08-25").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        RFQResponse apiResponse = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-100").build();
        Mockito.when(rfqApiService.submitRFQ(request)).thenReturn(apiResponse);

        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-100").buyerEmail("buyer@test.com").build();
        Mockito.when(rfqRepository.save(any())).thenReturn(savedEntity);
        Mockito.doThrow(new RuntimeException("DB item error")).when(rfqItemRecordRepository).save(any());

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail when deduplication leaves no valid items")
    void testProcessSingleEmailNoValidItems() {
        EmailData email = EmailData.builder()
                .messageId("MSG-NO-ITEMS")
                .senderEmail("buyer@test.com")
                .subject("Empty Items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-NO-ITEMS")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .items(List.of(RFQItem.builder().itemDescription("").build()))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("VALIDATION_FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail submitRFQ failure and partial failure")
    void testProcessSingleEmailSubmitRfqFailure() {
        EmailData email = EmailData.builder()
                .messageId("MSG-SUBMIT-FAIL")
                .senderEmail("buyer@test.com")
                .subject("Submit Fail")
                .body("Quantity: 5 units, Delivery Location: Mumbai")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-SUBMIT-FAIL")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        List<RFQItem> items = List.of(
                RFQItem.builder().itemDescription("Laptop").quantity(5.0).deliveryLocation("Mumbai").build()
        );
        ExtractedRFQ rfq = ExtractedRFQ.builder().buyerEmail("buyer@test.com").deliveryLocation("Mumbai").items(items).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-FAIL").deliveryDate("2026-08-25").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        RFQResponse apiResponse = RFQResponse.builder().status("FAILED").message("API Error").build();
        Mockito.when(rfqApiService.submitRFQ(request)).thenReturn(apiResponse);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail unhandled exception handling")
    void testProcessSingleEmailUnhandledException() {
        EmailData email = EmailData.builder()
                .messageId("MSG-EX")
                .senderEmail("buyer@test.com")
                .subject("Crash")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-EX")).thenThrow(new RuntimeException("Crash"));

        assertThrows(RuntimeException.class, () -> emailProcessorService.processSingleEmail(email));
    }

    @Test
    @DisplayName("Test processUnreadEmails with errors")
    void testProcessUnreadEmailsWithErrors() {
        EmailData email1 = EmailData.builder().messageId("M1").senderEmail("buyer@test.com").build();
        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(List.of(email1));
        Mockito.when(emailTransactionRepository.findByMessageId("M1")).thenThrow(new RuntimeException("Error"));

        ProcessingStats stats = emailProcessorService.processUnreadEmails();
        assertNotNull(stats);
        assertEquals("COMPLETED_WITH_ERRORS", stats.getStatus());
        assertEquals(1, stats.getErrors());
    }

    @Test
    @DisplayName("Test processUnreadEmails with mixed success and failure")
    void testProcessUnreadEmailsMixed() {
        EmailData e1 = EmailData.builder().messageId("M-OK").senderEmail("noreply@procucev.com").subject("sys").build();
        EmailData e2 = EmailData.builder().messageId("M-ERR").senderEmail("buyer@test.com").build();

        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(List.of(e1, e2));
        Mockito.when(emailTransactionRepository.findByMessageId("M-ERR")).thenThrow(new RuntimeException("Crash"));

        ProcessingStats stats = emailProcessorService.processUnreadEmails();
        assertNotNull(stats);
        assertEquals(2, stats.getEmailsProcessed());
    }

    @Test
    @DisplayName("Test processSingleEmail with null items in extractedRFQ")
    void testProcessSingleEmailNullItems() {
        EmailData email = EmailData.builder()
                .messageId("MSG-NULL-ITEMS")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-NULL-ITEMS")).thenReturn(Optional.empty());

        Buyer buyer = Buyer.builder().email("buyer@test.com").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(buyer);

        ExtractedRFQ rfq = ExtractedRFQ.builder().buyerEmail("buyer@test.com").items(null).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("VALIDATION_FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail with empty items list after dedup")
    void testProcessSingleEmailEmptyItemsAfterDedup() {
        EmailData email = EmailData.builder()
                .messageId("MSG-EMPTY-DEDUP")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-EMPTY-DEDUP")).thenReturn(Optional.empty());

        Buyer buyer = Buyer.builder().email("buyer@test.com").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(buyer);

        ExtractedRFQ rfq = ExtractedRFQ.builder().buyerEmail("buyer@test.com").items(List.of()).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("VALIDATION_FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail validation failure non-missing-quantity")
    void testProcessSingleEmailValidationFailedNonQuantity() {
        EmailData email = EmailData.builder()
                .messageId("MSG-VAL-NON-QTY")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .body("Quantity: 5 units, Delivery Location: Mumbai")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-VAL-NON-QTY")).thenReturn(Optional.empty());

        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(buyer);

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .deliveryLocation("Mumbai")
                .items(List.of(RFQItem.builder().itemDescription("Widget").quantity(5.0).deliveryLocation("Mumbai").build()))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(false, false, List.of(), "Invalid format");
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail with null extractedRFQ from AI")
    void testProcessSingleEmailNullExtractedRFQ() {
        EmailData email = EmailData.builder()
                .messageId("MSG-NULL-RFQ")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-NULL-RFQ")).thenReturn(Optional.empty());

        Buyer buyer = Buyer.builder().email("buyer@test.com").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(buyer);

        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(null);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("AI_FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail with item-level delivery location/date")
    void testProcessSingleEmailItemLevelDelivery() {
        EmailData email = EmailData.builder()
                .messageId("MSG-ITEM-LOC")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .body("Quantity: 5 units, Delivery Location: Default Location")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-ITEM-LOC")).thenReturn(Optional.empty());

        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(buyer);

        List<RFQItem> items = List.of(
                RFQItem.builder().itemDescription("Widget A").quantity(5.0).deliveryLocation("Mumbai").deliveryDate("2026-09-01").build(),
                RFQItem.builder().itemDescription("Widget B").quantity(2.0).deliveryLocation("Default Location").build()
        );
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .deliveryLocation("Default Location")
                .deliveryDate("2026-08-25")
                .category("Test Category")
                .items(items)
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-LOC").deliveryDate("2026-08-25").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        RFQResponse apiResponse = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-LOC").build();
        Mockito.when(rfqApiService.submitRFQ(any())).thenReturn(apiResponse);

        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-LOC").buyerEmail("buyer@test.com").build();
        Mockito.when(rfqRepository.save(any())).thenReturn(savedEntity);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", result);
    }

    @Test
    @DisplayName("Test processUnreadEmails returns null fetch gracefully")
    void testProcessUnreadEmailsNullFetch() {
        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(null);

        ProcessingStats stats = emailProcessorService.processUnreadEmails();
        assertNotNull(stats);
    }
}

