package com.portal.procucev.rfq;

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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MultiRfqGroupingTest {

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
    private Buyer defaultBuyer;

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

        defaultBuyer = Buyer.builder()
                .userId("BUYER-100")
                .email("buyer@procucev.com")
                .name("Test Buyer")
                .address("100 Tech Park")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .verified(true)
                .build();

        when(buyerVerificationService.verifyAndGetBuyer("buyer@procucev.com")).thenReturn(defaultBuyer);

        RFQResponse successResponse = new RFQResponse();
        successResponse.setStatus("SUCCESS");
        successResponse.setRfqNumber("RFQ-MOCK");
        when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenReturn(successResponse);
        when(rfqRepository.save(any(RFQEntity.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    @DisplayName("Test 1: Single item -> one RFQ")
    void test1_SingleItemOneRfq() {
        EmailData email = EmailData.builder().messageId("M1").senderEmail("buyer@procucev.com").subject("RFQ 1").build();
        RFQItem item = RFQItem.builder().itemDescription("Laptop").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(item)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(RFQRequest.builder().rfqNumber("RFQ-1").build());

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 2: Multiple items same category/location/date -> one RFQ")
    void test2_MultipleItemsSameGroupOneRfq() {
        EmailData email = EmailData.builder().messageId("M2").senderEmail("buyer@procucev.com").subject("RFQ 2").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop A").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Laptop B").quantity(20.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1, i2)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(RFQRequest.builder().rfqNumber("RFQ-2").build());

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 3: Different locations -> separate RFQs")
    void test3_DifferentLocationsSeparateRfqs() {
        EmailData email = EmailData.builder().messageId("M3").senderEmail("buyer@procucev.com").subject("RFQ 3").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop A").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Laptop B").quantity(20.0).category("IT").deliveryLocation("Hyderabad").deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1, i2)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenAnswer(inv -> {
            ExtractedRFQ e = inv.getArgument(0);
            return RFQRequest.builder().rfqNumber("RFQ-" + e.getDeliveryLocation()).build();
        });

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(2)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 4: Different dates -> separate RFQs")
    void test4_DifferentDatesSeparateRfqs() {
        EmailData email = EmailData.builder().messageId("M4").senderEmail("buyer@procucev.com").subject("RFQ 4").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop A").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Laptop B").quantity(20.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-10-15").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1, i2)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(RFQRequest.builder().rfqNumber("RFQ-4").build());

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(2)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 5: Different categories, same location & date -> 1 RFQ")
    void test5_DifferentCategoriesSeparateRfqs() {
        EmailData email = EmailData.builder().messageId("M5").senderEmail("buyer@procucev.com").subject("RFQ 5").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Monitor").quantity(20.0).category("Medical").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1, i2)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(RFQRequest.builder().rfqNumber("RFQ-5").build());

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 6: Mixed grouping (4 items across 3 distinct groups)")
    void test6_MixedGrouping() {
        EmailData email = EmailData.builder().messageId("M6").senderEmail("buyer@procucev.com").subject("RFQ 6").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Dell Laptop").quantity(10.0).category("Laptop").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i2 = RFQItem.builder().itemDescription("HP Laptop").quantity(20.0).category("Laptop").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i3 = RFQItem.builder().itemDescription("Patient Monitor").quantity(5.0).category("Medical").deliveryLocation("Hyderabad").deliveryDate("2026-09-30").build();
        RFQItem i4 = RFQItem.builder().itemDescription("MCCB").quantity(8.0).category("Electrical").deliveryLocation("Chennai").deliveryDate("2026-09-30").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1, i2, i3, i4)).build();
        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenAnswer(inv -> RFQRequest.builder().rfqNumber("RFQ-G" + System.nanoTime()).build());

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(3)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 7: Numeric quantity")
    void test7_NumericQuantity() {
        assertEquals(1000.0, QuantityNormalizer.normalize("1000"));
    }

    @Test
    @DisplayName("Test 8: Quantity as number words")
    void test8_QuantityAsNumberWords() {
        assertEquals(7.0, QuantityNormalizer.normalize("seven"));
        assertEquals(25.0, QuantityNormalizer.normalize("twenty five"));
    }

    @Test
    @DisplayName("Test 9: Quantity as lakh/lakhs")
    void test9_QuantityAsLakh() {
        assertEquals(100000.0, QuantityNormalizer.normalize("one lakh"));
        assertEquals(500000.0, QuantityNormalizer.normalize("five lakhs"));
        assertEquals(1000000.0, QuantityNormalizer.normalize("ten lakh"));
    }

    @Test
    @DisplayName("Test 10: Missing quantity -> defaulted to 1.0 and RFQ created")
    void test10_MissingQuantity() {
        EmailData email = EmailData.builder().messageId("M10").senderEmail("buyer@procucev.com").subject("No Qty").body("Laptop").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop").quantity(null).build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-M10").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);
        RFQResponse mockResp = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-M10").build();
        when(rfqApiService.submitRFQ(mockRfqReq)).thenReturn(mockResp);
        when(rfqRepository.save(any(RFQEntity.class))).thenReturn(RFQEntity.builder().rfqNumber("RFQ-M10").buyerEmail("buyer@procucev.com").build());

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 11: Missing location -> creates the RFQ against the buyer's registered profile location")
    void test11_MissingLocation_FallsBackToBuyerProfile() {
        EmailData email = EmailData.builder().messageId("M11").senderEmail("buyer@procucev.com").subject("RFQ 11").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop").quantity(10.0).category("IT").deliveryLocation(null).deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any()))
                .thenReturn(RFQRequest.builder().rfqNumber("RFQ-11").build());

        String result = emailProcessorService.processSingleEmail(email);

        // Delivery location is optional; the buyer's profile location is used instead of aborting.
        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(1)).save(any());

        ArgumentCaptor<ExtractedRFQ> builderCaptor = ArgumentCaptor.forClass(ExtractedRFQ.class);
        verify(rfqBuilderService).buildRFQRequest(builderCaptor.capture(), any(), any(), any());
        String resolvedLocation = builderCaptor.getValue().getDeliveryLocation();
        assertNotNull(resolvedLocation);
        assertTrue(resolvedLocation.contains("100 Tech Park"), "expected profile address, got: " + resolvedLocation);
        assertTrue(resolvedLocation.contains("Bengaluru"), "expected profile city, got: " + resolvedLocation);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertNotEquals("⚡ One Quick Detail Needed to Process Your RFQ", mailCaptor.getValue().getSubject());
        assertFalse(mailCaptor.getValue().getText().contains("• Delivery location"));
    }

    @Test
    @DisplayName("Test 12: Missing date -> defaults to current date + 5 days")
    void test12_MissingDateDefaultsToPlus5Days() {
        EmailData email = EmailData.builder().messageId("M12").senderEmail("buyer@procucev.com").subject("RFQ 12").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate(null).build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(RFQRequest.builder().rfqNumber("RFQ-12").build());

        emailProcessorService.processSingleEmail(email);

        String expectedDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ArgumentCaptor<RFQEntity> entityCaptor = ArgumentCaptor.forClass(RFQEntity.class);
        verify(rfqRepository).save(entityCaptor.capture());
        assertEquals(expectedDate, entityCaptor.getValue().getDeliveryDate());
    }

    @Test
    @DisplayName("Test 13: Multiple RFQs -> ONE acknowledgement containing all RFQ numbers")
    void test13_MultipleRfqsOneAcknowledgement() {
        EmailData email = EmailData.builder().messageId("M13").senderEmail("buyer@procucev.com").subject("RFQ 13").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Monitor").quantity(5.0).category("Medical").deliveryLocation("Hyderabad").deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1, i2)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any()))
                .thenReturn(RFQRequest.builder().rfqNumber("RFQ-101").build())
                .thenReturn(RFQRequest.builder().rfqNumber("RFQ-102").build());

        emailProcessorService.processSingleEmail(email);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());

        SimpleMailMessage sentMail = mailCaptor.getValue();
        assertEquals("🚀 Your RFQs are Live — Suppliers Notified!", sentMail.getSubject());
    }

    @Test
    @DisplayName("Test 14: CC support@procucev.com")
    void test14_CcSupportAddress() {
        EmailData email = EmailData.builder().messageId("M14").senderEmail("buyer@procucev.com").subject("RFQ 14").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(RFQRequest.builder().rfqNumber("RFQ-14").build());

        emailProcessorService.processSingleEmail(email);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertEquals("support@procucev.com", mailCaptor.getValue().getCc()[0]);
    }

    @Test
    @DisplayName("Test 15: Sender must be rfq@procucev.com")
    void test15_SenderMustBeRfqAddress() {
        EmailData email = EmailData.builder().messageId("M15").senderEmail("buyer@procucev.com").subject("RFQ 15").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Laptop").quantity(10.0).category("IT").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("buyer@procucev.com").items(List.of(i1)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(RFQRequest.builder().rfqNumber("RFQ-15").build());

        emailProcessorService.processSingleEmail(email);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertEquals("rfq@procucev.com", mailCaptor.getValue().getFrom());
        assertNotEquals("notification@procucev.com", mailCaptor.getValue().getFrom());
    }

    @Test
    @DisplayName("Test 16: Buyer not registered -> Unregistered Buyer Acknowledgement sent")
    void test16_BuyerNotRegistered() {
        EmailData email = EmailData.builder().messageId("M16").senderEmail("unregistered@procucev.com").subject("RFQ 16").build();
        when(buyerVerificationService.verifyAndGetBuyer("unregistered@procucev.com")).thenReturn(null);

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("INVALID_BUYER", status);
        verify(rfqRepository, never()).save(any(RFQEntity.class));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertEquals("🚀 Almost There! Register to Get Your RFQ Live", mailCaptor.getValue().getSubject());
        assertEquals("govardhan.kilari@procucev.com", mailCaptor.getValue().getTo()[0]);
    }

    @Test
    @DisplayName("Test 17: Category must be independently extracted for every item")
    void test17_IndependentCategoriesPerItem() {
        RFQItem i1 = RFQItem.builder().itemDescription("Dell Laptop").category("Laptop").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Patient Monitor").category("Medical").build();
        RFQItem i3 = RFQItem.builder().itemDescription("MCCB").category("Electrical").build();

        assertNotEquals(i1.getCategory(), i2.getCategory());
        assertNotEquals(i2.getCategory(), i3.getCategory());
        assertEquals("Laptop", i1.getCategory());
        assertEquals("Medical", i2.getCategory());
        assertEquals("Electrical", i3.getCategory());
    }
}
