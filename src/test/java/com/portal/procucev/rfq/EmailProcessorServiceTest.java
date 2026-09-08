package com.portal.procucev.rfq;

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
import com.portal.procucev.rfq.entity.RfqAiTokenUsage;
import com.portal.procucev.rfq.model.TokenUsageTelemetry;
import com.portal.procucev.rfq.repository.RfqAiTokenUsageRepository;
import com.portal.procucev.rfq.repository.RfqItemRecordRepository;
import com.portal.procucev.rfq.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

        Mockito.when(rfqRepository.save(Mockito.any(RFQEntity.class))).thenAnswer(i -> i.getArgument(0));
        Mockito.when(buyerVerificationService.verifyAndGetBuyer(Mockito.anyString()))
                .thenReturn(Buyer.builder().email("buyer@corp.com").name("Buyer Name").userId("1").orgId("1").verified(true).build());
        Mockito.when(rfqBuilderService.buildRFQRequest(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(i -> RFQRequest.builder().rfqNumber("RFQ-123").build());

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
    @DisplayName("Test processSingleEmail missing quantity defaults to 1.0 and creates RFQ successfully")
    void testProcessSingleEmailMissingQuantityDefaultsToOne() {
        EmailData email = EmailData.builder()
                .messageId("MSG-VAL-QTY")
                .senderEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-VAL-QTY")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .deliveryLocation("Bangalore")
                .deliveryDate("2026-08-25")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(null).uom(null).build()))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, List.of(), null);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-QTY-1").deliveryDate("2026-08-25").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        RFQResponse apiResponse = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-QTY-1").build();
        Mockito.when(rfqApiService.submitRFQ(request)).thenReturn(apiResponse);

        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-QTY-1").buyerEmail("buyer@test.com").build();
        Mockito.when(rfqRepository.save(any())).thenReturn(savedEntity);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        assertEquals(1.0, rfq.getItems().get(0).getQuantity());
        assertEquals("Nos", rfq.getItems().get(0).getUom());
        Mockito.verify(acknowledgementEmailService).sendConsolidatedAcknowledgement(anyList(), anyList(), eq(verifiedBuyer), anyString());
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
                .subject("RFQ")
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
                .subject("RFQ")
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
                .subject("RFQ")
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
    @DisplayName("Test processSingleEmail with fileSizeExceeded moves to error folder and records transaction")
    void testProcessSingleEmailFileSizeExceeded() {
        EmailData email = EmailData.builder()
                .messageId("MSG-LARGE-FILE")
                .senderEmail("buyer@test.com")
                .subject("Big Drawing RFQ")
                .fileSizeExceeded(true)
                .errorMessage("Attachment 'large_drawing.pdf' (30000000 bytes) exceeds the configured size limit of 26214400 bytes.")
                .failedAttachmentName("large_drawing.pdf")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-LARGE-FILE")).thenReturn(Optional.empty());
        Buyer buyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(buyer);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("FILE_SIZE_EXCEEDED", result);
        Mockito.verify(acknowledgementEmailService).sendFileSizeExceededAcknowledgement(eq("buyer@test.com"), eq("Buyer"), eq("large_drawing.pdf"), anyLong());
        Mockito.verify(emailReaderService).moveMessageToFolder(eq("MSG-LARGE-FILE"), any());
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

    @Test
    @DisplayName("Test EmailProcessorService helper methods: looksLikeBrandName, isPlausibleSpecification, salvageSpecificationFromBrand")
    void testBrandAndSpecificationHelpers() {
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", "Dell / HP / Lenovo"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", "new laptops with 16 GB RAM and 512 GB SSD"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", "A very long text that exceeds sixty characters in total length for brand name"));

        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", "16GB RAM, 512GB SSD"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", "qty"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", "Delivery Location: Mumbai"));

        RFQItem itemWithProseBrand = RFQItem.builder().itemDescription("Laptop").brand("Core i7 16GB RAM").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemWithProseBrand, "Laptop");
        assertNull(itemWithProseBrand.getBrand());
        assertEquals("Core i7 16GB RAM", itemWithProseBrand.getSpecification());
    }

    @Test
    @DisplayName("Test EmailProcessorService helper methods: location and quantity scans")
    void testLocationAndQuantityHelpers() {
        String prod = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "extractProductFromSubject", "RFQ: Industrial Ball Valves");
        assertEquals("Industrial Ball Valves", prod);

        assertEquals("", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "extractProductFromSubject", "RFQ"));

        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "Subject", "We need 50 units", ""));

        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "Subject", "Quantity: 25", ""));

        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "", "", "Required Quantity: 100"));

        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "", "Hello", ""));

        EmailData emailWithQty = EmailData.builder().body("Requirement Details:\nQuantity: 500 Nos").build();
        Double scannedQty = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "scanQuantityFromEmail", emailWithQty);
        assertEquals(500.0, scannedQty);

        String cleaned = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "cleanScannedValue", " | Part A | 16GB RAM | ");
        assertEquals("Part A, 16GB RAM", cleaned);
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "cleanScannedValue", (String) null));

        String[] locParts = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation",
                "Kakinada, 533431, Andhra Pradesh", "", "", "", null);
        assertNotNull(locParts);
        assertEquals("Kakinada", locParts[0]);
        assertEquals("Andhra Pradesh", locParts[1]);
        assertEquals("533431", locParts[2]);

        String[] cities = {"bangalore", "hyderabad", "chennai", "mumbai", "delhi", "kolkata", "pune", "ahmedabad", "kakinada"};
        for (String c : cities) {
            String[] res = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    emailProcessorService, "parseCityStatePincodeFromLocation",
                    "Plant in " + c, "", "", "", null);
            assertNotNull(res);
            assertFalse(res[0].isBlank());
        }
    }

    @Test
    @DisplayName("Test mergeThreadContext when replying in thread")
    void testMergeThreadContext() throws Exception {
        EmailData email = EmailData.builder()
                .inReplyTo("MSG-ORIGINAL")
                .body("Delivery Location: Chennai")
                .build();

        ExtractedRFQ priorRfq = ExtractedRFQ.builder()
                .deliveryLocation("Bangalore")
                .deliveryDate("2026-08-25")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").brand("Dell").specification("16GB").build()))
                .build();

        EmailTransaction priorTx = EmailTransaction.builder()
                .messageId("MSG-ORIGINAL")
                .extractionJson(new ObjectMapper().writeValueAsString(priorRfq))
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-ORIGINAL")).thenReturn(Optional.of(priorTx));

        ExtractedRFQ currentRfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().quantity(10.0).build()))
                .build();

        ExtractedRFQ merged = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", email, currentRfq);

        assertNotNull(merged);
        assertEquals("Laptop", merged.getItems().get(0).getItemDescription());
        assertEquals("Dell", merged.getItems().get(0).getBrand());
        assertEquals("16GB", merged.getItems().get(0).getSpecification());
        assertEquals("Bangalore", merged.getDeliveryLocation());
        assertEquals("2026-08-25", merged.getDeliveryDate());
    }

    @Test
    @DisplayName("Test buildFailedRequestFromEmail with null, empty, and complete ExtractedRFQ")
    void testBuildFailedRequestFromEmail() {
        EmailData email = EmailData.builder()
                .messageId("MSG-FAIL-1")
                .senderEmail("sender@test.com")
                .subject("Need Items")
                .build();

        // 1. null extractedRFQ
        FailedRfqRequest f1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", email, null, "Extraction failed");
        assertNotNull(f1);
        assertEquals("sender@test.com", f1.getBuyerEmail());
        assertEquals("Not Provided", f1.getDescription());
        assertEquals("Not Provided", f1.getDeliveryLocation());

        // 2. complete extractedRFQ with line items
        RFQItem item = RFQItem.builder()
                .itemDescription("Hex Bolt")
                .partCode("HB-100")
                .specification("M10x50")
                .brand("Unbrako")
                .quantity(50.0)
                .uom("Nos")
                .build();
        ExtractedRFQ rfqFull = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Pune")
                .deliveryDate("2026-09-01")
                .items(List.of(item))
                .build();

        FailedRfqRequest f2 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", email, rfqFull, "Missing pricing");
        assertNotNull(f2);
        assertEquals("buyer@corp.com", f2.getBuyerEmail());
        assertEquals("Hex Bolt", f2.getDescription());
        assertEquals("HB-100", f2.getPartNumber());
        assertEquals("M10x50", f2.getSpecification());
        assertEquals("Unbrako", f2.getBrand());
        assertEquals("50", f2.getQuantity());
        assertEquals("Nos", f2.getUom());
        assertEquals("Pune", f2.getDeliveryLocation());
        assertEquals("2026-09-01", f2.getDeliveryDate());

        // 3. extractedRFQ with blank fields and empty items
        ExtractedRFQ rfqBlank = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryDate("   ")
                .items(Collections.emptyList())
                .build();

        FailedRfqRequest f3 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", email, rfqBlank, "Reason");
        assertNotNull(f3);
        assertEquals("Not Provided", f3.getDescription());
    }

    @Test
    @DisplayName("Test hasExplicitLocationInPayload all branches")
    void testHasExplicitLocationInPayloadBranches() {
        // null rfq and null email
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", null, null));

        // deliveryLocation variants
        ExtractedRFQ rfq1 = ExtractedRFQ.builder().deliveryLocation("Bengaluru").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfq1, null));

        ExtractedRFQ rfqIgnoredLoc = ExtractedRFQ.builder().deliveryLocation("Not Specified").build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqIgnoredLoc, null));

        ExtractedRFQ rfqNullStr = ExtractedRFQ.builder().deliveryLocation("null").build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqNullStr, null));

        ExtractedRFQ rfqProfile = ExtractedRFQ.builder().deliveryLocation("Registered Profile Address").build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqProfile, null));

        // deliveryCity, deliveryState, deliveryPincode
        ExtractedRFQ rfqCity = ExtractedRFQ.builder().deliveryCity("Mumbai").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqCity, null));

        ExtractedRFQ rfqState = ExtractedRFQ.builder().deliveryState("Maharashtra").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqState, null));

        ExtractedRFQ rfqPin = ExtractedRFQ.builder().deliveryPincode("560001").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqPin, null));

        // item-level location
        RFQItem itemWithLoc = RFQItem.builder().deliveryLocation("Delhi").build();
        ExtractedRFQ rfqItem = ExtractedRFQ.builder().items(List.of(itemWithLoc)).build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqItem, null));

        // email text location pattern
        EmailData emailWithLoc = EmailData.builder().body("Please deliver to: Plant in Chennai").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", null, emailWithLoc));
    }

    @Test
    @DisplayName("Test deleteTemporaryAttachments, parseQuantityFromText, isBlank, stripTrailingLabels")
    void testRemainingHelperMethods() throws Exception {
        // deleteTemporaryAttachments with null
        EmailData emailNoAtt = EmailData.builder().build();
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "deleteTemporaryAttachments", emailNoAtt));

        // deleteTemporaryAttachments with temp file
        File tempFile = File.createTempFile("rfq_test_", ".tmp");
        EmailData emailWithAtt = EmailData.builder().attachments(List.of(tempFile)).build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "deleteTemporaryAttachments", emailWithAtt);
        assertFalse(tempFile.exists());

        // parseQuantityFromText
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseQuantityFromText", (String) null));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseQuantityFromText", "   "));
        assertEquals(25.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseQuantityFromText", "25 Nos"));

        // isBlank
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isBlank", (String) null));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isBlank", "  "));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isBlank", "Not Specified"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isBlank", "Valid Value"));

        // stripTrailingLabels
        assertEquals("Ball Valve", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "stripTrailingLabels", "Ball Valve Quantity: 10"));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "stripTrailingLabels", (String) null));

        // buildDeduplicationKey
        RFQItem item = RFQItem.builder().itemDescription("Valve").build();
        String key = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildDeduplicationKey", item, "buyer@test.com", "2026-09-01", "Hyd");
        assertNotNull(key);

        // scanQuantityForItem
        EmailData scanEmail = EmailData.builder().body("Item: Hex Bolt - Quantity: 100 Nos").build();
        Object itemQty = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "scanQuantityForItem", scanEmail, "Hex Bolt");
        assertNotNull(itemQty);

        // groupItemsByCategoryLocationAndDate
        RFQItem item1 = RFQItem.builder().category("CatA").deliveryLocation("Loc1").deliveryDate("Date1").build();
        RFQItem item2 = RFQItem.builder().category("CatA").deliveryLocation("Loc1").deliveryDate("Date1").build();
        RFQItem item3 = RFQItem.builder().category("CatB").deliveryLocation("Loc2").deliveryDate("Date2").build();
        Map<?, ?> grouped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "groupItemsByCategoryLocationAndDate", List.of(item1, item2, item3), "DefaultCat", "DefaultLoc");
        assertNotNull(grouped);
        assertEquals(2, grouped.size());
    }

    @Test
    @DisplayName("Test mergeThreadContext, parseCityStatePincode, and brand salvage branch coverage")
    void testMergeThreadContextAndHelpers() {
        // 1. mergeThreadContext
        EmailData nullEmail = EmailData.builder().build();
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", nullEmail, null));

        ExtractedRFQ currentRfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("").build()))
                .build();
        EmailData threadEmail = EmailData.builder()
                .inReplyTo("<PREV-MSG-1>")
                .references("<REF-MSG-1> <REF-MSG-2>")
                .build();

        String histJson = "{\"deliveryLocation\": \"Bangalore\", \"deliveryDate\": \"2026-10-01\", \"items\": [{\"itemDescription\": \"Historical Part\", \"specification\": \"Spec A\", \"brand\": \"Brand B\", \"category\": \"Cat C\"}]}";
        EmailTransaction histTx = new EmailTransaction();
        histTx.setExtractionJson(histJson);
        Mockito.when(emailTransactionRepository.findByMessageId("<PREV-MSG-1>")).thenReturn(Optional.of(histTx));

        ExtractedRFQ merged = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", threadEmail, currentRfq);
        assertNotNull(merged);
        assertEquals("Historical Part", merged.getItems().get(0).getItemDescription());
        assertEquals("Bangalore", merged.getDeliveryLocation());
        assertEquals("2026-10-01", merged.getDeliveryDate());

        // 2. parseCityStatePincodeFromLocation cities & states
        Buyer bProfile = Buyer.builder().city("BuyerCity").state("BuyerState").pincode("560099").build();
        String[] c1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Bangalore, Karnataka 560001", null, null, null, bProfile);
        assertEquals("Bangalore", c1[0]);
        assertEquals("Karnataka", c1[1]);
        assertEquals("560001", c1[2]);

        String[] c2 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Hyderabad, Telangana 500001", null, null, null, null);
        assertEquals("Hyderabad", c2[0]);
        assertEquals("Telangana", c2[1]);

        String[] c3 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Chennai, Tamil Nadu 600001", null, null, null, null);
        assertEquals("Chennai", c3[0]);

        String[] c4 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Mumbai, Maharashtra 400001", null, null, null, null);
        assertEquals("Mumbai", c4[0]);

        String[] c5 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Kolkata, West Bengal 700001", null, null, null, null);
        assertEquals("Kolkata", c5[0]);

        String[] c6 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Pune, Maharashtra", null, null, null, null);
        assertEquals("Pune", c6[0]);

        String[] c7 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Ahmedabad, Gujarat", null, null, null, null);
        assertEquals("Ahmedabad", c7[0]);

        String[] c8 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Kakinada, Andhra Pradesh", null, null, null, null);
        assertEquals("Kakinada", c8[0]);

        String[] c9 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Jaipur, Rajasthan", null, null, null, null);
        assertEquals("Rajasthan", c9[1]);

        // 3. looksLikeBrandName
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", "   "));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", "Intel i7 16GB RAM 512GB SSD"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", "Please require Dell Laptops"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "looksLikeBrandName", "Dell / HP / Lenovo"));

        // 4. isPlausibleSpecification
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", "abc"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", "Delivery to plant"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "isPlausibleSpecification", "Stainless Steel Grade 316"));

        // 5. salvageSpecificationFromBrand
        RFQItem itemProseBrand = RFQItem.builder().brand("Intel i7 16GB RAM").specification("").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemProseBrand, "Laptop");
        assertEquals("Intel i7 16GB RAM", itemProseBrand.getSpecification());
        assertNull(itemProseBrand.getBrand());

        RFQItem itemProseWithSpec = RFQItem.builder().brand("Intel i7 16GB RAM").specification("Existing Spec").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemProseWithSpec, "Laptop");
        assertEquals("Existing Spec", itemProseWithSpec.getSpecification());
        assertNull(itemProseWithSpec.getBrand());

        // 6. resolveDeliveryLocation
        assertEquals("Registered Profile Address", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", "Not Specified", (Buyer) null));
        Buyer bAddr = Buyer.builder().address("123 Main St").city("Hyd").state("TS").pincode("500001").build();
        String resLoc = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", "Not Specified", bAddr);
        assertTrue(resLoc.contains("123 Main St"));

        // 7. extractProductFromSubject
        assertEquals("", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "extractProductFromSubject", (String) null));
        assertEquals("", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "extractProductFromSubject", "Request for Quotation"));
        assertEquals("Industrial Valves", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "extractProductFromSubject", "RFQ: Industrial Valves"));

        // 8. buildFailedRequestFromEmail branches
        EmailData emailWithSender = EmailData.builder()
                .senderEmail("alice@test.com")
                .senderName("Alice Smith")
                .subject("Requirement for Steel Plates")
                .body("Need steel plates")
                .build();
        ExtractedRFQ rfqFullItem = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder()
                        .itemDescription("Steel Plate")
                        .partCode("SP-100")
                        .specification("10mm thick")
                        .brand("Tata Steel")
                        .quantity(50.0)
                        .uom("Sheets")
                        .build()))
                .build();
        FailedRfqRequest failedReq1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", emailWithSender, rfqFullItem, "Missing Location");
        assertEquals("Steel Plate", failedReq1.getDescription());
        assertEquals("SP-100", failedReq1.getPartNumber());
        assertEquals("10mm thick", failedReq1.getSpecification());
        assertEquals("Tata Steel", failedReq1.getBrand());
        assertEquals("50", failedReq1.getQuantity());
        assertEquals("Sheets", failedReq1.getUom());

        FailedRfqRequest failedReqNull = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", emailWithSender, (ExtractedRFQ) null, "Failure");
        assertEquals("Not Provided", failedReqNull.getDescription());

        // 9. mergeThreadContext with historical items and namesItsOwnProduct = false
        String histJson2 = "{\"items\":[{\"itemDescription\":\"Gate Valve\",\"specification\":\"Class 150\",\"brand\":\"L&T\",\"category\":\"Valves\"}],\"deliveryLocation\":\"Bangalore\",\"deliveryDate\":\"2026-10-01\"}";
        EmailTransaction histTx2 = new EmailTransaction();
        histTx2.setMessageId("<msg-ref-1>");
        histTx2.setExtractionJson(histJson2);
        Mockito.when(emailTransactionRepository.findByMessageId("<msg-ref-1>")).thenReturn(Optional.of(histTx2));

        EmailData threadEmail2 = EmailData.builder()
                .inReplyTo("<msg-ref-1>")
                .references("<msg-ref-1>")
                .body("Quantity is 20 nos")
                .build();
        ExtractedRFQ unpopulatedExtracted = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("").quantity(20.0).build()))
                .build();

        ExtractedRFQ mergedRfq = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", threadEmail2, unpopulatedExtracted);
        assertEquals("Gate Valve", mergedRfq.getItems().get(0).getItemDescription());
        assertEquals("Class 150", mergedRfq.getItems().get(0).getSpecification());
        assertEquals("L&T", mergedRfq.getItems().get(0).getBrand());
        assertEquals("Bangalore", mergedRfq.getDeliveryLocation());
        assertEquals("2026-10-01", mergedRfq.getDeliveryDate());

        // 10. groupItemsByCategoryLocationAndDate
        RFQItem itemGroup1 = RFQItem.builder().category("Pumps").deliveryLocation("Mumbai").deliveryDate("2026-11-01").build();
        RFQItem itemGroup2 = RFQItem.builder().category(null).deliveryLocation(null).deliveryDate(null).build();
        Map<String, List<RFQItem>> grouped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "groupItemsByCategoryLocationAndDate", List.of(itemGroup1, itemGroup2), "DefaultCat", "DefaultLoc");
        assertNotNull(grouped);
        assertEquals(2, grouped.size());

        // 11. cleanScannedValue edge cases
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "cleanScannedValue", (String) null));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "cleanScannedValue", ",,,:::---;;;"));
        assertEquals("Valve, Item 2", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "cleanScannedValue", ": Valve ; | Item 2"));

        // 12. parseCityStatePincodeFromLocation for all major cities and states
        String[] testLocs = {
                "Site in Bengaluru 560001",
                "Plant in Hyderabad 500001",
                "Factory in Chennai 600001",
                "Warehouse in Mumbai 400001",
                "Office in Delhi 110001",
                "Unit in Kolkata 700001",
                "Plant in Pune 411001",
                "Port in Ahmedabad 380001",
                "Terminal in Kakinada 533001",
                "Karnataka area",
                "Telangana area",
                "Andhra area",
                "Maharashtra area",
                "Tamilnadu area",
                "West Bengal area",
                "Gujarat area",
                "Rajasthan area"
        };
        for (String loc : testLocs) {
            String[] res = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    emailProcessorService, "parseCityStatePincodeFromLocation", loc, "", "", "", null);
            assertNotNull(res);
            assertTrue(!res[0].isBlank() || !res[1].isBlank());
        }

        // 13. hasExplicitLocationInPayload variants
        ExtractedRFQ rfqGenericLoc = ExtractedRFQ.builder()
                .deliveryLocation("Registered Profile Address")
                .deliveryCity("Not Specified")
                .deliveryState("Not Specified")
                .deliveryPincode("Not Specified")
                .items(List.of(RFQItem.builder().deliveryLocation("null").build()))
                .build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqGenericLoc, null));

        EmailData emailAttLoc = EmailData.builder()
                .attachmentText("Destination: Hyderabad Plant, Telangana")
                .build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqGenericLoc, emailAttLoc));

        // 14. hasExplicitPurchaseQuantityInText
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "Subject", "Need qty: 100 nos", ""));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "Subject", "", "Quantity: 50 pcs"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "Subject", "Please find attached document", ""));
    }

    @Test
    @DisplayName("Test processSingleEmail: System notification email is skipped")
    void testProcessSingleEmailSystemNotification() {
        EmailData email = EmailData.builder()
                .messageId("SYS-MSG-1")
                .senderEmail("noreply@procucev.com")
                .subject("System Alert")
                .build();
        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("SKIPPED_SYSTEM_EMAIL", result);
    }

    @Test
    @DisplayName("Test processSingleEmail: Unexpected exception in buyer verification triggers catch block")
    void testProcessSingleEmailUnexpectedException() {
        EmailData email = EmailData.builder()
                .messageId("EX-MSG-1")
                .senderEmail("buyer@corp.com")
                .subject("RFQ Requirement")
                .build();
        Mockito.when(emailTransactionRepository.findByMessageId("EX-MSG-1")).thenReturn(Optional.empty());
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@corp.com"))
                .thenThrow(new RuntimeException("DB Outage"));

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail: Subject fallback item extraction and duplicate item suppression")
    void testProcessSingleEmailSubjectFallbackAndDuplicateItem() throws Exception {
        EmailData email = EmailData.builder()
                .messageId("DUP-MSG-1")
                .senderEmail("verified@buyer.com")
                .subject("RFQ: Stainless Steel Pipes")
                .body("Need 50 Nos")
                .build();

        Buyer buyer = Buyer.builder()
                .email("verified@buyer.com")
                .name("Verified Buyer")
                .verified(true)
                .userId("101")
                .orgId("201")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("DUP-MSG-1")).thenReturn(Optional.empty());
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("verified@buyer.com")).thenReturn(buyer);

        // AI extraction returns empty items -> triggers Subject fallback
        ExtractedRFQ emptyItemsRfq = ExtractedRFQ.builder().buyerEmail("verified@buyer.com").items(new ArrayList<>()).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(emptyItemsRfq);

        RFQRequest mockReq = RFQRequest.builder().rfqNumber("RFQ-DUP-1").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(ExtractedRFQ.class), any(Buyer.class), anyString(), any()))
                .thenReturn(mockReq);

        RFQResponse successResp = RFQResponse.builder().status("SUCCESS").build();
        Mockito.when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(successResp);

        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-DUP-1").build();
        Mockito.when(rfqRepository.save(any(RFQEntity.class))).thenReturn(savedEntity);

        // Throw exception on item record save to cover catch block on line 525
        Mockito.doThrow(new RuntimeException("DB Item Record Error"))
                .when(rfqItemRecordRepository).save(any(RfqItemRecord.class));

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail: Partial failure and oversized document error paths")
    void testProcessSingleEmailPartialFailureAndOversized() throws Exception {
        EmailData email = EmailData.builder()
                .messageId("PARTIAL-MSG-1")
                .senderEmail("buyer@corp.com")
                .subject("RFQ Requirement Multi")
                .body("Details enclosed")
                .build();

        Buyer buyer = Buyer.builder()
                .email("buyer@corp.com")
                .name("Buyer Corp")
                .verified(true)
                .userId("102")
                .orgId("202")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("PARTIAL-MSG-1")).thenReturn(Optional.empty());
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@corp.com")).thenReturn(buyer);

        // Multi-item across different categories
        RFQItem item1 = RFQItem.builder().itemDescription("Laptop").category("IT Hardware & Electronics").quantity(5.0).deliveryLocation("Bangalore").deliveryDate("2026-10-01").build();
        RFQItem item2 = RFQItem.builder().itemDescription("Gate Valve").category("Industrial Valves").quantity(10.0).deliveryLocation("Mumbai").deliveryDate("2026-10-05").build();
        ExtractedRFQ multiRfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(List.of(item1, item2))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(multiRfq);

        RFQRequest req1 = RFQRequest.builder().rfqNumber("RFQ-PART-1").build();
        RFQRequest req2 = RFQRequest.builder().rfqNumber("RFQ-PART-2").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(ExtractedRFQ.class), any(Buyer.class), anyString(), any()))
                .thenReturn(req1, req2);

        RFQResponse successResp = RFQResponse.builder().status("SUCCESS").build();
        RFQResponse failResp = RFQResponse.builder().status("FAILED").message("Downstream vendor unavailable").build();
        Mockito.when(rfqApiService.submitRFQ(any(RFQRequest.class)))
                .thenReturn(successResp, failResp);

        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-PART-1").build();
        Mockito.when(rfqRepository.save(any(RFQEntity.class))).thenReturn(savedEntity);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("PARTIAL_FAILURE", result);

        // Oversized document error
        EmailData emailOversized = EmailData.builder()
                .messageId("OVERSIZED-MSG-1")
                .senderEmail("buyer@corp.com")
                .subject("RFQ Requirement Oversized")
                .build();
        Mockito.when(emailTransactionRepository.findByMessageId("OVERSIZED-MSG-1")).thenReturn(Optional.empty());
        RFQItem singleItem = RFQItem.builder().itemDescription("Machine").quantity(1.0).build();
        ExtractedRFQ singleRfq = ExtractedRFQ.builder().buyerEmail("buyer@corp.com").items(List.of(singleItem)).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(emailOversized)).thenReturn(singleRfq);

        RFQResponse oversizedResp = RFQResponse.builder()
                .status(RFQApiService.STATUS_FILE_SIZE_EXCEEDED)
                .message("Attachment size exceeds 10MB limit")
                .build();
        Mockito.when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(oversizedResp);

        String resOversized = emailProcessorService.processSingleEmail(emailOversized);
        assertEquals("FILE_SIZE_EXCEEDED", resOversized);
    }

    @Test
    @DisplayName("Test processUnreadEmails full loop and exception handling")
    void testProcessUnreadEmailsFullLoop() {
        EmailData e1 = EmailData.builder().messageId("MSG-LOOP-1").senderEmail("noreply@procucev.com").subject("Alert").build();
        File tmpFile = new File("non_existent_file_xyz_123.tmp");
        EmailData e2 = EmailData.builder().messageId("MSG-LOOP-2").senderEmail("buyer@corp.com").subject("RFQ").attachments(List.of(tmpFile)).build();
        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(List.of(e1, e2));
        assertDoesNotThrow(() -> emailProcessorService.processUnreadEmails());

        // Exception in fetchUnreadEmails
        Mockito.when(emailReaderService.fetchUnreadEmails()).thenThrow(new RuntimeException("IMAP Poll Crash"));
        assertThrows(RuntimeException.class, () -> emailProcessorService.processUnreadEmails());
    }

    @Test
    @DisplayName("Test mergeThreadContext all edge cases and fallback paths")
    void testMergeThreadContextAllBranches() throws Exception {
        EmailData emailNoReply = EmailData.builder().inReplyTo(null).build();
        ExtractedRFQ rfqOrig = ExtractedRFQ.builder().buyerEmail("b@test.com").build();
        ExtractedRFQ res1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailNoReply, rfqOrig);
        assertEquals(rfqOrig, res1);

        EmailData emailReplyMissing = EmailData.builder().inReplyTo("MISSING-TX").build();
        Mockito.when(emailTransactionRepository.findByMessageId("MISSING-TX")).thenReturn(Optional.empty());
        ExtractedRFQ res2 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailReplyMissing, rfqOrig);
        assertEquals(rfqOrig, res2);

        EmailData emailReplyInvalidJson = EmailData.builder().inReplyTo("INVALID-JSON").build();
        EmailTransaction txInvalid = EmailTransaction.builder().messageId("INVALID-JSON").extractionJson("{invalid-json").build();
        Mockito.when(emailTransactionRepository.findByMessageId("INVALID-JSON")).thenReturn(Optional.of(txInvalid));
        ExtractedRFQ res3 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailReplyInvalidJson, rfqOrig);
        assertEquals(rfqOrig, res3);

        // Prior RFQ with items and current RFQ with empty item description
        ExtractedRFQ priorRfq = ExtractedRFQ.builder()
                .deliveryLocation("Pune")
                .deliveryDate("2026-11-11")
                .items(List.of(RFQItem.builder().itemDescription("Cylinder").quantity(5.0).build()))
                .build();
        EmailTransaction txValid = EmailTransaction.builder()
                .messageId("VALID-TX")
                .extractionJson(new ObjectMapper().writeValueAsString(priorRfq))
                .build();
        Mockito.when(emailTransactionRepository.findByMessageId("VALID-TX")).thenReturn(Optional.of(txValid));

        EmailData emailValidReply = EmailData.builder().inReplyTo("VALID-TX").build();
        ExtractedRFQ currentNullItems = ExtractedRFQ.builder()
                .items(new ArrayList<>(List.of(RFQItem.builder().itemDescription("").build())))
                .deliveryLocation(null)
                .deliveryDate(null)
                .build();
        ExtractedRFQ mergedNull = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailValidReply, currentNullItems);
        assertNotNull(mergedNull);
        assertEquals("Pune", mergedNull.getDeliveryLocation());
        assertEquals("2026-11-11", mergedNull.getDeliveryDate());
        assertEquals("Cylinder", mergedNull.getItems().get(0).getItemDescription());
    }

    @Test
    @DisplayName("Test scanQuantityFromEmail, scanQuantityForItem, and hasExplicitLocationInPayload all branches")
    void testScanQuantityAndExplicitLocationBranches() {
        EmailData emailWithQty = EmailData.builder()
                .body("Please send a quotation for requirement: quantity = 100 Nos")
                .build();
        Double qty = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "scanQuantityFromEmail", emailWithQty);
        assertNotNull(qty);
        assertEquals(100.0, qty);

        EmailData emailNullBody = EmailData.builder().body(null).subject(null).attachmentText(null).build();
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "scanQuantityFromEmail", emailNullBody));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "scanQuantityForItem", emailNullBody, "Item"));

        // hasExplicitLocationInPayload branches
        ExtractedRFQ rfqCity = ExtractedRFQ.builder().deliveryCity("Chennai").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqCity, null));

        ExtractedRFQ rfqState = ExtractedRFQ.builder().deliveryState("Tamil Nadu").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqState, null));

        ExtractedRFQ rfqPin = ExtractedRFQ.builder().deliveryPincode("600001").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqPin, null));

        ExtractedRFQ rfqItemLoc = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().deliveryLocation("Warehouse 4, Bangalore").build()))
                .build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqItemLoc, null));

        EmailData emailBodyLoc = EmailData.builder().body("Ship to: Plant in Ahmedabad, Gujarat").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", ExtractedRFQ.builder().build(), emailBodyLoc));

        // Additional processSingleEmail branches:
        // 1. System notification senders
        EmailData emailInvitations = EmailData.builder().messageId("MSG-SYS-1").senderEmail("invitations@procucev.com").build();
        assertEquals("SKIPPED_SYSTEM_EMAIL", emailProcessorService.processSingleEmail(emailInvitations));

        EmailData emailGmt = EmailData.builder().messageId("MSG-SYS-2").senderEmail("gmtrfq@procucev.com").build();
        assertEquals("SKIPPED_SYSTEM_EMAIL", emailProcessorService.processSingleEmail(emailGmt));

        // 2. extractedRFQ == null
        EmailData emailNullRfq = EmailData.builder().messageId("MSG-NULL-RFQ").senderEmail("buyer@corp.com").build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@corp.com")).thenReturn(Buyer.builder().email("buyer@corp.com").verified(true).build());
        Mockito.when(aiExtractionService.extractRFQFromEmail(emailNullRfq)).thenReturn(null);
        String resNullRfq = emailProcessorService.processSingleEmail(emailNullRfq);
        assertEquals("AI_FAILED", resNullRfq);

        // 3. extractedRFQ.getItems().isEmpty()
        EmailData emailEmptyItems = EmailData.builder().messageId("MSG-EMPTY-ITEMS").senderEmail("buyer@corp.com").build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(emailEmptyItems)).thenReturn(ExtractedRFQ.builder().buyerEmail("buyer@corp.com").items(List.of()).build());
        String resEmptyItems = emailProcessorService.processSingleEmail(emailEmptyItems);
        assertEquals("VALIDATION_FAILED", resEmptyItems);

        // 4. validItems.isEmpty() (item without description and subject empty)
        EmailData emailAllBlank = EmailData.builder().messageId("MSG-BLANK-ITEMS").senderEmail("buyer@corp.com").subject("").build();
        RFQItem itemBlank = RFQItem.builder().itemDescription("").build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(emailAllBlank)).thenReturn(ExtractedRFQ.builder().buyerEmail("buyer@corp.com").items(List.of(itemBlank)).build());
        String resAllBlank = emailProcessorService.processSingleEmail(emailAllBlank);
        assertEquals("VALIDATION_FAILED", resAllBlank);

        // 5. itemGroups.size() > 1 and all fail
        EmailData emailMultiFail = EmailData.builder().messageId("MSG-MULTI-FAIL").senderEmail("buyer@corp.com").subject("Multi RFQ").build();
        RFQItem item1 = RFQItem.builder().itemDescription("Valve").quantity(1.0).deliveryLocation("Pune").build();
        RFQItem item2 = RFQItem.builder().itemDescription("Pump").quantity(2.0).deliveryLocation("Mumbai").build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(emailMultiFail)).thenReturn(ExtractedRFQ.builder().buyerEmail("buyer@corp.com").items(List.of(item1, item2)).build());
        RFQResponse respFail = RFQResponse.builder().status("FAILED").message("API submission failed").build();
        Mockito.when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(respFail);
        String resMultiFail = emailProcessorService.processSingleEmail(emailMultiFail);
        assertEquals("FAILED", resMultiFail);

        // 6. Duplicate items in same email payload
        EmailData emailDupItems = EmailData.builder().messageId("MSG-DUP-ITEMS").senderEmail("buyer@corp.com").subject("Dup Items").build();
        RFQItem dup1 = RFQItem.builder().itemDescription("Gasket").quantity(10.0).deliveryLocation("Pune").build();
        RFQItem dup2 = RFQItem.builder().itemDescription("Gasket").quantity(10.0).deliveryLocation("Pune").build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(emailDupItems)).thenReturn(ExtractedRFQ.builder().buyerEmail("buyer@corp.com").items(List.of(dup1, dup2)).build());
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any()))
                .thenReturn(RFQRequest.builder().rfqNumber("RFQ-12345").build());
        RFQResponse respSuccess = RFQResponse.builder().status("SUCCESS").build();
        Mockito.when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(respSuccess);
        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-12345").buyerEmail("buyer@corp.com").status("SUCCESS").build();
        Mockito.when(rfqRepository.save(any(RFQEntity.class))).thenReturn(savedEntity);
        String resDup = emailProcessorService.processSingleEmail(emailDupItems);
        assertEquals("RFQ_CREATED", resDup);
    }

    @Test
    @DisplayName("Test brand, spec, quantity prose, clean value, and location parsing helper methods")
    void testAllProcessorHelperMethods() throws Exception {
        // looksLikeBrandName branches
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "   "));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "A".repeat(65)));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Laptop with 16 GB RAM"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Please provide Siemens"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Brand One Two Three Four Five Six Seven Eight"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Siemens / ABB / Schneider"));

        // isPlausibleSpecification branches
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "abc"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "Location: Bangalore"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "50mm Stainless Steel"));

        // salvageSpecificationFromBrand branches
        RFQItem itemValidBrand = RFQItem.builder().brand("Siemens").specification("").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemValidBrand, "Item");
        assertEquals("Siemens", itemValidBrand.getBrand());

        RFQItem itemProseNoSpec = RFQItem.builder().brand("50mm Stainless Steel Flanged Valve").specification("").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemProseNoSpec, "Item");
        assertEquals("50mm Stainless Steel Flanged Valve", itemProseNoSpec.getSpecification());
        assertNull(itemProseNoSpec.getBrand());

        RFQItem itemProseWithSpec = RFQItem.builder().brand("50mm Stainless Steel Flanged Valve").specification("Class 150").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemProseWithSpec, "Item");
        assertEquals("Class 150", itemProseWithSpec.getSpecification());
        assertNull(itemProseWithSpec.getBrand());

        // cleanScannedValue branches
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "cleanScannedValue", (String) null));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "cleanScannedValue", " | ,,,, | ---- | "));
        assertEquals("Valves, 50mm", org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "cleanScannedValue", " | Valves | 50mm | "));

        // hasExplicitPurchaseQuantityInText branches
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitPurchaseQuantityInText", "RFQ", "Quantity: 50 nos", ""));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitPurchaseQuantityInText", "RFQ", "We require ten units", ""));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitPurchaseQuantityInText", "RFQ", "500 meters of cable", ""));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitPurchaseQuantityInText", "RFQ", "General Inquiry", ""));

        // parseCityStatePincodeFromLocation all cities and states
        String[] cities = {"Bangalore", "Hyderabad", "Chennai", "Mumbai", "Delhi", "Kolkata", "Pune", "Ahmedabad", "Kakinada"};
        for (String c : cities) {
            String[] res = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    emailProcessorService, "parseCityStatePincodeFromLocation", "Plot 10, " + c + " 560001", null, null, null, null);
            assertNotNull(res);
            assertEquals(c, res[0]);
            assertEquals("560001", res[2]);
        }

        String[] states = {"Karnataka", "Telangana", "Andhra", "Maharashtra", "Tamil Nadu", "West Bengal", "Gujarat", "Rajasthan"};
        for (String s : states) {
            String[] res = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    emailProcessorService, "parseCityStatePincodeFromLocation", "Industrial Area, " + s, null, null, null, null);
            assertNotNull(res);
            assertFalse(res[1].isBlank());
        }

        // buyer profile fallback in parseCityStatePincodeFromLocation
        Buyer bProfile = Buyer.builder().city("Jaipur").state("Rajasthan").pincode("302001").build();
        String[] resBuyer = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "", null, null, null, bProfile);
        assertEquals("Jaipur", resBuyer[0]);
        assertEquals("Rajasthan", resBuyer[1]);
        assertEquals("302001", resBuyer[2]);

        // buildFailedRequestFromEmail populated
        ExtractedRFQ rfqFullFailed = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Pune")
                .deliveryDate("2026-12-01")
                .items(List.of(RFQItem.builder()
                        .itemDescription("Pump")
                        .partCode("PMP-100")
                        .specification("50HP")
                        .brand("Kirloskar")
                        .quantity(5.0)
                        .uom("Sets")
                        .build()))
                .build();
        EmailData emailFailed = EmailData.builder().messageId("MSG-FAIL-1").subject("Inquiry").senderEmail("sender@corp.com").build();
        com.portal.procucev.rfq.dto.FailedRfqRequest failedReq = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", emailFailed, rfqFullFailed, "Some Error");
        assertNotNull(failedReq);
        assertEquals("Pump", failedReq.getDescription());
        assertEquals("PMP-100", failedReq.getPartNumber());
        assertEquals("50HP", failedReq.getSpecification());
        assertEquals("Kirloskar", failedReq.getBrand());
        assertEquals("5", failedReq.getQuantity());
        assertEquals("Sets", failedReq.getUom());
        assertEquals("Pune", failedReq.getDeliveryLocation());
        assertEquals("2026-12-01", failedReq.getDeliveryDate());

        // buildFailedRequestFromEmail null
        com.portal.procucev.rfq.dto.FailedRfqRequest failedReqNull = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", emailFailed, null, "Null RFQ Error");
        assertEquals("Not Provided", failedReqNull.getDescription());
        assertEquals("sender@corp.com", failedReqNull.getBuyerEmail());

        // resolveDeliveryLocation partial and null
        Buyer bPartial = Buyer.builder().address("123 Street").city("Pune").build();
        String locPartial = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", null, bPartial);
        assertEquals("123 Street, Pune", locPartial);

        String locNullBuyer = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", null, null);
        assertEquals("Registered Profile Address", locNullBuyer);

        // hasExplicitLocationInPayload with Registered Profile Address and attachment text
        ExtractedRFQ rfqRegProf = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().deliveryLocation("Registered Profile Address").build()))
                .build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqRegProf, EmailData.builder().body("no loc").build()));

        EmailData emailAttLoc = EmailData.builder().attachmentText("Deliver to: Hyderabad Site").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", ExtractedRFQ.builder().build(), emailAttLoc));

        // mergeThreadContext comprehensive testing
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ExtractedRFQ histRfq = ExtractedRFQ.builder()
                .deliveryLocation("Historical Location")
                .deliveryDate("2026-11-15")
                .items(List.of(RFQItem.builder()
                        .itemDescription("Hydraulic Pump")
                        .specification("50 bar")
                        .brand("Bosch")
                        .category("Pumps")
                        .build()))
                .build();
        String histJson = objectMapper.writeValueAsString(histRfq);
        EmailTransaction histTx = EmailTransaction.builder().messageId("HIST-MSG-1").extractionJson(histJson).build();
        EmailTransaction emptyJsonTx = EmailTransaction.builder().messageId("EMPTY-JSON-MSG").extractionJson(null).build();

        Mockito.when(emailTransactionRepository.findByMessageId("HIST-MSG-1")).thenReturn(Optional.of(histTx));
        Mockito.when(emailTransactionRepository.findByMessageId("EMPTY-JSON-MSG")).thenReturn(Optional.of(emptyJsonTx));
        Mockito.when(emailTransactionRepository.findByMessageId("UNKNOWN-MSG")).thenReturn(Optional.empty());

        // 1. null inReplyTo and references
        EmailData emailNoThread = EmailData.builder().messageId("MSG-CURRENT").build();
        ExtractedRFQ curRfq = ExtractedRFQ.builder().build();
        assertSame(curRfq, org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailNoThread, curRfq));

        // 2. references with empty/unknown message IDs
        EmailData emailUnknownRef = EmailData.builder().messageId("MSG-CURRENT").references("UNKNOWN-MSG EMPTY-JSON-MSG").build();
        assertNotNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailUnknownRef, curRfq));

        // 3. valid thread history with multiple current items without description / spec / brand / category
        EmailData emailReply = EmailData.builder().messageId("MSG-CURRENT").inReplyTo("HIST-MSG-1").body("Please provide 20 units").build();
        ExtractedRFQ extractedBlankItems = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().quantity(20.0).build(),
                        RFQItem.builder().quantity(10.0).build()
                )))
                .build();
        ExtractedRFQ mergedThread = org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailReply, extractedBlankItems);
        assertNotNull(mergedThread);
        assertEquals("Hydraulic Pump", mergedThread.getItems().get(0).getItemDescription());
        assertEquals("50 bar", mergedThread.getItems().get(0).getSpecification());
        assertEquals("Bosch", mergedThread.getItems().get(0).getBrand());
        assertEquals("Pumps", mergedThread.getItems().get(0).getCategory());
        assertEquals("Hydraulic Pump", mergedThread.getItems().get(1).getItemDescription());
        assertEquals("Historical Location", mergedThread.getDeliveryLocation());
        assertEquals("2026-11-15", mergedThread.getDeliveryDate());

        // 4. valid thread history but email names its own product
        EmailData emailReplyOwnProd = EmailData.builder().messageId("MSG-CURRENT").inReplyTo("HIST-MSG-1").body("Product: Centrifugal Blower\nQuantity: 5").build();
        ExtractedRFQ extractedOwnProd = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(RFQItem.builder().quantity(5.0).build())))
                .build();
        ExtractedRFQ mergedOwn = org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailReplyOwnProd, extractedOwnProd);
        assertNotNull(mergedOwn);
        assertEquals("Centrifugal Blower", mergedOwn.getItems().get(0).getItemDescription());
        assertNull(mergedOwn.getItems().get(0).getSpecification());

        // 5. processSingleEmail fallback scan triggering all field extractors
        EmailData scanFallbackEmail = EmailData.builder()
                .messageId("SCAN-FALLBACK-MSG-ALL")
                .subject("Procurement Request")
                .senderEmail("buyer@corp.com")
                .body("""
                Product: Heavy Duty Industrial Ball Valve
                Technical Specifications: 50mm Stainless Steel 316 Class 150 Flanged
                Brand: Siemens
                Required Delivery Date: 2026-12-25
                Delivery Location: Industrial Area, Pune 411001
                Heavy Duty Industrial Ball Valve - 50 Sets
                """)
                .build();

        ExtractedRFQ rfqNeedsScan = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("RFQ Procurement Item")
                                .specification("")
                                .brand("Not Specified")
                                .deliveryLocation("Not Specified")
                                .deliveryDate("Not Specified")
                                .quantity(0.0)
                                .uom("")
                                .build()
                )))
                .build();

        Buyer validBuyer = Buyer.builder().email("buyer@corp.com").name("Buyer").verified(true).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(scanFallbackEmail)).thenReturn(rfqNeedsScan);
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@corp.com")).thenReturn(validBuyer);
        Mockito.when(rfqBuilderService.buildRFQRequest(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(RFQRequest.builder().rfqNumber("RFQ-SCAN-1").build());
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").build());

        String scanResult = emailProcessorService.processSingleEmail(scanFallbackEmail);
        assertEquals("RFQ_CREATED", scanResult);

        // 6. Partial success with oversized document failure in second group
        EmailData emailPartialOversized = EmailData.builder()
                .messageId("PARTIAL-OVERSIZED-MSG")
                .subject("Two Locations Order")
                .senderEmail("buyer@corp.com")
                .build();

        ExtractedRFQ rfqTwoLocs = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Item 1").quantity(5.0).deliveryLocation("Mumbai").deliveryDate("2026-11-01").category("Cat A").build(),
                        RFQItem.builder().itemDescription("Item 2").quantity(10.0).deliveryLocation("Delhi").deliveryDate("2026-11-10").category("Cat B").build()
                )))
                .build();

        Mockito.when(aiExtractionService.extractRFQFromEmail(emailPartialOversized)).thenReturn(rfqTwoLocs);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").build())
                .thenReturn(RFQResponse.builder().status(RFQApiService.STATUS_FILE_SIZE_EXCEEDED).message("File too large").build());

        String partialRes = emailProcessorService.processSingleEmail(emailPartialOversized);
        assertEquals("PARTIAL_FAILURE", partialRes);

        // 7. extractProductFromSubject keyword variations
        String[] emptySubjects = {"RFQ", "Request for Quotation", "Inquiry", "(no subject)", "no subject", "RE: RFQ", "FWD: (no subject)"};
        for (String subj : emptySubjects) {
            String prod = org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "extractProductFromSubject", subj);
            assertEquals("", prod);
        }

        // 8. salvageSpecificationFromBrand variations
        RFQItem itemWithNullBrandStr = RFQItem.builder().brand("null").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemWithNullBrandStr, "Desc");
        assertEquals("null", itemWithNullBrandStr.getBrand());

        RFQItem itemWithNotSpecBrandStr = RFQItem.builder().brand("Not Specified").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemWithNotSpecBrandStr, "Desc");
        assertEquals("Not Specified", itemWithNotSpecBrandStr.getBrand());

        RFQItem itemWithExistingSpec = RFQItem.builder()
                .brand("This is a very long descriptive requirement text that is not a brand")
                .specification("Existing Specification")
                .build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemWithExistingSpec, "Desc");
        assertEquals("Existing Specification", itemWithExistingSpec.getSpecification());
        assertNull(itemWithExistingSpec.getBrand());

        // 9. buildFailedRequestFromEmail with null/empty items and null extractedRFQ
        EmailData failEmail = EmailData.builder().messageId("FAIL-1").senderEmail("sender@corp.com").subject("Failure").build();
        FailedRfqRequest failNullRfq = org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "buildFailedRequestFromEmail", failEmail, (ExtractedRFQ) null, "Null RFQ");
        assertNotNull(failNullRfq);
        assertEquals("sender@corp.com", failNullRfq.getBuyerEmail());
        assertEquals("Not Provided", failNullRfq.getDescription());

        ExtractedRFQ rfqEmptyItems = ExtractedRFQ.builder().buyerEmail(null).items(List.of()).build();
        FailedRfqRequest failEmpty = org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "buildFailedRequestFromEmail", failEmail, rfqEmptyItems, "Empty items");
        assertNotNull(failEmpty);
        assertEquals("sender@corp.com", failEmpty.getBuyerEmail());

        // 10. hasExplicitLocationInPayload variations (null, Not Specified, Registered Profile Address)
        ExtractedRFQ locNotSpec = ExtractedRFQ.builder()
                .deliveryLocation("Not Specified")
                .deliveryCity("Not Specified")
                .deliveryState("Not Specified")
                .deliveryPincode("Not Specified")
                .items(List.of(
                        RFQItem.builder().deliveryLocation("null").build(),
                        RFQItem.builder().deliveryLocation("Registered Profile Address").build(),
                        RFQItem.builder().deliveryLocation("Not Specified").build()
                ))
                .build();
        EmailData emailNoLoc = EmailData.builder().subject(null).body(null).attachmentText(null).build();
        boolean hasLoc = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", locNotSpec, emailNoLoc);
        assertFalse(hasLoc);

        ExtractedRFQ locRegistered = ExtractedRFQ.builder()
                .deliveryLocation("Registered Profile Address")
                .build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", locRegistered, emailNoLoc));

        ExtractedRFQ locNullStr = ExtractedRFQ.builder()
                .deliveryLocation("null")
                .build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", locNullStr, emailNoLoc));

        // 11. scanQuantityFromEmail edge cases (null email, null source, empty tail, invalid parsed)
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", (EmailData) null));
        EmailData emailInvalidQty = EmailData.builder().body("Quantity: \nRequired Qty: invalid_number").build();
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emailInvalidQty));

        // 12. scanQuantityForItem edge cases
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityForItem", (EmailData) null, "Item"));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityForItem", emailInvalidQty, (String) null));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityForItem", emailInvalidQty, "   "));

        // 13. scanFieldFromEmail with null email and body fallback to attachment
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanFieldFromEmail", (EmailData) null, ".*"));
        EmailData emailAttOnly = EmailData.builder().body("").attachmentText("Brand: Schneider").build();
        String scannedFromAtt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "scanFieldFromEmail", emailAttOnly, "(?i)brand[:\\s]+([^\\r\\n]+)");
        assertEquals("Schneider", scannedFromAtt);

        // 14. hasExplicitPurchaseQuantityInText variations
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", (String) null, (String) null, (String) null));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", "Subject", "We need ten valves", null));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitPurchaseQuantityInText", null, null, "100 drums required"));

        // 15. processSingleEmail with null item, top location recovery, and rfqItemRecordRepository exception
        EmailData topLocEmail = EmailData.builder()
                .messageId("TOP-LOC-MSG")
                .subject("Requirement")
                .senderEmail("buyer@corp.com")
                .body("Delivery Address: Bangalore 560001\nNeed 10 units")
                .build();
        ExtractedRFQ rfqWithNullItem = ExtractedRFQ.builder()
                .deliveryLocation("Not Specified")
                .items(new java.util.ArrayList<>(java.util.Arrays.asList(
                        (RFQItem) null,
                        RFQItem.builder().itemDescription("Item With Top Loc").quantity(10.0).build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(topLocEmail)).thenReturn(rfqWithNullItem);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").build());
        Mockito.when(rfqItemRecordRepository.save(Mockito.any())).thenThrow(new RuntimeException("DB Exception"));

        String topLocResult = emailProcessorService.processSingleEmail(topLocEmail);
        assertEquals("RFQ_CREATED", topLocResult);

        // 16. resolveDeliveryLocation variations
        assertEquals("Registered Profile Address", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", (String) null, (Buyer) null));

        Buyer buyerBlank = Buyer.builder().address("   ").city("   ").state("   ").pincode("   ").build();
        assertEquals("Registered Profile Address", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", "Not Specified", buyerBlank));

        Buyer buyerCityOnly = Buyer.builder().city("Pune").build();
        assertEquals("Pune", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", "", buyerCityOnly));

        Buyer buyerStateOnly = Buyer.builder().state("Maharashtra").build();
        assertEquals("Maharashtra", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", "", buyerStateOnly));

        Buyer buyerPinOnly = Buyer.builder().pincode("411001").build();
        assertEquals("411001", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "resolveDeliveryLocation", "", buyerPinOnly));

        // 17. buildDeduplicationKey with null fields
        RFQItem itemNullKeyFields = RFQItem.builder().itemDescription("Item").quantity(null).deliveryDate(null).deliveryLocation(null).build();
        String dedupKeyNull = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildDeduplicationKey", itemNullKeyFields, (String) null, "2026-12-01", "DefaultLoc");
        assertNotNull(dedupKeyNull);
        assertTrue(dedupKeyNull.contains("2026-12-01"));
        assertTrue(dedupKeyNull.contains("defaultloc"));

        // 18. Duplicate items in same email and quantity defaulting to 1.0
        EmailData dupEmail = EmailData.builder()
                .messageId("DUP-ITEMS-MSG")
                .subject("Duplicate Requirement")
                .senderEmail("buyer@corp.com")
                .body("Duplicate items")
                .build();
        ExtractedRFQ rfqDup = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(java.util.Arrays.asList(
                        RFQItem.builder().itemDescription("Duplicate Item").quantity(null).uom(null).build(),
                        RFQItem.builder().itemDescription("Duplicate Item").quantity(null).uom(null).build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(dupEmail)).thenReturn(rfqDup);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").build());

        String dupResult = emailProcessorService.processSingleEmail(dupEmail);
        assertEquals("RFQ_CREATED", dupResult);

        // 19. General API failure returning "FAILED"
        EmailData failApiEmail = EmailData.builder()
                .messageId("FAIL-API-MSG")
                .subject("API Failure")
                .senderEmail("buyer@corp.com")
                .body("API fail body")
                .build();
        ExtractedRFQ rfqFailApi = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item For API Fail").quantity(10.0).build()))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(failApiEmail)).thenReturn(rfqFailApi);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("FAILED").message("Internal API error").build());

        String apiFailResult = emailProcessorService.processSingleEmail(failApiEmail);
        assertEquals("FAILED", apiFailResult);

        // 20. parseCityStatePincodeFromLocation state variations
        String[] resTel = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Delivery to Telangana region", "", "", "", null);
        assertEquals("Telangana", resTel[1]);

        String[] resAndhra = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Delivery to Andhra area", "", "", "", null);
        assertEquals("Andhra Pradesh", resAndhra[1]);

        String[] resTN = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Delivery to Tamil Nadu", "", "", "", null);
        assertEquals("Tamil Nadu", resTN[1]);

        String[] resWB = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Delivery to West Bengal", "", "", "", null);
        assertEquals("West Bengal", resWB[1]);

        String[] resGuj = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Delivery to Gujarat", "", "", "", null);
        assertEquals("Gujarat", resGuj[1]);

        String[] resRaj = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Delivery to Rajasthan", "", "", "", null);
        assertEquals("Rajasthan", resRaj[1]);

        // 21. cleanScannedValue variations
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "cleanScannedValue", (String) null));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "cleanScannedValue", "||| ,,, ;;; |||"));
        assertEquals("Part A, Part B", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "cleanScannedValue", "Part A | Part B"));

        // 22. looksLikeBrandName variations
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "   "));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Very Long Brand Name Exceeding Maximum Limit of Characters"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "16 GB RAM with 512 GB SSD"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "please supply our brand"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "One Two Three Four Five Six Seven Eight"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Dell / HP / Lenovo"));

        // 23. isPlausibleSpecification variations
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", (String) null));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "ab"));
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "Delivery to Bangalore"));
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "Stainless Steel Grade 316"));

        // 24. processSingleEmail where all items have blank descriptions
        EmailData allBlankItemsEmail = EmailData.builder()
                .messageId("ALL-BLANK-DESC-MSG")
                .subject("RFQ")
                .senderEmail("buyer@corp.com")
                .body("Body text")
                .build();
        ExtractedRFQ rfqAllBlankDesc = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("   ").build()))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(allBlankItemsEmail)).thenReturn(rfqAllBlankDesc);

        String allBlankResult = emailProcessorService.processSingleEmail(allBlankItemsEmail);
        assertEquals("VALIDATION_FAILED", allBlankResult);

        // 25. mergeThreadContext comprehensive branch coverage
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", EmailData.builder().build(), (ExtractedRFQ) null));

        EmailData emailNoThread2 = EmailData.builder().inReplyTo(null).references(null).build();
        ExtractedRFQ rfqNoThread2 = ExtractedRFQ.builder().build();
        assertSame(rfqNoThread2, org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailNoThread2, rfqNoThread2));

        // Prior empty / null json
        EmailData emailWithRef = EmailData.builder().inReplyTo("NON-EXIST-ID").references("REF-1 REF-2").build();
        Mockito.when(emailTransactionRepository.findByMessageId("NON-EXIST-ID")).thenReturn(Optional.empty());
        Mockito.when(emailTransactionRepository.findByMessageId("REF-1")).thenReturn(Optional.of(EmailTransaction.builder().extractionJson(null).build()));
        Mockito.when(emailTransactionRepository.findByMessageId("REF-2")).thenReturn(Optional.of(EmailTransaction.builder().extractionJson("{invalid json}").build()));
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailWithRef, rfqNoThread2);

        // Prior with valid historical extraction and current email naming its own product
        String histJson2 = """
                {
                  "deliveryLocation": "Hist Location",
                  "deliveryDate": "2026-12-12",
                  "items": [
                    {"itemDescription": "Hist Item 1", "specification": "Hist Spec", "brand": "Hist Brand", "category": "Hist Cat"}
                  ]
                }
                """;
        Mockito.when(emailTransactionRepository.findByMessageId("REF-2")).thenReturn(Optional.of(EmailTransaction.builder().extractionJson(histJson2).build()));

        EmailData emailNamesProduct = EmailData.builder()
                .references("REF-2")
                .body("Item: New Product Name\nQuantity: 10")
                .build();
        ExtractedRFQ rfqCurrent = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("").build(),
                        RFQItem.builder().itemDescription("").build()
                )))
                .build();

        ExtractedRFQ merged1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailNamesProduct, rfqCurrent);
        assertNotNull(merged1);
        assertEquals("Hist Location", merged1.getDeliveryLocation());
        assertEquals("Hist Location", rfqCurrent.getDeliveryLocation());

        // Prior with valid historical extraction and current email NOT naming its own product
        EmailData emailNoName = EmailData.builder().references("REF-2").body("Just quantity 50").build();
        ExtractedRFQ rfqCurrentBlankFields = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription(null).specification(null).brand(null).category(null).build(),
                        RFQItem.builder().itemDescription("").specification("").brand("").category("").build()
                )))
                .build();

        ExtractedRFQ merged2 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailNoName, rfqCurrentBlankFields);
        assertNotNull(merged2);
        assertEquals("Hist Item 1", rfqCurrentBlankFields.getItems().get(0).getItemDescription());
        assertEquals("Hist Spec", rfqCurrentBlankFields.getItems().get(0).getSpecification());
        assertEquals("Hist Brand", rfqCurrentBlankFields.getItems().get(0).getBrand());
        assertEquals("Hist Cat", rfqCurrentBlankFields.getItems().get(0).getCategory());

        // 26. buildFailedRequestFromEmail comprehensive branch coverage
        EmailData failEmail2 = EmailData.builder().messageId("FAIL-MSG-ID").subject("Fail Subject").senderEmail("sender@corp.com").build();
        FailedRfqRequest fail1 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", failEmail2, (ExtractedRFQ) null, "Reason A");
        assertEquals("Not Provided", fail1.getDescription());
        assertEquals("sender@corp.com", fail1.getBuyerEmail());

        ExtractedRFQ rfqFailEmpty = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("")
                .deliveryDate("")
                .items(List.of())
                .build();
        FailedRfqRequest fail2 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", failEmail2, rfqFailEmpty, "Reason B");
        assertEquals("buyer@corp.com", fail2.getBuyerEmail());

        ExtractedRFQ rfqFailPopulated = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Plant Loc")
                .deliveryDate("2026-11-11")
                .items(List.of(RFQItem.builder()
                        .itemDescription("Item 1")
                        .partCode("P-1")
                        .specification("Spec 1")
                        .brand("Brand 1")
                        .quantity(10.0)
                        .uom("Nos")
                        .build()))
                .build();
        FailedRfqRequest fail3 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", failEmail2, rfqFailPopulated, "Reason C");
        assertEquals("Item 1", fail3.getDescription());
        assertEquals("Plant Loc", fail3.getDeliveryLocation());
        assertEquals("10", fail3.getQuantity());

        ExtractedRFQ rfqFailNegativeQty = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("").quantity(-5.0).uom("").build()))
                .build();
        FailedRfqRequest fail4 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", failEmail2, rfqFailNegativeQty, "Reason D");
        assertEquals("Not Provided", fail4.getDescription());
        assertEquals("Not Provided", fail4.getQuantity());

        // 27. salvageSpecificationFromBrand variations
        RFQItem itemBrandNull = RFQItem.builder().brand(null).build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemBrandNull, "Item A");
        assertNull(itemBrandNull.getBrand());

        RFQItem itemBrandNotSpec = RFQItem.builder().brand("Not Specified").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemBrandNotSpec, "Item A");

        RFQItem itemBrandReal = RFQItem.builder().brand("Dell").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemBrandReal, "Item A");
        assertEquals("Dell", itemBrandReal.getBrand());

        RFQItem itemBrandProseSpecMissing = RFQItem.builder().brand("16 GB RAM with 512 GB SSD").specification("Not Specified").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemBrandProseSpecMissing, "Item A");
        assertEquals("16 GB RAM with 512 GB SSD", itemBrandProseSpecMissing.getSpecification());
        assertNull(itemBrandProseSpecMissing.getBrand());

        RFQItem itemBrandProseSpecPresent = RFQItem.builder().brand("16 GB RAM with 512 GB SSD").specification("Existing Spec").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "salvageSpecificationFromBrand", itemBrandProseSpecPresent, "Item A");
        assertEquals("Existing Spec", itemBrandProseSpecPresent.getSpecification());
        assertNull(itemBrandProseSpecPresent.getBrand());

        // 28. groupItemsByCategoryLocationAndDate variations
        List<RFQItem> groupTestItems = List.of(
                RFQItem.builder().itemDescription("Item 1").deliveryLocation("Loc 1").deliveryDate("2026-10-10").build(),
                RFQItem.builder().itemDescription("Item 2").deliveryLocation("").deliveryDate("").build()
        );
        Map<String, List<RFQItem>> resGroups = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "groupItemsByCategoryLocationAndDate", groupTestItems, "Default Loc", "2026-01-01");
        assertNotNull(resGroups);
        assertEquals(2, resGroups.size());

        // 29. processSingleEmail with missing item fields recovered via email body fallback scans
        EmailData scanRecoveryEmail = EmailData.builder()
                .messageId("SCAN-RECOVERY-MSG")
                .subject("Requirement for Steel Valves")
                .senderEmail("buyer@corp.com")
                .body("""
                      Please provide quote for:
                      Item: Steel Valves
                      Specification: Stainless Steel Grade 316
                      Brand: L&T
                      Delivery Location: Mumbai Port
                      Required Delivery Date: 2026-12-15
                      Quantity: 50 Nos
                      """)
                .build();

        ExtractedRFQ rfqMissingItemFields = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Steel Valves")
                                .specification("Not Specified")
                                .brand("Not Specified")
                                .deliveryLocation("Not Specified")
                                .deliveryDate("Not Specified")
                                .quantity(0.0)
                                .build()
                )))
                .build();

        Mockito.when(aiExtractionService.extractRFQFromEmail(scanRecoveryEmail)).thenReturn(rfqMissingItemFields);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-SCAN-1").build());

        String scanResult2 = emailProcessorService.processSingleEmail(scanRecoveryEmail);
        assertEquals("RFQ_CREATED", scanResult2);
        assertEquals("Stainless Steel Grade 316", rfqMissingItemFields.getItems().get(0).getSpecification());
        assertEquals("L&T", rfqMissingItemFields.getItems().get(0).getBrand());
        assertEquals("Mumbai Port", rfqMissingItemFields.getItems().get(0).getDeliveryLocation());
        assertEquals("2026-12-15", rfqMissingItemFields.getItems().get(0).getDeliveryDate());
        assertEquals(50.0, rfqMissingItemFields.getItems().get(0).getQuantity());

        // 30. processSingleEmail when API returns FILE_SIZE_EXCEEDED
        EmailData oversizedEmail = EmailData.builder()
                .messageId("OVERSIZED-DOC-MSG")
                .subject("Big Attachment RFQ")
                .senderEmail("buyer@corp.com")
                .body("PFA requirement")
                .build();
        ExtractedRFQ rfqOversized = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(List.of(RFQItem.builder().itemDescription("Item With Big Doc").quantity(10.0).build()))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(oversizedEmail)).thenReturn(rfqOversized);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("FILE_SIZE_EXCEEDED").message("File exceeds 10MB").build());

        String sizeResult = emailProcessorService.processSingleEmail(oversizedEmail);
        assertEquals("FILE_SIZE_EXCEEDED", sizeResult);

        // 31. hasExplicitLocationInPayload full branch coverage
        ExtractedRFQ rfqCityNotSpec = ExtractedRFQ.builder().deliveryCity("Not Specified").build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqCityNotSpec, null));

        ExtractedRFQ rfqCityExplicit = ExtractedRFQ.builder().deliveryCity("Bangalore").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqCityExplicit, null));

        ExtractedRFQ rfqStateNotSpec = ExtractedRFQ.builder().deliveryState("Not Specified").build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqStateNotSpec, null));

        ExtractedRFQ rfqStateExplicit = ExtractedRFQ.builder().deliveryState("Karnataka").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqStateExplicit, null));

        ExtractedRFQ rfqPinNotSpec = ExtractedRFQ.builder().deliveryPincode("Not Specified").build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqPinNotSpec, null));

        ExtractedRFQ rfqPinExplicit = ExtractedRFQ.builder().deliveryPincode("560001").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqPinExplicit, null));

        ExtractedRFQ rfqItemLocVariants = ExtractedRFQ.builder().items(List.of(
                RFQItem.builder().deliveryLocation("Not Specified").build(),
                RFQItem.builder().deliveryLocation("null").build(),
                RFQItem.builder().deliveryLocation("Registered Profile Address").build()
        )).build();
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqItemLocVariants, null));

        ExtractedRFQ rfqItemLocExplicit = ExtractedRFQ.builder().items(List.of(
                RFQItem.builder().deliveryLocation("Plant Site 1").build()
        )).build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", rfqItemLocExplicit, null));

        EmailData emailAttLoc2 = EmailData.builder().subject(null).body(null).attachmentText("Plant: Pune Industrial").build();
        assertTrue((Boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "hasExplicitLocationInPayload", null, emailAttLoc2));

        // 32. processSingleEmail with implausible spec, prose brand, null topLocation / topDate
        EmailData edgeBodyEmail = EmailData.builder()
                .messageId("EDGE-BODY-MSG")
                .subject("Requirement for Industrial Valves")
                .senderEmail("buyer@corp.com")
                .body("""
                      Requirement details:
                      Specification: Delivery Location: Bangalore
                      Brand: Please deliver this urgently as soon as possible
                      Delivery Location: Pune Plant
                      Required Delivery Date: 2026-11-20
                      Quantity: 25 Nos
                      """)
                .build();

        ExtractedRFQ rfqEdgeBody = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryDate("null")
                .deliveryLocation("Registered Profile Address")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription(null)
                                .specification("Not Specified")
                                .brand("Not Specified")
                                .deliveryLocation("null")
                                .deliveryDate("Not Specified")
                                .quantity(0.0)
                                .build()
                )))
                .build();

        Mockito.when(aiExtractionService.extractRFQFromEmail(edgeBodyEmail)).thenReturn(rfqEdgeBody);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-EDGE-32").build());

        String edgeResult = emailProcessorService.processSingleEmail(edgeBodyEmail);
        assertEquals("RFQ_CREATED", edgeResult);
        assertEquals("Requirement for Industrial Valves", rfqEdgeBody.getItems().get(0).getItemDescription());
        assertNotEquals("Delivery Location: Bangalore", rfqEdgeBody.getItems().get(0).getSpecification());
        assertEquals("Bangalore", rfqEdgeBody.getItems().get(0).getDeliveryLocation());
        assertEquals("2026-11-20", rfqEdgeBody.getItems().get(0).getDeliveryDate());
        assertEquals(25.0, rfqEdgeBody.getItems().get(0).getQuantity());

        // 33. Multi-item payload with duplicate item, per-item quantity scan, and multi-group failure
        EmailData multiGroupEmail = EmailData.builder()
                .messageId("MULTI-GROUP-MSG")
                .subject("RFQ for Valves and Pipes")
                .senderEmail("buyer@corp.com")
                .body("""
                      Requirement details:
                      Steel Valves - 100 Nos
                      Copper Pipes - 50 Meters
                      """)
                .build();

        ExtractedRFQ rfqMultiGroup = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Mumbai")
                .deliveryDate("2026-11-30")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Steel Valves").quantity(0.0).deliveryLocation("Mumbai").deliveryDate("2026-11-30").build(),
                        RFQItem.builder().itemDescription("Steel Valves").quantity(100.0).deliveryLocation("Mumbai").deliveryDate("2026-11-30").build(),
                        RFQItem.builder().itemDescription("Copper Pipes").quantity(50.0).deliveryLocation("Pune").deliveryDate("2026-12-15").build()
                )))
                .build();

        Mockito.when(aiExtractionService.extractRFQFromEmail(multiGroupEmail)).thenReturn(rfqMultiGroup);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("ERROR").message("Internal submission error").build());

        String multiResult = emailProcessorService.processSingleEmail(multiGroupEmail);
        assertEquals("FAILED", multiResult);

        // 34. processSingleEmail with PARTIAL_FAILURE
        EmailData partialEmail = EmailData.builder()
                .messageId("PARTIAL-FAIL-MSG")
                .subject("RFQ for Valves and Pipes")
                .senderEmail("buyer@corp.com")
                .body("Requirement")
                .build();

        ExtractedRFQ rfqPartial = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Mumbai")
                .deliveryDate("2026-11-30")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Steel Valves").quantity(10.0).deliveryLocation("Mumbai").deliveryDate("2026-11-30").build(),
                        RFQItem.builder().itemDescription("Copper Pipes").quantity(20.0).deliveryLocation("Pune").deliveryDate("2026-12-15").build()
                )))
                .build();

        Mockito.when(aiExtractionService.extractRFQFromEmail(partialEmail)).thenReturn(rfqPartial);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(
                        RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-PARTIAL-1").build(),
                        RFQResponse.builder().status("ERROR").message("Second group failed").build()
                );

        String partialResult = emailProcessorService.processSingleEmail(partialEmail);
        assertEquals("PARTIAL_FAILURE", partialResult);

        // 35. processSingleEmail unhandled exception in outer block with nested catch exceptions
        EmailData fatalEmail = EmailData.builder()
                .messageId("FATAL-MSG")
                .subject("Fatal test")
                .senderEmail("fatal@corp.com")
                .build();

        Mockito.when(buyerVerificationService.verifyAndGetBuyer("fatal@corp.com"))
                .thenThrow(new RuntimeException("Fatal DB Crash"));
        Mockito.doThrow(new RuntimeException("Mail error in catch"))
                .when(acknowledgementEmailService).sendProcessingFailureAcknowledgement(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.doThrow(new RuntimeException("Move error in catch"))
                .when(emailReaderService).moveMessageToFolder(Mockito.eq("FATAL-MSG"), Mockito.anyString());

        String fatalResult = emailProcessorService.processSingleEmail(fatalEmail);
        assertEquals("FAILED", fatalResult);

        // 36. scanQuantityFromEmail branch tests
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", (EmailData) null));
        
        EmailData emailQtyAtt = EmailData.builder().body(null).subject("").attachmentText("Total Requirement Quantity: 500 Nos").build();
        assertEquals(500.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emailQtyAtt));

        EmailData emailQtySub = EmailData.builder().body(null).attachmentText(null).subject("RFQ Order Quantity: 150 Units").build();
        assertEquals(150.0, (Double) org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emailQtySub));

        EmailData emailQtyEmptyTail = EmailData.builder().body("Quantity: \n").attachmentText(null).subject(null).build();
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emailQtyEmptyTail));

        // 37. mergeThreadContext branch tests
        EmailData emailNoThread37 = EmailData.builder().inReplyTo(null).references(null).build();
        ExtractedRFQ rfqNoThread37 = ExtractedRFQ.builder().build();
        assertSame(rfqNoThread37, org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailNoThread37, rfqNoThread37));
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailNoThread37, (ExtractedRFQ) null));

        // inReplyTo only with namesItsOwnProduct = true
        EmailData emailReplyOnly37 = EmailData.builder()
                .inReplyTo("MSG-PRIOR-1")
                .references(null)
                .body("Product: Custom Valves\nQuantity is 20")
                .build();
        ExtractedRFQ rfqCurrent37 = ExtractedRFQ.builder()
                .deliveryLocation(null)
                .deliveryDate(null)
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription(null).specification(null).brand(null).category(null).build(),
                        RFQItem.builder().itemDescription(null).specification(null).brand(null).category(null).build()
                )))
                .build();

        String histJson37 = """
                {
                    "deliveryLocation": "Old Plant Location",
                    "deliveryDate": "2026-10-15",
                    "items": [
                        {"itemDescription": "Old Hex Bolt", "specification": "Grade 8.8", "brand": "Unbrako", "category": "Fasteners"}
                    ]
                }
                """;
        EmailTransaction histTx37 = new EmailTransaction();
        histTx37.setMessageId("MSG-PRIOR-1");
        histTx37.setExtractionJson(histJson37);

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-PRIOR-1")).thenReturn(Optional.of(histTx37));

        ExtractedRFQ mergedOwn37 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailReplyOnly37, rfqCurrent37);
        assertNotNull(mergedOwn37);
        assertEquals("Custom Valves", mergedOwn37.getItems().get(0).getItemDescription());
        assertEquals("Old Plant Location", mergedOwn37.getDeliveryLocation());
        assertEquals("2026-10-15", mergedOwn37.getDeliveryDate());

        // references only with namesItsOwnProduct = false and corrupt JSON
        EmailData emailRefOnly37 = EmailData.builder()
                .inReplyTo(null)
                .references("MSG-PRIOR-2 MSG-PRIOR-BAD")
                .body("Quantity is 20 Nos")
                .build();
        EmailTransaction histTx237 = new EmailTransaction();
        histTx237.setMessageId("MSG-PRIOR-2");
        histTx237.setExtractionJson("{INVALID_JSON}");

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-PRIOR-2")).thenReturn(Optional.of(histTx237));

        ExtractedRFQ mergedBad37 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "mergeThreadContext", emailRefOnly37, rfqCurrent37);
        assertNotNull(mergedBad37);

        // 38. buildFailedRequestFromEmail
        EmailData failEmail38 = EmailData.builder().senderEmail("sender@corp.com").subject("Fail Subject").messageId("FAIL-MSG-ID").build();
        FailedRfqRequest failNullRfq38 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", failEmail38, (ExtractedRFQ) null, "Failure Reason");
        assertNotNull(failNullRfq38);
        assertEquals("sender@corp.com", failNullRfq38.getBuyerEmail());
        assertEquals("Not Provided", failNullRfq38.getDescription());

        ExtractedRFQ fullFailRfq38 = ExtractedRFQ.builder()
                .buyerEmail("buyer@fail.com")
                .deliveryLocation("Plant Alpha")
                .deliveryDate("2026-11-20")
                .items(List.of(
                        RFQItem.builder()
                                .itemDescription("Fail Item")
                                .partNumber("PC-999")
                                .specification("Grade A")
                                .brand("Apex")
                                .quantity(10.0)
                                .uom("Nos")
                                .build()
                ))
                .build();
        FailedRfqRequest failFullRfq38 = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "buildFailedRequestFromEmail", failEmail38, fullFailRfq38, "Full Failure");
        assertNotNull(failFullRfq38);
        assertEquals("buyer@fail.com", failFullRfq38.getBuyerEmail());
        assertEquals("Fail Item", failFullRfq38.getDescription());
        assertEquals("PC-999", failFullRfq38.getPartNumber());
        assertEquals("10", failFullRfq38.getQuantity());

        // 39. deleteTemporaryAttachments
        EmailData emailNullAtt = EmailData.builder().attachments(null).build();
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "deleteTemporaryAttachments", emailNullAtt));

        java.io.File mockFile = Mockito.mock(java.io.File.class);
        Mockito.when(mockFile.toPath()).thenThrow(new SecurityException("Access Denied"));
        EmailData emailBadAtt = EmailData.builder().attachments(List.of(mockFile)).build();
        assertDoesNotThrow(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "deleteTemporaryAttachments", emailBadAtt));

        // 40. parseCityStatePincodeFromLocation with kakinada and buyer fallbacks
        Buyer buyerWithLoc = Buyer.builder().city("Buyer City").state("Buyer State").pincode("500081").build();
        String[] kakRes = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "Delivery to Kakinada port 533001", "", "", "", null);
        assertEquals("Kakinada", kakRes[0]);
        assertEquals("Andhra Pradesh", kakRes[1]);
        assertEquals("533001", kakRes[2]);

        String[] buyerFallbackRes = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                emailProcessorService, "parseCityStatePincodeFromLocation", "", "", "", "", buyerWithLoc);
        assertEquals("Buyer City", buyerFallbackRes[0]);
        assertEquals("Buyer State", buyerFallbackRes[1]);
        assertEquals("500081", buyerFallbackRes[2]);

        // 41. Ignored system notification senders and null sender
        for (String sysSender : List.of("notification@corp.com", "rfq@procucev.com", "rfqprocucev@gmail.com", "veerababu.v@procucev.com", "no-reply@domain.com")) {
            EmailData sysEmail = EmailData.builder().senderEmail(sysSender).messageId("SYS-MSG-" + sysSender).build();
            assertEquals("SKIPPED_SYSTEM_EMAIL", emailProcessorService.processSingleEmail(sysEmail));
        }
        Mockito.when(buyerVerificationService.verifyAndGetBuyer(""))
                .thenReturn(Buyer.builder().email("").verified(false).build());
        EmailData nullSenderEmail = EmailData.builder().senderEmail(null).messageId("NULL-SENDER-MSG").build();
        assertEquals("INVALID_BUYER", emailProcessorService.processSingleEmail(nullSenderEmail));

        // 42. File size exceeded with buyer verification exception
        EmailData oversizedExEmail = EmailData.builder()
                .senderEmail("buyer@oversized.com")
                .messageId("OVERSIZED-EX-MSG")
                .fileSizeExceeded(true)
                .errorMessage("Attachment too large")
                .build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@oversized.com"))
                .thenThrow(new RuntimeException("Buyer service down"));
        assertEquals("FILE_SIZE_EXCEEDED", emailProcessorService.processSingleEmail(oversizedExEmail));

        // 43. Fallback recovery for null strings, brand, date, and UOM recovery beside item
        EmailData fallbackRecoveryEmail = EmailData.builder()
                .messageId("FALLBACK-RECOVERY-MSG")
                .senderEmail("buyer@corp.com")
                .subject("Requirement for Industrial Bolts")
                .body("""
                      Industrial Bolts - 40 Nos
                      Brand: Tata Steel
                      Required Delivery Date: 2026-12-25
                      Delivery Location: Hyderabad Plant
                      """)
                .build();

        ExtractedRFQ rfqFallbackRecovery = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("null")
                .deliveryDate("null")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Industrial Bolts")
                                .quantity(0.0)
                                .uom(null)
                                .brand("Not Specified")
                                .deliveryDate("Not Specified")
                                .deliveryLocation("Not Specified")
                                .build()
                )))
                .build();

        Mockito.when(aiExtractionService.extractRFQFromEmail(fallbackRecoveryEmail)).thenReturn(rfqFallbackRecovery);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-RECOVERY-43").build());

        String recoveryResult = emailProcessorService.processSingleEmail(fallbackRecoveryEmail);
        assertEquals("RFQ_CREATED", recoveryResult);
        assertEquals("Tata Steel", rfqFallbackRecovery.getItems().get(0).getBrand());
        assertEquals("2026-12-25", rfqFallbackRecovery.getItems().get(0).getDeliveryDate());
        assertEquals("Hyderabad Plant", rfqFallbackRecovery.getItems().get(0).getDeliveryLocation());
        assertEquals(40.0, rfqFallbackRecovery.getItems().get(0).getQuantity());
        assertEquals("Nos", rfqFallbackRecovery.getItems().get(0).getUom());

        // 44. rfqItemRecordRepository.save exception and null deliveryDate on rfqRequest
        EmailData itemSaveExEmail = EmailData.builder()
                .messageId("ITEM-SAVE-EX-MSG")
                .senderEmail("buyer@corp.com")
                .subject("Test Item Save Ex")
                .build();
        ExtractedRFQ rfqItemSaveEx = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Delhi")
                .deliveryDate("2026-12-31")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Item With Save Ex").quantity(1.0).build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(itemSaveExEmail)).thenReturn(rfqItemSaveEx);
        Mockito.when(rfqBuilderService.buildRFQRequest(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(RFQRequest.builder().rfqNumber("RFQ-NULL-DATE").deliveryDate(null).build());
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-NULL-DATE").build());
        Mockito.doThrow(new RuntimeException("Record save failure")).when(rfqItemRecordRepository).save(Mockito.any());

        String itemSaveExResult = emailProcessorService.processSingleEmail(itemSaveExEmail);
        assertEquals("RFQ_CREATED", itemSaveExResult);

        // 45. processUnreadEmails with null unread list
        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(null);
        ProcessingStats nullStats = emailProcessorService.processUnreadEmails();
        assertNotNull(nullStats);
        assertEquals(0, nullStats.getEmailsProcessed());
        assertEquals("SUCCESS", nullStats.getStatus());

        // 46. processUnreadEmails with mixed outcomes: success, error status, skipped system status, and unhandled exception
        EmailData em1 = EmailData.builder().senderEmail("rfq@procucev.com").messageId("LOOP-1").build();
        EmailData em2 = EmailData.builder().senderEmail("buyer@corp.com").messageId("OK-1").subject("Req 1").build();
        ExtractedRFQ rfqOk = ExtractedRFQ.builder().buyerEmail("buyer@corp.com").items(List.of(RFQItem.builder().itemDescription("Item 1").quantity(1.0).build())).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(em2)).thenReturn(rfqOk);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-MIXED-1").build());

        EmailData em3 = EmailData.builder().senderEmail("unregistered@corp.com").messageId("UNREG-1").build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("unregistered@corp.com"))
                .thenReturn(Buyer.builder().email("unregistered@corp.com").verified(false).build());

        EmailData em4 = EmailData.builder().senderEmail("crash@corp.com").messageId("CRASH-1").build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("crash@corp.com"))
                .thenThrow(new RuntimeException("Severe crash"));

        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(List.of(em1, em2, em3, em4));
        ProcessingStats mixedStats = emailProcessorService.processUnreadEmails();
        assertNotNull(mixedStats);
        assertEquals(4, mixedStats.getEmailsProcessed());
        assertEquals(1, mixedStats.getRfqsCreated());
        assertEquals(2, mixedStats.getErrors());
        assertEquals("COMPLETED_WITH_ERRORS", mixedStats.getStatus());

        // 47. hasExplicitLocationInPayload comprehensive branches
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", null, null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", ExtractedRFQ.builder().deliveryLocation("Mumbai").build(), null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", ExtractedRFQ.builder().deliveryCity("Hyderabad").build(), null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", ExtractedRFQ.builder().deliveryState("Telangana").build(), null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", ExtractedRFQ.builder().deliveryPincode("500081").build(), null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", ExtractedRFQ.builder().items(List.of(RFQItem.builder().deliveryLocation("Chennai").build())).build(), null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", null, EmailData.builder().subject("Plant: Pune").build()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", null, EmailData.builder().body("Ship to: Kolkata").build()));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", null, EmailData.builder().attachmentText("Destination: Bangalore").build()));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", null, EmailData.builder().subject("General inquiry").body("Hello team").build()));

        // 48. buildFailedRequestFromEmail comprehensive branches
        FailedRfqRequest fNull = ReflectionTestUtils.invokeMethod(emailProcessorService, "buildFailedRequestFromEmail",
                EmailData.builder().senderEmail("sender@corp.com").subject("Raw Sub").messageId("M-FAIL-NULL").build(),
                null, "REASON_NULL");
        assertNotNull(fNull);
        assertEquals("sender@corp.com", fNull.getBuyerEmail());
        assertEquals("Not Provided", fNull.getDescription());

        FailedRfqRequest fFull = ReflectionTestUtils.invokeMethod(emailProcessorService, "buildFailedRequestFromEmail",
                EmailData.builder().senderEmail("sender@corp.com").subject("Raw Sub").messageId("M-FAIL-FULL").build(),
                ExtractedRFQ.builder()
                        .buyerEmail("buyer.extracted@corp.com")
                        .deliveryLocation("Plant Alpha")
                        .deliveryDate("2026-11-15")
                        .items(List.of(RFQItem.builder()
                                .itemDescription("Custom Valve")
                                .partNumber("CV-999")
                                .specification("Grade 316")
                                .brand("Tata")
                                .quantity(15.0)
                                .uom("Set")
                                .build()))
                        .build(),
                "REASON_FULL");
        assertNotNull(fFull);
        assertEquals("buyer.extracted@corp.com", fFull.getBuyerEmail());
        assertEquals("Custom Valve", fFull.getDescription());
        assertEquals("CV-999", fFull.getPartNumber());
        assertEquals("Grade 316", fFull.getSpecification());
        assertEquals("Tata", fFull.getBrand());
        assertEquals("15", fFull.getQuantity());
        assertEquals("Set", fFull.getUom());
        assertEquals("Plant Alpha", fFull.getDeliveryLocation());
        assertEquals("2026-11-15", fFull.getDeliveryDate());

        // 49. mergeThreadContext comprehensive branches
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext",
                EmailData.builder().build(), null));
        ExtractedRFQ extNoHeaders = ExtractedRFQ.builder().deliveryLocation("Loc").build();
        assertSame(extNoHeaders, ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext",
                EmailData.builder().build(), extNoHeaders));

        // Prior transaction with extraction JSON
        EmailTransaction histTxThread = new EmailTransaction();
        histTxThread.setMessageId("PARENT-MSG-1");
        histTxThread.setExtractionJson("{\"items\":[{\"itemDescription\":\"Parent Item 1\",\"specification\":\"Parent Spec 1\",\"brand\":\"Parent Brand 1\",\"category\":\"Valves\"}],\"deliveryLocation\":\"Parent Loc\",\"deliveryDate\":\"2026-10-10\"}");
        Mockito.when(emailTransactionRepository.findByMessageId("PARENT-MSG-1")).thenReturn(Optional.of(histTxThread));

        EmailData replyEmailNoProd = EmailData.builder()
                .inReplyTo("PARENT-MSG-1")
                .body("Qty is 50")
                .build();
        ExtractedRFQ extToMerge = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().quantity(50.0).build(),
                        RFQItem.builder().quantity(20.0).build()
                )))
                .build();
        ExtractedRFQ merged = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", replyEmailNoProd, extToMerge);
        assertNotNull(merged);
        assertEquals("Parent Item 1", merged.getItems().get(0).getItemDescription());
        assertEquals("Parent Spec 1", merged.getItems().get(0).getSpecification());
        assertEquals("Parent Brand 1", merged.getItems().get(0).getBrand());
        assertEquals("Valves", merged.getItems().get(0).getCategory());
        assertEquals("Parent Loc", merged.getDeliveryLocation());
        assertEquals("2026-10-10", merged.getDeliveryDate());

        // 50. salvageSpecificationFromBrand comprehensive branches
        RFQItem itemNullBrand = RFQItem.builder().brand(null).build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemNullBrand, "Item Desc");
        assertNull(itemNullBrand.getBrand());

        RFQItem itemTataBrand = RFQItem.builder().brand("Tata Steel").build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemTataBrand, "Item Desc");
        assertEquals("Tata Steel", itemTataBrand.getBrand());

        RFQItem itemLongSpecBrand = RFQItem.builder().brand("High Quality Stainless Steel Grade 316 Seamless Pipe").specification(null).build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemLongSpecBrand, "Item Desc");
        assertNull(itemLongSpecBrand.getBrand());
        assertEquals("High Quality Stainless Steel Grade 316 Seamless Pipe", itemLongSpecBrand.getSpecification());

        RFQItem itemLongSpecBrandWithExistingSpec = RFQItem.builder().brand("High Quality Stainless Steel Grade 316 Seamless Pipe").specification("Existing Spec").build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemLongSpecBrandWithExistingSpec, "Item Desc");
        assertNull(itemLongSpecBrandWithExistingSpec.getBrand());
        assertEquals("Existing Spec", itemLongSpecBrandWithExistingSpec.getSpecification());

        // 51. parseCityStatePincodeFromLocation state branches
        Buyer dummyBuyer = Buyer.builder().city("BuyerCity").state("BuyerState").pincode("110001").build();
        String[] rKar = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Karnataka Plant 560001", null, null, null, null);
        assertEquals("Karnataka", rKar[1]);
        assertEquals("560001", rKar[2]);

        String[] rTel = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Telangana Warehouse 500001", null, null, null, null);
        assertEquals("Telangana", rTel[1]);

        String[] rAnd = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Andhra Site 530001", null, null, null, null);
        assertEquals("Andhra Pradesh", rAnd[1]);

        String[] rMah = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Maharashtra Unit 400001", null, null, null, null);
        assertEquals("Maharashtra", rMah[1]);

        String[] rTam = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Tamil Nadu Facility 600001", null, null, null, null);
        assertEquals("Tamil Nadu", rTam[1]);

        String[] rWest = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "West Bengal Yard 700001", null, null, null, null);
        assertEquals("West Bengal", rWest[1]);

        String[] rGuj = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Gujarat Hub 380001", null, null, null, null);
        assertEquals("Gujarat", rGuj[1]);

        String[] rRaj = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Rajasthan Mine 302001", null, null, null, null);
        assertEquals("Rajasthan", rRaj[1]);

        String[] rBuyer = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Unknown Location", null, null, null, dummyBuyer);
        assertEquals("BuyerCity", rBuyer[0]);
        assertEquals("BuyerState", rBuyer[1]);
        assertEquals("110001", rBuyer[2]);

        // 52. processSingleEmail comprehensive edge cases
        // A. Full fallback recovery of all fields from body patterns
        EmailData fullRecEmail = EmailData.builder()
                .messageId("FULL-REC-52")
                .senderEmail("buyer@corp.com")
                .subject("Requirement: Custom Pump")
                .body("Product: Custom Pump\nTechnical Specifications: Heavy Duty Cast Iron Grade 304\nBrand: Kirloskar Pumps\nDelivery Location: Pune Plant 411001\nDelivery Date: 2026-11-20\nQuantity: 25 Nos")
                .build();
        ExtractedRFQ extFullRec = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("RFQ Procurement Item").build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(fullRecEmail)).thenReturn(extFullRec);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-FULL-REC-52").build());
        String recRes = emailProcessorService.processSingleEmail(fullRecEmail);
        assertEquals("RFQ_CREATED", recRes);

        // B. Multi-item with null items and blank item descriptions
        EmailData nullItemEmail = EmailData.builder()
                .messageId("NULL-ITEM-52")
                .senderEmail("buyer@corp.com")
                .subject("Multi Item Test")
                .build();
        ExtractedRFQ extNullItem = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(new java.util.ArrayList<>(java.util.Arrays.asList(
                        null,
                        RFQItem.builder().itemDescription("   ").build(),
                        RFQItem.builder().itemDescription("Valid Pump").quantity(5.0).build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(nullItemEmail)).thenReturn(extNullItem);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-NULL-ITEM-52").build());
        String nullItemRes = emailProcessorService.processSingleEmail(nullItemEmail);
        assertEquals("RFQ_CREATED", nullItemRes);

        // C. RFQ submission returning oversized document error
        EmailData oversizedSubmitEmail = EmailData.builder()
                .messageId("OVERSIZED-52")
                .senderEmail("buyer@corp.com")
                .subject("Oversized Req")
                .build();
        ExtractedRFQ extOversizedSubmit = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Big File Item").quantity(1.0).build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(oversizedSubmitEmail)).thenReturn(extOversizedSubmit);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("FILE_SIZE_EXCEEDED").message("Document exceeds maximum allowed size").build());
        String oversizedRes = emailProcessorService.processSingleEmail(oversizedSubmitEmail);
        assertEquals("FILE_SIZE_EXCEEDED", oversizedRes);

        // D. RFQ submission returning generic failure
        EmailData genericFailEmail = EmailData.builder()
                .messageId("GEN-FAIL-52")
                .senderEmail("buyer@corp.com")
                .subject("Generic Fail Req")
                .build();
        ExtractedRFQ extGenFail = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Gen Fail Item").quantity(1.0).build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(genericFailEmail)).thenReturn(extGenFail);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("FAILED").message("Database timeout").build());
        String genFailRes = emailProcessorService.processSingleEmail(genericFailEmail);
        assertEquals("FAILED", genFailRes);

        // 53. Exhaustive branch combinations for remaining methods
        // A. hasExplicitLocationInPayload negative strings and item loop edge cases
        ExtractedRFQ extNegStrings = ExtractedRFQ.builder()
                .deliveryLocation("Not Specified")
                .deliveryCity("Not Specified")
                .deliveryState("Not Specified")
                .deliveryPincode("Not Specified")
                .items(new java.util.ArrayList<>(java.util.Arrays.asList(
                        null,
                        RFQItem.builder().deliveryLocation(null).build(),
                        RFQItem.builder().deliveryLocation("").build(),
                        RFQItem.builder().deliveryLocation("Not Specified").build(),
                        RFQItem.builder().deliveryLocation("null").build(),
                        RFQItem.builder().deliveryLocation("Registered Profile Address").build()
                )))
                .build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", extNegStrings, null));

        ExtractedRFQ extNullLoc = ExtractedRFQ.builder().deliveryLocation("null").build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", extNullLoc, null));

        ExtractedRFQ extRegProf = ExtractedRFQ.builder().deliveryLocation("Registered Profile Address").build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", extRegProf, null));

        // B. parseCityStatePincodeFromLocation remaining cities and variations
        String[] rBen = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "bengaluru tech park", null, null, null, null);
        assertEquals("Bangalore", rBen[0]);
        assertEquals("Karnataka", rBen[1]);

        String[] rHyd = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "hyderabad hitec city", null, null, null, null);
        assertEquals("Hyderabad", rHyd[0]);
        assertEquals("Telangana", rHyd[1]);

        String[] rChe = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "chennai port", null, null, null, null);
        assertEquals("Chennai", rChe[0]);
        assertEquals("Tamil Nadu", rChe[1]);

        String[] rMum = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "mumbai central", null, null, null, null);
        assertEquals("Mumbai", rMum[0]);
        assertEquals("Maharashtra", rMum[1]);

        String[] rDel = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "delhi ncr", null, null, null, null);
        assertEquals("Delhi", rDel[0]);
        assertEquals("Delhi", rDel[1]);

        String[] rKol = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "kolkata hub", null, null, null, null);
        assertEquals("Kolkata", rKol[0]);
        assertEquals("West Bengal", rKol[1]);

        String[] rPun = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "pune plant", null, null, null, null);
        assertEquals("Pune", rPun[0]);
        assertEquals("Maharashtra", rPun[1]);

        String[] rAhm = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "ahmedabad zone", null, null, null, null);
        assertEquals("Ahmedabad", rAhm[0]);
        assertEquals("Gujarat", rAhm[1]);

        String[] rKak = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "kakinada refinery", null, null, null, null);
        assertEquals("Kakinada", rKak[0]);
        assertEquals("Andhra Pradesh", rKak[1]);

        String[] rTamNoSpace = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "somewhere in tamilnadu", null, null, null, null);
        assertEquals("Tamil Nadu", rTamNoSpace[1]);

        // C. buildFailedRequestFromEmail with blank fields inside extractedRFQ
        ExtractedRFQ extBlanks = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryDate("")
                .buyerEmail(null)
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("")
                                .partNumber("")
                                .specification("")
                                .brand("")
                                .quantity(0.0)
                                .uom("")
                                .build()
                )))
                .build();
        FailedRfqRequest fBlanks = ReflectionTestUtils.invokeMethod(emailProcessorService, "buildFailedRequestFromEmail",
                EmailData.builder().senderEmail("sender@corp.com").subject("Raw Sub").messageId("M-FAIL-BLANKS").build(),
                extBlanks, "REASON_BLANKS");
        assertNotNull(fBlanks);
        assertEquals("sender@corp.com", fBlanks.getBuyerEmail());
        assertEquals("Not Provided", fBlanks.getDescription());
        assertEquals("Not Provided", fBlanks.getQuantity());

        // D. mergeThreadContext multi-reference loop and namesItsOwnProduct branch
        EmailTransaction ref3Tx = new EmailTransaction();
        ref3Tx.setMessageId("REF-3");
        ref3Tx.setExtractionJson("{\"items\":[{\"itemDescription\":\"Ref3 Desc\",\"specification\":\"Ref3 Spec\",\"brand\":\"Ref3 Brand\",\"category\":\"Ref3 Cat\"}],\"deliveryLocation\":\"Ref3 Loc\",\"deliveryDate\":\"2026-12-12\"}");
        Mockito.when(emailTransactionRepository.findByMessageId("REF-1")).thenReturn(Optional.empty());
        EmailTransaction ref2Tx = new EmailTransaction();
        ref2Tx.setMessageId("REF-2");
        ref2Tx.setExtractionJson(null);
        Mockito.when(emailTransactionRepository.findByMessageId("REF-2")).thenReturn(Optional.of(ref2Tx));
        Mockito.when(emailTransactionRepository.findByMessageId("REF-3")).thenReturn(Optional.of(ref3Tx));

        EmailData emailMultiRef = EmailData.builder()
                .references("REF-1 REF-2 REF-3")
                .body("Product: Stated New Product Name\nQuantity: 10")
                .build();
        ExtractedRFQ extBlankItems = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("").specification("").brand("").category("").quantity(10.0).build()
                )))
                .build();
        ExtractedRFQ mergedMultiRef = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailMultiRef, extBlankItems);
        assertNotNull(mergedMultiRef);
        assertEquals("Stated New Product Name", mergedMultiRef.getItems().get(0).getItemDescription());
        assertEquals("", mergedMultiRef.getItems().get(0).getSpecification());

        // 54. Acknowledgement sendProcessingFailureAcknowledgement throwing exception in catch block
        EmailData ackFailEmail = EmailData.builder()
                .messageId("ACK-FAIL-MSG")
                .senderEmail("ackfail@corp.com")
                .subject("Ack Failure")
                .build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("ackfail@corp.com"))
                .thenThrow(new RuntimeException("Crash for ack fail"));
        Mockito.doThrow(new RuntimeException("Ack send crashed"))
                .when(acknowledgementEmailService).sendProcessingFailureAcknowledgement(Mockito.any(), Mockito.any(), Mockito.any());

        String ackFailRes = emailProcessorService.processSingleEmail(ackFailEmail);
        assertEquals("FAILED", ackFailRes);

        // 55. Edge values "null" and "Not Specified" for topDeliveryDate, item uom, location, date
        EmailData nullStrEmail = EmailData.builder()
                .messageId("NULL-STR-MSG")
                .senderEmail("buyer@corp.com")
                .subject("Null String Edge")
                .build();
        ExtractedRFQ extNullStr = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryDate("Not Specified")
                .deliveryLocation("Not Specified")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Null Str Item")
                                .uom("null")
                                .deliveryLocation("Not Specified")
                                .deliveryDate("Not Specified")
                                .quantity(1.0)
                                .build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(nullStrEmail)).thenReturn(extNullStr);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-NULL-STR").build());
        String nullStrRes = emailProcessorService.processSingleEmail(nullStrEmail);
        assertEquals("RFQ_CREATED", nullStrRes);

        // 56. All items invalid in email (triggers validItems.isEmpty())
        EmailData allInvalidEmail = EmailData.builder()
                .messageId("ALL-INVALID-MSG")
                .senderEmail("buyer@corp.com")
                .subject("   ")
                .body("Just chat")
                .build();
        ExtractedRFQ extAllInvalid = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("").build(),
                        RFQItem.builder().itemDescription("   ").build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(allInvalidEmail)).thenReturn(extAllInvalid);
        String allInvalidRes = emailProcessorService.processSingleEmail(allInvalidEmail);
        assertEquals("VALIDATION_FAILED", allInvalidRes);

        // 57. Target remaining complexity points across helper methods
        // A. scanQuantityFromEmail edge branches
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", (EmailData) null));
        EmailData emEmptyQty = EmailData.builder().body("Requirement: ").attachmentText("").subject("Req 0 Nos").build();
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emEmptyQty));
        EmailData emAttQty = EmailData.builder().body("").attachmentText("Required Quantity: 50 Pieces").subject("").build();
        assertEquals(50.0, (Double) ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emAttQty));

        // B. scanFieldFromEmail edge branches
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "scanFieldFromEmail", (EmailData) null, "pattern"));
        EmailData emAttField = EmailData.builder().body("   ").attachmentText("Delivery Location: Pune Yard").build();
        assertEquals("Pune Yard", ReflectionTestUtils.invokeMethod(emailProcessorService, "scanFieldFromEmail", emAttField, "(?i)Delivery Location:\\s*([^\\r\\n]+)"));

        // C. looksLikeBrandName token count overflow
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Alpha Beta Gamma Delta Epsilon Zeta Eta Theta Iota Kappa Lambda"));

        // D. salvageSpecificationFromBrand with "null" and "Not Specified"
        RFQItem itemNullBrandEdge = RFQItem.builder().brand("null").specification("null").build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemNullBrandEdge, "Desc");
        RFQItem itemNotSpecBrandEdge = RFQItem.builder().brand("Not Specified").specification("Not Specified").build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemNotSpecBrandEdge, "Desc");
        RFQItem itemSalvageNotSpec = RFQItem.builder().brand("Requirement for heavy duty valve with flanges").specification("Not Specified").build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemSalvageNotSpec, "Desc");
        assertEquals("Requirement for heavy duty valve with flanges", itemSalvageNotSpec.getSpecification());
        assertNull(itemSalvageNotSpec.getBrand());
        RFQItem itemSalvageNullSpec = RFQItem.builder().brand("Requirement for heavy duty valve with flanges").specification("null").build();
        ReflectionTestUtils.invokeMethod(emailProcessorService, "salvageSpecificationFromBrand", itemSalvageNullSpec, "Desc");
        assertEquals("Requirement for heavy duty valve with flanges", itemSalvageNullSpec.getSpecification());
        assertNull(itemSalvageNullSpec.getBrand());

        // E. mergeThreadContext edge branches
        EmailTransaction badJsonTx = new EmailTransaction();
        badJsonTx.setMessageId("BAD-JSON-1");
        badJsonTx.setExtractionJson("{invalid json");
        Mockito.when(emailTransactionRepository.findByMessageId("BAD-JSON-1")).thenReturn(Optional.of(badJsonTx));
        ExtractedRFQ extTryBadJson = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext",
                EmailData.builder().inReplyTo("BAD-JSON-1").build(),
                ExtractedRFQ.builder().build());
        assertNotNull(extTryBadJson);

        EmailTransaction nullItemsHistTx = new EmailTransaction();
        nullItemsHistTx.setMessageId("NULL-HIST-1");
        nullItemsHistTx.setExtractionJson("{\"deliveryLocation\":\"HistLoc\",\"deliveryDate\":\"2026-10-10\"}");
        Mockito.when(emailTransactionRepository.findByMessageId("NULL-HIST-1")).thenReturn(Optional.of(nullItemsHistTx));
        ExtractedRFQ extNullHist = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext",
                EmailData.builder().inReplyTo("NULL-HIST-1").build(),
                ExtractedRFQ.builder().items(new java.util.ArrayList<>(List.of(RFQItem.builder().itemDescription("Item").build()))).build());
        assertNotNull(extNullHist);
        assertEquals("HistLoc", extNullHist.getDeliveryLocation());
        assertEquals("2026-10-10", extNullHist.getDeliveryDate());

        EmailTransaction histSingleTx = new EmailTransaction();
        histSingleTx.setMessageId("HIST-SINGLE-1");
        histSingleTx.setExtractionJson("{\"items\":[{\"itemDescription\":\"Hist Item 1\",\"specification\":\"Hist Spec 1\",\"brand\":\"Hist Brand 1\",\"category\":\"Hist Cat 1\"}]}");
        Mockito.when(emailTransactionRepository.findByMessageId("HIST-SINGLE-1")).thenReturn(Optional.of(histSingleTx));
        ExtractedRFQ extTwoItems = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("").specification("").brand("").category("").build(),
                        RFQItem.builder().itemDescription("").specification("").brand("").category("").build()
                )))
                .build();
        ExtractedRFQ mergedTwoItems = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext",
                EmailData.builder().inReplyTo("HIST-SINGLE-1").body("No product stated").build(),
                extTwoItems);
        assertNotNull(mergedTwoItems);
        assertEquals("Hist Item 1", mergedTwoItems.getItems().get(0).getItemDescription());
        assertEquals("Hist Spec 1", mergedTwoItems.getItems().get(0).getSpecification());
        assertEquals("Hist Brand 1", mergedTwoItems.getItems().get(0).getBrand());
        assertEquals("Hist Cat 1", mergedTwoItems.getItems().get(0).getCategory());
        assertEquals("Hist Item 1", mergedTwoItems.getItems().get(1).getItemDescription());
        assertEquals("Hist Spec 1", mergedTwoItems.getItems().get(1).getSpecification());

        // F. parseCityStatePincodeFromLocation buyer null/blank/present branches
        Buyer buyerEmptyFields = Buyer.builder().city("").state("").pincode("").build();
        String[] rEmptyBuyer = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "", null, null, null, buyerEmptyFields);
        assertEquals("", rEmptyBuyer[0]);
        assertEquals("", rEmptyBuyer[1]);
        assertEquals("", rEmptyBuyer[2]);

        Buyer buyerNullFields = Buyer.builder().city(null).state(null).pincode(null).build();
        String[] rNullBuyer = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "", null, null, null, buyerNullFields);
        assertEquals("", rNullBuyer[0]);

        Buyer buyerFull = Buyer.builder().city("OtherCity").state("OtherState").pincode("999999").build();
        String[] rAlreadyPresent = ReflectionTestUtils.invokeMethod(emailProcessorService, "parseCityStatePincodeFromLocation", "Bangalore 560001", null, null, null, buyerFull);
        assertEquals("Bangalore", rAlreadyPresent[0]);
        assertEquals("Karnataka", rAlreadyPresent[1]);
        assertEquals("560001", rAlreadyPresent[2]);

        // 58. Final complexity branches
        // A. buildFailedRequestFromEmail empty items, partCode fallback, and buyer email
        ExtractedRFQ extEmptyItems = ExtractedRFQ.builder()
                .buyerEmail("buyerExt@corp.com")
                .items(Collections.emptyList())
                .build();
        FailedRfqRequest fEmptyItems = ReflectionTestUtils.invokeMethod(emailProcessorService, "buildFailedRequestFromEmail",
                EmailData.builder().senderEmail("sender@corp.com").subject("Raw Sub").messageId("M-EMPTY-ITEMS").build(),
                extEmptyItems, "REASON_EMPTY_ITEMS");
        assertEquals("buyerExt@corp.com", fEmptyItems.getBuyerEmail());
        assertEquals("Not Provided", fEmptyItems.getDescription());

        ExtractedRFQ extPartCode = ExtractedRFQ.builder()
                .buyerEmail(null)
                .items(List.of(RFQItem.builder().partCode("PART-CODE-XYZ").quantity(25.0).build()))
                .build();
        FailedRfqRequest fPartCode = ReflectionTestUtils.invokeMethod(emailProcessorService, "buildFailedRequestFromEmail",
                EmailData.builder().senderEmail("sender@corp.com").subject("Raw Sub").messageId("M-PART-CODE").build(),
                extPartCode, "REASON_PART_CODE");
        assertEquals("sender@corp.com", fPartCode.getBuyerEmail());
        assertEquals("PART-CODE-XYZ", fPartCode.getPartNumber());
        assertEquals("25", fPartCode.getQuantity());

        // B. hasExplicitLocationInPayload blank subfields and null item list
        ExtractedRFQ extBlankSubfields = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryCity("")
                .deliveryState("")
                .deliveryPincode("")
                .items(Collections.emptyList())
                .build();
        EmailData emailNullFields = EmailData.builder().subject(null).body(null).attachmentText(null).build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", extBlankSubfields, emailNullFields));

        ExtractedRFQ extNullItemList = ExtractedRFQ.builder()
                .deliveryLocation("Not Specified")
                .deliveryCity("Not Specified")
                .deliveryState("Not Specified")
                .deliveryPincode("Not Specified")
                .items(null)
                .build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "hasExplicitLocationInPayload", extNullItemList, null));

        // C. processSingleEmail fully pre-populated single item
        EmailData populatedItemEmail = EmailData.builder()
                .messageId("POPULATED-ITEM-1")
                .senderEmail("buyer@corp.com")
                .subject("Complete Item Details")
                .body("Here is the requirement")
                .build();
        ExtractedRFQ extPopulated = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Pune Warehouse 411001")
                .deliveryDate("2026-12-01")
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder()
                                .itemDescription("Gate Valve 2 inch")
                                .specification("Cast Steel Class 150")
                                .brand("L&T")
                                .uom("Units")
                                .quantity(15.0)
                                .deliveryLocation("Pune Warehouse 411001")
                                .deliveryDate("2026-12-01")
                                .build()
                )))
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(populatedItemEmail)).thenReturn(extPopulated);
        Mockito.when(rfqApiService.submitRFQ(Mockito.any(RFQRequest.class)))
                .thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-POPULATED-1").build());
        String popRes = emailProcessorService.processSingleEmail(populatedItemEmail);
        assertEquals("RFQ_CREATED", popRes);
    }

    @Test
    @DisplayName("Test scanFieldFromEmail, mergeThreadContext, looksLikeBrandName and scanQuantity branches")
    void testBranchCoverageMatrix() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 1. scanFieldFromEmail branches
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "scanFieldFromEmail", (EmailData) null, ".*"));

        EmailData emailAttOnly = EmailData.builder()
                .body(null)
                .attachmentText("Delivery Location: Bangalore Plant")
                .build();
        String scannedLoc = ReflectionTestUtils.invokeMethod(emailProcessorService, "scanFieldFromEmail", emailAttOnly, "(?i)Delivery Location:\\s*(.*)");
        assertEquals("Bangalore Plant", scannedLoc);

        // 2. mergeThreadContext branches
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailAttOnly, (ExtractedRFQ) null));

        EmailData emailNoThread = EmailData.builder().inReplyTo(null).references(null).build();
        ExtractedRFQ extRfq = ExtractedRFQ.builder().deliveryLocation("Mumbai").build();
        assertSame(extRfq, ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailNoThread, extRfq));

        // In-Reply-To where prior transaction doesn't exist
        EmailData emailPriorNotFound = EmailData.builder().inReplyTo("MSG-NON-EXISTENT").build();
        Mockito.when(emailTransactionRepository.findByMessageId("MSG-NON-EXISTENT")).thenReturn(Optional.empty());
        assertSame(extRfq, ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailPriorNotFound, extRfq));

        // Prior transaction with null extraction json
        EmailData emailPriorNullJson = EmailData.builder().inReplyTo("MSG-NULL-JSON").build();
        EmailTransaction txNullJson = new EmailTransaction();
        txNullJson.setMessageId("MSG-NULL-JSON");
        txNullJson.setExtractionJson(null);
        Mockito.when(emailTransactionRepository.findByMessageId("MSG-NULL-JSON")).thenReturn(Optional.of(txNullJson));
        assertSame(extRfq, ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailPriorNullJson, extRfq));

        // Prior transaction with invalid JSON
        EmailData emailBadJson = EmailData.builder().inReplyTo("MSG-BAD-JSON").build();
        EmailTransaction txBadJson = new EmailTransaction();
        txBadJson.setMessageId("MSG-BAD-JSON");
        txBadJson.setExtractionJson("{invalid json");
        Mockito.when(emailTransactionRepository.findByMessageId("MSG-BAD-JSON")).thenReturn(Optional.of(txBadJson));
        assertSame(extRfq, ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailBadJson, extRfq));

        // Prior transaction with historical items and namesItsOwnProduct == false
        EmailData emailThreadNoOwnProduct = EmailData.builder()
                .inReplyTo("MSG-VALID-THREAD")
                .references("MSG-REF-1")
                .body("Quantity is 50 Nos")
                .build();
        ExtractedRFQ historicalRfq = ExtractedRFQ.builder()
                .deliveryLocation("Chennai Hub")
                .deliveryDate("2026-11-15")
                .items(List.of(
                        RFQItem.builder()
                                .itemDescription("Centrifugal Pump")
                                .specification("Cast Iron 5HP")
                                .brand("Kirloskar")
                                .category("Pumps & Motors")
                                .build()
                ))
                .build();
        EmailTransaction txValid = new EmailTransaction();
        txValid.setMessageId("MSG-VALID-THREAD");
        txValid.setExtractionJson(mapper.writeValueAsString(historicalRfq));
        Mockito.when(emailTransactionRepository.findByMessageId("MSG-VALID-THREAD")).thenReturn(Optional.of(txValid));

        ExtractedRFQ currentRfqMissingDetails = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("").quantity(50.0).build(),
                        RFQItem.builder().itemDescription(null).quantity(20.0).build()
                )))
                .build();
        ExtractedRFQ mergedRfq = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailThreadNoOwnProduct, currentRfqMissingDetails);
        assertNotNull(mergedRfq);
        assertEquals("Centrifugal Pump", mergedRfq.getItems().get(0).getItemDescription());
        assertEquals("Cast Iron 5HP", mergedRfq.getItems().get(0).getSpecification());
        assertEquals("Kirloskar", mergedRfq.getItems().get(0).getBrand());
        assertEquals("Pumps & Motors", mergedRfq.getItems().get(0).getCategory());
        assertEquals("Chennai Hub", mergedRfq.getDeliveryLocation());
        assertEquals("2026-11-15", mergedRfq.getDeliveryDate());

        // Thread with namesItsOwnProduct == true
        EmailData emailThreadOwnProduct = EmailData.builder()
                .inReplyTo("MSG-VALID-THREAD-2")
                .body("Product: Submersible Pump\nQuantity is 10 Nos")
                .build();
        EmailTransaction txValid2 = new EmailTransaction();
        txValid2.setMessageId("MSG-VALID-THREAD-2");
        txValid2.setExtractionJson(mapper.writeValueAsString(historicalRfq));
        Mockito.when(emailTransactionRepository.findByMessageId("MSG-VALID-THREAD-2")).thenReturn(Optional.of(txValid2));

        ExtractedRFQ currentRfqBlankDesc = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("").quantity(10.0).build()
                )))
                .build();
        ExtractedRFQ mergedOwnProduct = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailThreadOwnProduct, currentRfqBlankDesc);
        assertEquals("Submersible Pump", mergedOwnProduct.getItems().get(0).getItemDescription());
        assertNull(mergedOwnProduct.getItems().get(0).getSpecification());

        // 3. looksLikeBrandName branches
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", (String) null));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "   "));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "A".repeat(70)));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Laptop with 16 GB RAM"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Please provide Dell"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "One Two Three Four Five Six Seven Eight"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "looksLikeBrandName", "Dell / HP"));

        // 4. isPlausibleSpecification branches
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", (String) null));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "abc"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "Delivery Location: Mumbai"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isPlausibleSpecification", "Stainless Steel Grade 304"));

        // 5. scanQuantityFromEmail branches
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", (EmailData) null));
        EmailData emailNullSources = EmailData.builder().body(null).attachmentText(null).subject(null).build();
        assertNull(ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emailNullSources));
        EmailData emailValidQty = EmailData.builder().body("Required Quantity: 100 Nos").build();
        assertEquals(100.0, (Double) ReflectionTestUtils.invokeMethod(emailProcessorService, "scanQuantityFromEmail", emailValidQty));

        // 6. Additional mergeThreadContext branches
        EmailData emailThread3 = EmailData.builder()
                .references("MSG-HIST-3")
                .attachmentText("Product: Hex Bolts")
                .build();
        ExtractedRFQ histEmptyItems = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>())
                .deliveryLocation("Pune")
                .deliveryDate("2026-12-01")
                .build();
        EmailTransaction txHist3 = new EmailTransaction();
        txHist3.setMessageId("MSG-HIST-3");
        txHist3.setExtractionJson(mapper.writeValueAsString(histEmptyItems));
        Mockito.when(emailTransactionRepository.findByMessageId("MSG-HIST-3")).thenReturn(Optional.of(txHist3));

        ExtractedRFQ currentPopulated = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Populated").specification("Spec1").brand("Brand1").category("Cat1").build()
                )))
                .deliveryLocation("Mumbai")
                .deliveryDate("2026-10-01")
                .build();
        ExtractedRFQ merged3 = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailThread3, currentPopulated);
        assertNotNull(merged3);
        assertEquals("Mumbai", merged3.getDeliveryLocation());
        assertEquals("2026-10-01", merged3.getDeliveryDate());
        assertEquals("Populated", merged3.getItems().get(0).getItemDescription());
        assertEquals("Spec1", merged3.getItems().get(0).getSpecification());

        // Test merge with null items in extracted
        ExtractedRFQ currentNullItems = ExtractedRFQ.builder().items(null).build();
        ExtractedRFQ mergedNullItems = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailThreadNoOwnProduct, currentNullItems);
        assertNotNull(mergedNullItems);

        // Test merge when current has non-blank spec, brand, category with no own product
        ExtractedRFQ currentWithSpecs = ExtractedRFQ.builder()
                .items(new java.util.ArrayList<>(List.of(
                        RFQItem.builder().itemDescription("Item1").specification("SpecX").brand("BrandY").category("CatZ").build()
                )))
                .build();
        ExtractedRFQ mergedWithSpecs = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailThreadNoOwnProduct, currentWithSpecs);
        assertNotNull(mergedWithSpecs);
        assertEquals("SpecX", mergedWithSpecs.getItems().get(0).getSpecification());
        assertEquals("BrandY", mergedWithSpecs.getItems().get(0).getBrand());
        assertEquals("CatZ", mergedWithSpecs.getItems().get(0).getCategory());

        // Test scanFieldFromEmail from body and attachment
        EmailData emailWithBodyProduct = EmailData.builder().body("Requirement for Product: Copper Wire").build();
        String scannedBody = ReflectionTestUtils.invokeMethod(emailProcessorService, "scanFieldFromEmail", emailWithBodyProduct, "Product:\\s*([^\n\r,;]+)");
        assertEquals("Copper Wire", scannedBody);

        EmailData emailWithAttachProduct = EmailData.builder().attachmentText("Product: Aluminum Rod").build();
        String scannedAttach = ReflectionTestUtils.invokeMethod(emailProcessorService, "scanFieldFromEmail", emailWithAttachProduct, "Product:\\s*([^\n\r,;]+)");
        assertEquals("Aluminum Rod", scannedAttach);
    }

    @Test
    @DisplayName("Test new helper methods for conversational reply detection and subject filtering to ensure Jacoco coverage")
    void testConversationalReplyAndSubjectFilteringCoverage() throws Exception {
        // 1. isAcknowledgementSubject
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "🚀 Your RFQ #RFQ-100 is Live — Suppliers Notified!"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "Your RFQs are Live"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "Some RFQs Were Created"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "One Quick Detail Needed"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "Could Not Process Your RFQ"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "Duplicate Request Received"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "File Size Exceeded"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "RFQ Acknowledgement"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "RFQ Created"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", (String) null));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", ""));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isAcknowledgementSubject", "Requirement for Laptops"));

        // 2. extractProductFromSubject with acknowledgement subjects
        assertEquals("", ReflectionTestUtils.invokeMethod(emailProcessorService, "extractProductFromSubject", "Re: 🚀 Your RFQ #RFQ-100 is Live — Suppliers Notified!"));
        assertEquals("", ReflectionTestUtils.invokeMethod(emailProcessorService, "extractProductFromSubject", "Re: ⚡ One Quick Detail Needed to Process Your RFQ"));
        assertEquals("Laptops", ReflectionTestUtils.invokeMethod(emailProcessorService, "extractProductFromSubject", "Re: Laptops"));

        // 3. isConversationalPhrase
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalPhrase", ""));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalPhrase", "thanks"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalPhrase", "thank you so much"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalPhrase", "ok, got it"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalPhrase", "noted and received"));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalPhrase", "please send quotes"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalPhrase", "delivery location is bangalore plant and pincode is 560058"));

        // 4. extractNewReplyContent
        assertEquals("", ReflectionTestUtils.invokeMethod(emailProcessorService, "extractNewReplyContent", (String) null));
        assertEquals("", ReflectionTestUtils.invokeMethod(emailProcessorService, "extractNewReplyContent", ""));
        String threadBody = "Thanks team!\n> Quoted line 1\n> Quoted line 2\nOn Tue, Sep 1 wrote:\nOriginal Message";
        assertEquals("Thanks team!", ReflectionTestUtils.invokeMethod(emailProcessorService, "extractNewReplyContent", threadBody));
        String threadBodyFrom = "Got it!\nFrom: support@procucev.com\nSent: Monday";
        assertEquals("Got it!", ReflectionTestUtils.invokeMethod(emailProcessorService, "extractNewReplyContent", threadBodyFrom));

        // 5. isConversationalReplyToCreatedRfq branches
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalReplyToCreatedRfq", (EmailData) null));
        
        EmailData regularEmail = EmailData.builder().subject("Requirement for Steel Pipes").body("Need 500 meters").build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalReplyToCreatedRfq", regularEmail));

        EmailData emailWithAttach = EmailData.builder()
                .subject("Re: Your RFQ #RFQ-100 is Live")
                .body("Thanks")
                .attachments(List.of(new File("test.txt")))
                .build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalReplyToCreatedRfq", emailWithAttach));

        EmailData emailWithQty = EmailData.builder()
                .subject("Re: Your RFQ #RFQ-100 is Live")
                .body("Quantity: 1000 Nos")
                .build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalReplyToCreatedRfq", emailWithQty));

        EmailData emailLongContent = EmailData.builder()
                .subject("Re: Your RFQ #RFQ-100 is Live")
                .body("A".repeat(300))
                .build();
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalReplyToCreatedRfq", emailLongContent));

        EmailData emailAckShortConv = EmailData.builder()
                .subject("Re: 🚀 Your RFQ #RFQ-100 is Live — Suppliers Notified!")
                .body("Thanks!")
                .build();
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(emailProcessorService, "isConversationalReplyToCreatedRfq", emailAckShortConv));

        // 6. mergeThreadContext when prior transaction status is RFQ_CREATED
        EmailData emailThreadCreated = EmailData.builder().inReplyTo("MSG-ALREADY-CREATED").build();
        EmailTransaction txCreated = new EmailTransaction();
        txCreated.setMessageId("MSG-ALREADY-CREATED");
        txCreated.setStatus("RFQ_CREATED");
        txCreated.setExtractionJson(new ObjectMapper().writeValueAsString(ExtractedRFQ.builder().items(List.of(RFQItem.builder().itemDescription("Old Item").build())).build()));
        Mockito.when(emailTransactionRepository.findByMessageId("MSG-ALREADY-CREATED")).thenReturn(Optional.of(txCreated));

        ExtractedRFQ current = ExtractedRFQ.builder().items(new ArrayList<>()).build();
        ExtractedRFQ mergedCreated = ReflectionTestUtils.invokeMethod(emailProcessorService, "mergeThreadContext", emailThreadCreated, current);
        assertNotNull(mergedCreated);
        assertTrue(mergedCreated.getItems().isEmpty());
    }

    @Test
    @DisplayName("Test RfqAiTokenUsageRepository saving, null check, and exception handling branches")
    void testRfqAiTokenUsageRepositoryBranches() {
        RfqAiTokenUsageRepository tokenRepo = Mockito.mock(RfqAiTokenUsageRepository.class);
        emailProcessorService.setRfqAiTokenUsageRepository(tokenRepo);

        EmailData email = EmailData.builder()
                .messageId("MSG-TOKEN-USAGE")
                .senderEmail("buyer@corp.com")
                .subject("Need valves")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-TOKEN-USAGE")).thenReturn(Optional.empty());

        TokenUsageTelemetry telemetry = TokenUsageTelemetry.builder()
                .messageId("MSG-TOKEN-USAGE")
                .modelName("gemini-3.7-flash")
                .promptTokens(250)
                .candidateTokens(75)
                .totalTokens(325)
                .attemptsCount(1)
                .estimatedCostUsd(0.00025)
                .build();

        ExtractedRFQ rfqWithTokens = ExtractedRFQ.builder()
                .buyerEmail("buyer@corp.com")
                .deliveryLocation("Pune")
                .deliveryDate("2026-09-15")
                .tokenUsage(telemetry)
                .items(List.of(RFQItem.builder().itemDescription("Ball Valve").quantity(5.0).uom("Nos").category("Valves").build()))
                .build();

        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfqWithTokens);
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(new ValidationService.ValidationResult(true, false, List.of(), null));

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-TU-1").deliveryDate("2026-09-15").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);
        Mockito.when(rfqApiService.submitRFQ(request)).thenReturn(RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-TU-1").build());
        Mockito.when(rfqRepository.save(any())).thenReturn(RFQEntity.builder().rfqNumber("RFQ-TU-1").buyerEmail("buyer@corp.com").build());

        // 1. Success case: token repo saves telemetry
        String res1 = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", res1);
        Mockito.verify(tokenRepo).save(Mockito.argThat(u ->
                "RFQ-TU-1".equals(u.getRfqNumber())
                        && "MSG-TOKEN-USAGE".equals(u.getMessageId())
                        && "gemini-3.7-flash".equals(u.getModelName())
                        && u.getPromptTokens() == 250
                        && u.getCandidateTokens() == 75
                        && u.getTotalTokens() == 325
                        && u.getAttemptsCount() == 1
        ));

        // 2. Exception case: token repo throws exception -> handled safely without failing RFQ creation
        Mockito.reset(tokenRepo);
        Mockito.doThrow(new RuntimeException("DB Connection Timeout")).when(tokenRepo).save(any());
        String res2 = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", res2);
        Mockito.verify(tokenRepo).save(any());

        // 3. Null tokenUsage case: tokenRepo.save is not called
        Mockito.reset(tokenRepo);
        rfqWithTokens.setTokenUsage(null);
        String res3 = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", res3);
        Mockito.verify(tokenRepo, Mockito.never()).save(any());

        // 4. Null tokenRepo case: safely bypassed
        emailProcessorService.setRfqAiTokenUsageRepository(null);
        rfqWithTokens.setTokenUsage(telemetry);
        String res4 = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", res4);
    }
}


