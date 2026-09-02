package com.portal.procucev.rfq;

import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
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
import com.portal.procucev.rfq.util.QuantityNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RfqReplyProcessingTest {

    private EmailReaderService emailReaderService;
    private AIExtractionService aiExtractionService;
    private ValidationService validationService;
    private CategoryClassificationService categoryClassificationService;
    private BuyerVerificationService buyerVerificationService;
    private RFQBuilderService rfqBuilderService;
    private RFQApiService rfqApiService;
    private AcknowledgementEmailService acknowledgementEmailService;
    private EmailTransactionRepository emailTransactionRepository;
    private RFQRepository rfqRepository;
    private RfqItemRecordRepository rfqItemRecordRepository;
    private DateParser dateParser;
    private JavaMailSender mailSender;
    private ObjectMapper objectMapper;

    private EmailProcessorService emailProcessorService;

    @BeforeEach
    void setUp() {
        emailReaderService = mock(EmailReaderService.class);
        aiExtractionService = mock(AIExtractionService.class);
        validationService = mock(ValidationService.class);
        categoryClassificationService = mock(CategoryClassificationService.class);
        buyerVerificationService = mock(BuyerVerificationService.class);
        rfqBuilderService = mock(RFQBuilderService.class);
        rfqApiService = mock(RFQApiService.class);
        emailTransactionRepository = mock(EmailTransactionRepository.class);
        rfqRepository = mock(RFQRepository.class);
        rfqItemRecordRepository = mock(RfqItemRecordRepository.class);
        dateParser = new DateParser();
        mailSender = mock(JavaMailSender.class);
        objectMapper = new ObjectMapper();

        acknowledgementEmailService = new AcknowledgementEmailService(mailSender, objectMapper);

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
                dateParser,
                objectMapper
        );

        Buyer defaultBuyer = Buyer.builder()
                .userId("BUYER-001")
                .email("buyer@procucev.com")
                .name("John Buyer")
                .address("123 Industrial Area")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560058")
                .verified(true)
                .build();

        when(buyerVerificationService.verifyAndGetBuyer("buyer@procucev.com")).thenReturn(defaultBuyer);

        RFQResponse successResponse = new RFQResponse();
        successResponse.setStatus("SUCCESS");
        successResponse.setRfqNumber("RFQ-9999");
        when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(successResponse);
        when(rfqRepository.save(any(RFQEntity.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    @DisplayName("TEST 1: Quantity:1000 -> Expected: 1000.0")
    void test1_QuantityColonNoSpace() {
        assertEquals(1000.0, QuantityNormalizer.normalize("Quantity:1000"));
    }

    @Test
    @DisplayName("TEST 2: Quantity: 1000 -> Expected: 1000.0")
    void test2_QuantityColonSpace() {
        assertEquals(1000.0, QuantityNormalizer.normalize("Quantity: 1000"));
    }

    @Test
    @DisplayName("TEST 3: Qty:1000 -> Expected: 1000.0")
    void test3_QtyColonNoSpace() {
        assertEquals(1000.0, QuantityNormalizer.normalize("Qty:1000"));
    }

    @Test
    @DisplayName("TEST 4: Required Delivery Date: 25-Apr-2027 -> Expected: 2027-04-25")
    void test4_DateParsing() {
        String parsedDate = dateParser.parseDateString("25-Apr-2027");
        assertEquals("2027-04-25", parsedDate);
    }

    @Test
    @DisplayName("TEST 5: Buyer initially sends incomplete RFQ, then replies with quantity/location/date/specifications -> RFQ CREATED")
    void test5_BuyerReplyCompleteRFQCreated() {
        EmailData replyEmail = EmailData.builder()
                .messageId("MSG-REPLY-001")
                .senderEmail("buyer@procucev.com")
                .subject("Re: Request for Quotation – Helical Gearbox")
                .body("Product: Helical Gearbox\n\nSpecifications:\nInline helical gearbox, 15 HP capacity, reduction ratio 20:1, cast iron housing\n\nBrand:\nSEW Eurodrive\n\nQuantity:\n1000\n\nDelivery Location:\nABC Manufacturing Plant, Bangalore\n\nRequired Delivery Date:\n25-Apr-2027")
                .attachments(new ArrayList<>())
                .build();

        RFQItem item = RFQItem.builder()
                .itemDescription("Helical Gearbox")
                .specification("Inline helical gearbox, 15 HP capacity, reduction ratio 20:1")
                .brand("SEW Eurodrive")
                .quantity(1000.0)
                .deliveryLocation("ABC Manufacturing Plant, Bangalore")
                .deliveryDate("2027-04-25")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(new ArrayList<>(java.util.List.of(item)))
                .build();

        when(aiExtractionService.extractRFQFromEmail(replyEmail)).thenReturn(extracted);

        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-2027").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);

        String status = emailProcessorService.processSingleEmail(replyEmail);
        assertEquals("RFQ_CREATED", status);

        verify(rfqRepository, times(1)).save(any(RFQEntity.class));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        SimpleMailMessage sentMail = mailCaptor.getValue();

        assertEquals("rfq@procucev.com", sentMail.getFrom());
        assertEquals("buyer@procucev.com", sentMail.getTo()[0]);
        assertEquals("🚀 Your RFQ #RFQ-2027 is Live — Suppliers Notified!", sentMail.getSubject());
    }

    @Test
    @DisplayName("TEST 6: Initial email has quantity missing, reply contains Quantity:1000 -> Do NOT send Details Missing acknowledgement again")
    void test6_ReplyWithQuantityColon1000NoDetailsMissingSent() {
        EmailData replyEmail = EmailData.builder()
                .messageId("MSG-REPLY-002")
                .senderEmail("buyer@procucev.com")
                .subject("Re: Helical Gearbox Request")
                .body("Quantity:1000\nRequired Delivery Date: 25-Apr-2027\nDelivery Location: Bangalore")
                .attachments(new ArrayList<>())
                .build();

        RFQItem item = RFQItem.builder()
                .itemDescription("Helical Gearbox")
                .quantity(1000.0)
                .deliveryLocation("Bangalore")
                .deliveryDate("2027-04-25")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(new ArrayList<>(java.util.List.of(item)))
                .build();

        when(aiExtractionService.extractRFQFromEmail(replyEmail)).thenReturn(extracted);

        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-2028").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);

        String status = emailProcessorService.processSingleEmail(replyEmail);
        assertEquals("RFQ_CREATED", status);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());

        SimpleMailMessage sentMail = mailCaptor.getValue();
        assertNotEquals("⚡ One Quick Detail Needed to Process Your RFQ", sentMail.getSubject());
        assertEquals("🚀 Your RFQ #RFQ-2028 is Live — Suppliers Notified!", sentMail.getSubject());
    }

    @Test
    @DisplayName("TEST 7: Initial email has Product + specs, Reply has Quantity + location + date -> Merge both emails and create ONE RFQ")
    void test7_MergeInitialAndReplyEmails() throws Exception {
        EmailData replyEmail = EmailData.builder()
                .messageId("MSG-REPLY-003")
                .senderEmail("buyer@procucev.com")
                .subject("Re: Helical Gearbox Specs")
                .body("Quantity: 1000\nDelivery Location: Bangalore Plant\nRequired Delivery Date: 2027-04-25")
                .inReplyTo("MSG-INITIAL-003")
                .attachments(new ArrayList<>())
                .build();

        ExtractedRFQ initial = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(new ArrayList<>(java.util.List.of(RFQItem.builder()
                        .itemDescription("Helical Gearbox")
                        .specification("Inline helical gearbox 15 HP")
                        .build())))
                .build();
        when(emailTransactionRepository.findByMessageId("MSG-INITIAL-003"))
                .thenReturn(java.util.Optional.of(com.portal.procucev.rfq.entity.EmailTransaction.builder()
                        .messageId("MSG-INITIAL-003")
                        .extractionJson(objectMapper.writeValueAsString(initial))
                        .build()));

        RFQItem replyItem = RFQItem.builder()
                .quantity(1000.0)
                .deliveryLocation("Bangalore Plant")
                .deliveryDate("2027-04-25")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(new ArrayList<>(java.util.List.of(replyItem)))
                .build();

        when(aiExtractionService.extractRFQFromEmail(replyEmail)).thenReturn(extracted);

        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-2029").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);

        String status = emailProcessorService.processSingleEmail(replyEmail);
        assertEquals("RFQ_CREATED", status);

        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("TEST 8: Reply email is processed even if message ID is present in transaction log")
    void test8_PreventDuplicateProcessingOnSameReplyMessageId() {
        EmailData replyEmail = EmailData.builder()
                .messageId("MSG-REPLY-DUPLICATE-001")
                .senderEmail("buyer@procucev.com")
                .subject("Re: Helical Gearbox")
                .body("Quantity:1000")
                .build();

        when(emailTransactionRepository.findByMessageId("MSG-REPLY-DUPLICATE-001"))
                .thenReturn(java.util.Optional.of(com.portal.procucev.rfq.entity.EmailTransaction.builder().messageId("MSG-REPLY-DUPLICATE-001").status("FAILED").build()));

        RFQItem item = RFQItem.builder().itemDescription("Helical Gearbox").quantity(1000.0).build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(java.util.List.of(item)).build();
        when(aiExtractionService.extractRFQFromEmail(replyEmail)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any()))
                .thenReturn(RFQRequest.builder().rfqNumber("RFQ-REPLY-8").build());

        String status = emailProcessorService.processSingleEmail(replyEmail);
        assertEquals("RFQ_CREATED", status);

        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("TEST 9: Conversational reply ('Thanks') to RFQ creation acknowledgement -> SKIPPED_REPLY_ACKNOWLEDGEMENT, no duplicate RFQ created")
    void test9_ConversationalReplyToAcknowledgementSkipped() {
        EmailData replyEmail = EmailData.builder()
                .messageId("MSG-REPLY-ACK-001")
                .senderEmail("buyer@procucev.com")
                .subject("Re: 🚀 Your RFQ #RFQ-2027 is Live — Suppliers Notified!")
                .body("Thanks!")
                .inReplyTo("MSG-ORIGINAL-001")
                .attachments(new ArrayList<>())
                .build();

        when(emailTransactionRepository.findByMessageId("MSG-ORIGINAL-001"))
                .thenReturn(java.util.Optional.of(com.portal.procucev.rfq.entity.EmailTransaction.builder()
                        .messageId("MSG-ORIGINAL-001")
                        .status("RFQ_CREATED")
                        .build()));

        String status = emailProcessorService.processSingleEmail(replyEmail);
        assertEquals("SKIPPED_REPLY_ACKNOWLEDGEMENT", status);

        verify(rfqRepository, never()).save(any(RFQEntity.class));
        verify(aiExtractionService, never()).extractRFQFromEmail(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("TEST 10: Buyer replies 'Got it, thank you team' to created RFQ thread -> SKIPPED_REPLY_ACKNOWLEDGEMENT")
    void test10_BuyerReplyGotItSkipped() {
        EmailData replyEmail = EmailData.builder()
                .messageId("MSG-REPLY-ACK-002")
                .senderEmail("buyer@procucev.com")
                .subject("Re: Helical Gearbox Requirement")
                .body("Got it, thank you team!\n\nOn Tue, Sep 1, 2026, rfq@procucev.com wrote:\n> Hi John, Great news! Your requirement has been converted into RFQ...")
                .inReplyTo("MSG-ORIGINAL-002")
                .attachments(new ArrayList<>())
                .build();

        when(emailTransactionRepository.findByMessageId("MSG-ORIGINAL-002"))
                .thenReturn(java.util.Optional.of(com.portal.procucev.rfq.entity.EmailTransaction.builder()
                        .messageId("MSG-ORIGINAL-002")
                        .status("RFQ_CREATED")
                        .build()));

        String status = emailProcessorService.processSingleEmail(replyEmail);
        assertEquals("SKIPPED_REPLY_ACKNOWLEDGEMENT", status);

        verify(rfqRepository, never()).save(any(RFQEntity.class));
        verify(aiExtractionService, never()).extractRFQFromEmail(any());
    }
}

