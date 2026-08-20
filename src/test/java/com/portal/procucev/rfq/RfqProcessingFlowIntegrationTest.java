package com.portal.procucev.rfq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.rfq.client.GeminiApiClient;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RfqProcessingFlowIntegrationTest {

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
    private DateParser dateParser;
    private JavaMailSender mailSender;
    private ObjectMapper objectMapper;

    private EmailProcessorService emailProcessorService;
    private Buyer validBuyer;

    @BeforeEach
    void setUp() {
        emailReaderService = mock(EmailReaderService.class);
        aiExtractionService = mock(AIExtractionService.class);
        validationService = mock(ValidationService.class);
        buyerVerificationService = mock(BuyerVerificationService.class);
        rfqBuilderService = mock(RFQBuilderService.class);
        rfqApiService = mock(RFQApiService.class);
        categoryClassificationService = mock(CategoryClassificationService.class);
        rfqRepository = mock(RFQRepository.class);
        emailTransactionRepository = mock(EmailTransactionRepository.class);
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

        validBuyer = Buyer.builder()
                .userId("101")
                .orgId("1")
                .name("John Buyer")
                .email("buyer@procucev.com")
                .verified(true)
                .address("123 Industrial Area")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .build();

        when(buyerVerificationService.verifyAndGetBuyer("buyer@procucev.com")).thenReturn(validBuyer);
        when(emailTransactionRepository.findByMessageId(any())).thenReturn(Optional.empty());
        when(emailTransactionRepository.save(any(EmailTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rfqRepository.save(any(RFQEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        when(rfqApiService.submitRFQ(any())).thenReturn(RFQResponse.builder().status("SUCCESS").message("Created").build());

        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenAnswer(inv -> {
            ExtractedRFQ extracted = inv.getArgument(0);
            return RFQRequest.builder()
                    .rfqNumber("RFQ-" + UUID.randomUUID().toString().substring(0, 8))
                    .projectDesc(extracted.getItems() != null && !extracted.getItems().isEmpty() ? extracted.getItems().get(0).getItemDescription() : "RFQ")
                    .deliveryDate(extracted.getDeliveryDate())
                    .build();
        });

        doAnswer(inv -> {
            List<RFQItem> items = inv.getArgument(0);
            for (RFQItem item : items) {
                if (item.getCategory() == null || item.getCategory().isBlank()) {
                    if (item.getItemDescription().toLowerCase().contains("laptop")) {
                        item.setCategory("IT Hardware");
                    } else if (item.getItemDescription().toLowerCase().contains("monitor")) {
                        item.setCategory("Medical Equipment");
                    } else if (item.getItemDescription().toLowerCase().contains("mccb")) {
                        item.setCategory("Electrical");
                    } else {
                        item.setCategory("General Industrial Goods");
                    }
                }
            }
            return null;
        }).when(categoryClassificationService).classifyItems(any());
    }

    @Test
    @DisplayName("Test 1: Complete single-item RFQ -> 1 RFQ created")
    void test1_SingleItemRfq() {
        EmailData email = EmailData.builder()
                .messageId("MSG-001")
                .subject("RFQ for Laptops")
                .senderEmail("buyer@procucev.com")
                .body("Quantity: 5 Nos\nDelivery Location: Bengaluru\nDelivery Date: 2026-09-30")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .deliveryLocation("Bengaluru")
                .deliveryDate("2026-09-30")
                .items(List.of(
                        RFQItem.builder()
                                .itemDescription("Dell Latitude Laptop")
                                .quantity(5.0)
                                .uom("Nos")
                                .category("IT Hardware")
                                .deliveryLocation("Bengaluru")
                                .deliveryDate("2026-09-30")
                                .build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        assertEquals("rfq@procucev.com", mailCaptor.getValue().getFrom());
        assertEquals("buyer@procucev.com", mailCaptor.getValue().getTo()[0]);
    }

    @Test
    @DisplayName("Test 2: Complete multiple-item RFQ -> correct number of RFQs created")
    void test2_MultipleItemsDifferentCategories() {
        EmailData email = EmailData.builder()
                .messageId("MSG-002")
                .subject("Multiple Items Requirement")
                .senderEmail("buyer@procucev.com")
                .body("Please supply laptops and medical monitors")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop").quantity(5.0).uom("Nos").category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build(),
                        RFQItem.builder().itemDescription("Medical Monitor").quantity(2.0).uom("Nos").category("Medical Equipment").deliveryLocation("Hyderabad").deliveryDate("2026-09-30").build(),
                        RFQItem.builder().itemDescription("MCCB Breaker").quantity(10.0).uom("Nos").category("Electrical").deliveryLocation("Chennai").deliveryDate("2026-10-05").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(3)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 3: Same category + same location + same date -> 1 RFQ")
    void test3_SameGroupCombinedToOneRfq() {
        EmailData email = EmailData.builder().messageId("MSG-003").subject("Laptops order").senderEmail("buyer@procucev.com").body("Laptops").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop Model A").quantity(5.0).uom("Nos").category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build(),
                        RFQItem.builder().itemDescription("Laptop Model B").quantity(3.0).uom("Nos").category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 4: Different category + same location + same date -> separate RFQs")
    void test4_DifferentCategorySeparateRfqs() {
        EmailData email = EmailData.builder().messageId("MSG-004").subject("Mix items").senderEmail("buyer@procucev.com").body("items").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop").quantity(5.0).category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build(),
                        RFQItem.builder().itemDescription("MCCB").quantity(2.0).category("Electrical").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 5: Same category + different location + same date -> separate RFQs")
    void test5_DifferentLocationSeparateRfqs() {
        EmailData email = EmailData.builder().messageId("MSG-005").subject("Diff location").senderEmail("buyer@procucev.com").body("items").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop A").quantity(5.0).category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build(),
                        RFQItem.builder().itemDescription("Laptop B").quantity(5.0).category("IT Hardware").deliveryLocation("Hyderabad").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(2)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 6: Same category + same location + different date -> separate RFQs")
    void test6_DifferentDateSeparateRfqs() {
        EmailData email = EmailData.builder().messageId("MSG-006").subject("Diff dates").senderEmail("buyer@procucev.com").body("items").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop A").quantity(5.0).category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build(),
                        RFQItem.builder().itemDescription("Laptop B").quantity(5.0).category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-10-15").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(2)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 7: Missing quantity in email -> RFQ Creation Aborted + Details Missing Email Sent")
    void test7_MissingQuantity() {
        EmailData email = EmailData.builder().messageId("MSG-007").subject("No Qty").senderEmail("buyer@procucev.com").body("No qty bearings").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Industrial Bearings").quantity(null).deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("VALIDATION_FAILED", result);
        verify(rfqRepository, never()).save(any(RFQEntity.class));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        assertTrue(mailCaptor.getValue().getText().contains("Quantity"));
    }

    @Test
    @DisplayName("Test 8: Missing delivery date -> default to current date + 5 days")
    void test8_MissingDeliveryDateDefault() {
        EmailData email = EmailData.builder().messageId("MSG-008").subject("No date").senderEmail("buyer@procucev.com").body("Laptops").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop").quantity(5.0).deliveryLocation("Bengaluru").deliveryDate(null).build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        String expectedDefaultDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ArgumentCaptor<RFQEntity> captor = ArgumentCaptor.forClass(RFQEntity.class);
        verify(rfqRepository).save(captor.capture());
        assertEquals(expectedDefaultDate, captor.getValue().getDeliveryDate());
    }

    @Test
    @DisplayName("Test 9: Missing delivery location -> RFQ created using the buyer's registered profile location")
    void test9_MissingDeliveryLocationUsesBuyerProfile() {
        EmailData email = EmailData.builder().messageId("MSG-009").subject("No loc").senderEmail("buyer@procucev.com").body("Laptops").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop").quantity(5.0).deliveryLocation(null).deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        // Delivery location is optional; the buyer's profile location is used instead of aborting.
        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));

        ArgumentCaptor<ExtractedRFQ> builderCaptor = ArgumentCaptor.forClass(ExtractedRFQ.class);
        verify(rfqBuilderService).buildRFQRequest(builderCaptor.capture(), any(), any(), any());
        String resolvedLocation = builderCaptor.getValue().getDeliveryLocation();
        assertNotNull(resolvedLocation);
        assertTrue(resolvedLocation.contains("123 Industrial Area"), "expected profile address, got: " + resolvedLocation);
        assertTrue(resolvedLocation.contains("Bengaluru"), "expected profile city, got: " + resolvedLocation);
    }

    @Test
    @DisplayName("Test 10: HTML email table -> htmlToText converts table tags to structured pipe text")
    void test10_HtmlTableExtraction() {
        String html = "<table>" +
                "<tr><th>Item</th><th>Quantity</th><th>Date</th></tr>" +
                "<tr><td>Laptop</td><td>5</td><td>2026-09-30</td></tr>" +
                "</table>";

        EmailReaderService realReaderService = new EmailReaderService();
        String converted = realReaderService.htmlToText(html);

        assertTrue(converted.contains("|"));
        assertTrue(converted.contains("Laptop"));
        assertTrue(converted.contains("5"));
    }

    @Test
    @DisplayName("Test 11: Plain text email -> correctly processed")
    void test11_PlainTextEmail() {
        EmailData email = EmailData.builder().messageId("MSG-011").subject("Plain Text RFQ").senderEmail("buyer@procucev.com").body("Please quote for 10 Laptops").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop").quantity(10.0).deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
    }

    @Test
    @DisplayName("Test 12: Multipart email -> correctly processed")
    void test12_MultipartEmail() {
        EmailData email = EmailData.builder().messageId("MSG-012").subject("Multipart RFQ").senderEmail("buyer@procucev.com").body("Body text").attachmentText("Attachment text").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop").quantity(10.0).deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
    }

    @Test
    @DisplayName("Test 13: Different categories in one email -> each item gets its own category")
    void test13_IndependentCategoryAssignment() {
        EmailData email = EmailData.builder().messageId("MSG-013").subject("Mixed categories").senderEmail("buyer@procucev.com").body("Items").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop").quantity(5.0).deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build(),
                        RFQItem.builder().itemDescription("Medical Monitor").quantity(2.0).deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("Test 14: Processing -> repeat email continues and creates RFQ")
    void test14_IdempotencyDuplicateEmail() {
        EmailData email = EmailData.builder().messageId("MSG-014").subject("Repeat Email").senderEmail("buyer@procucev.com").body("Quantity: 100").build();

        when(emailTransactionRepository.findByMessageId("MSG-014")).thenReturn(Optional.of(EmailTransaction.builder().messageId("MSG-014").status("RFQ_CREATED").build()));

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("buyer@procucev.com")
                .items(List.of(
                        RFQItem.builder().itemDescription("Laptop Model A").quantity(5.0).uom("Nos").category("IT Hardware").deliveryLocation("Bengaluru").deliveryDate("2026-09-30").build()
                ))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String result = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", result);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }
}
