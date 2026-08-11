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
                .fromEmail("noreply@procucev.com")
                .subject("System Notification")
                .build();

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("SKIPPED_SYSTEM_EMAIL", result);
        Mockito.verify(emailReaderService).moveMessageToFolder(eq("MSG-SYS"), any());
    }

    @Test
    @DisplayName("Test processSingleEmail skips duplicate email")
    void testProcessSingleEmailDuplicate() {
        EmailData email = EmailData.builder()
                .messageId("MSG-DUP")
                .fromEmail("buyer@test.com")
                .subject("Need items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-DUP"))
                .thenReturn(Optional.of(EmailTransaction.builder().messageId("MSG-DUP").build()));

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("SKIPPED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail with unregistered buyer")
    void testProcessSingleEmailUnregisteredBuyer() {
        EmailData email = EmailData.builder()
                .messageId("MSG-UNREG")
                .fromEmail("unregistered@test.com")
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
                .fromEmail("buyer@test.com")
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
                .fromEmail("buyer@test.com")
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

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(false, true, "Missing Qty", List.of("Laptop"));
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("VALIDATION_FAILED", result);
        Mockito.verify(acknowledgementEmailService).sendMissingQuantityAcknowledgement(eq("buyer@test.com"), eq("Buyer"), anyList());
    }

    @Test
    @DisplayName("Test processSingleEmail success creation")
    void testProcessSingleEmailSuccess() {
        EmailData email = EmailData.builder()
                .messageId("MSG-SUCCESS")
                .fromEmail("buyer@test.com")
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

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, null, List.of());
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        RFQRequest request = RFQRequest.builder().rfqNumber("RFQ-999").deliveryDate("2026-08-25").build();
        Mockito.when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(request);

        RFQResponse apiResponse = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-999").build();
        Mockito.when(rfqApiService.submitRFQ(request)).thenReturn(apiResponse);

        RFQEntity savedEntity = RFQEntity.builder().rfqNumber("RFQ-999").buyerEmail("buyer@test.com").build();
        Mockito.when(rfqRepository.save(any())).thenReturn(savedEntity);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        Mockito.verify(acknowledgementEmailService).sendSuccessAcknowledgement(any(), eq(verifiedBuyer));
    }

    @Test
    @DisplayName("Test processSingleEmail deduplication and item grouping")
    void testProcessSingleEmailDeduplicationAndGrouping() {
        EmailData email = EmailData.builder()
                .messageId("MSG-DEDUP")
                .fromEmail("buyer@test.com")
                .subject("Need Items")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-DEDUP")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        List<RFQItem> items = List.of(
                RFQItem.builder().itemDescription("Dell Laptop").quantity(5.0).deliveryLocation("Loc1").deliveryDate("2026-08-25").build(),
                RFQItem.builder().itemDescription("Dell Laptop").quantity(5.0).deliveryLocation("Loc1").deliveryDate("2026-08-25").build(), // duplicate
                RFQItem.builder().itemDescription("").build(), // blank description
                RFQItem.builder().itemDescription("Monitor").quantity(2.0).deliveryLocation("Loc2").deliveryDate("2026-08-30").build()
        );

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .buyerEmail("buyer@test.com")
                .items(items)
                .build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, null, List.of());
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
                .fromEmail("buyer@test.com")
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

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, null, List.of());
        Mockito.when(validationService.validateWithDetails(any())).thenReturn(valResult);

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("VALIDATION_FAILED", result);
    }

    @Test
    @DisplayName("Test processSingleEmail submitRFQ failure and partial failure")
    void testProcessSingleEmailSubmitRfqFailure() {
        EmailData email = EmailData.builder()
                .messageId("MSG-SUBMIT-FAIL")
                .fromEmail("buyer@test.com")
                .subject("Submit Fail")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-SUBMIT-FAIL")).thenReturn(Optional.empty());

        Buyer verifiedBuyer = Buyer.builder().email("buyer@test.com").name("Buyer").verified(true).build();
        Mockito.when(buyerVerificationService.verifyAndGetBuyer("buyer@test.com")).thenReturn(verifiedBuyer);

        List<RFQItem> items = List.of(
                RFQItem.builder().itemDescription("Laptop").quantity(1.0).build()
        );
        ExtractedRFQ rfq = ExtractedRFQ.builder().buyerEmail("buyer@test.com").items(items).build();
        Mockito.when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(rfq);

        ValidationService.ValidationResult valResult = new ValidationService.ValidationResult(true, false, null, List.of());
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
                .fromEmail("buyer@test.com")
                .subject("Crash")
                .build();

        Mockito.when(emailTransactionRepository.findByMessageId("MSG-EX")).thenThrow(new RuntimeException("Crash"));

        String result = emailProcessorService.processSingleEmail(email);
        assertEquals("FAILED", result);
    }

    @Test
    @DisplayName("Test processUnreadEmails with errors")
    void testProcessUnreadEmailsWithErrors() {
        EmailData email1 = EmailData.builder().messageId("M1").fromEmail("buyer@test.com").build();
        Mockito.when(emailReaderService.fetchUnreadEmails()).thenReturn(List.of(email1));
        Mockito.when(emailTransactionRepository.findByMessageId("M1")).thenThrow(new RuntimeException("Error"));

        ProcessingStats stats = emailProcessorService.processUnreadEmails();
        assertNotNull(stats);
        assertEquals("COMPLETED_WITH_ERRORS", stats.getStatus());
        assertEquals(1, stats.getErrors());
    }
}
