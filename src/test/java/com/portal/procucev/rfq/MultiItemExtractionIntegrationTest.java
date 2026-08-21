package com.portal.procucev.rfq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MultiItemExtractionIntegrationTest {

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
                .userId("BUYER-BANGALORE")
                .email("procurement@abccorp.com")
                .name("ABC Corporate Buyer")
                .address("Plot No. 25, 2nd Main Road, Whitefield Industrial Area")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560066")
                .verified(true)
                .build();

        when(buyerVerificationService.verifyAndGetBuyer(anyString())).thenReturn(defaultBuyer);

        when(rfqApiService.submitRFQ(any(RFQRequest.class))).thenAnswer(inv -> {
            RFQRequest req = inv.getArgument(0);
            return RFQResponse.builder()
                    .status("SUCCESS")
                    .rfqNumber(req.getRfqNumber())
                    .message("RFQ created successfully")
                    .build();
        });

        when(rfqRepository.save(any(RFQEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenAnswer(inv -> {
            ExtractedRFQ extracted = inv.getArgument(0);
            List<RFQRequest.RfqItemDto> dtos = new ArrayList<>();
            if (extracted != null && extracted.getItems() != null) {
                for (RFQItem item : extracted.getItems()) {
                    dtos.add(RFQRequest.RfqItemDto.builder()
                            .description(item.getItemDescription())
                            .quantity(item.getQuantity())
                            .unitofMeasures(item.getUom())
                            .category(item.getCategory())
                            .build());
                }
            }
            return RFQRequest.builder()
                    .rfqNumber("RFQ-20260816174408")
                    .buyerEmail(extracted != null ? extracted.getBuyerEmail() : "buyer@test.com")
                    .deliveryDate(extracted != null ? extracted.getDeliveryDate() : null)
                    .clientdeliverylocationrfq(List.of(RFQRequest.LocationDto.builder().address(extracted != null ? extracted.getDeliveryLocation() : null).build()))
                    .rfqItem(dtos)
                    .build();
        });
    }

    @Test
    @DisplayName("TEST 1 & TEST 6 & TEST 7: 5 items email with same location/date -> ONE RFQ containing 5 items & quantities 20, 10, 50, 8, 5")
    void testSampleFiveItemEmailCreatesSingleRfqWithFiveItems() throws Exception {
        EmailData email = EmailData.builder()
                .messageId("MSG-FIVE-ITEMS")
                .subject("Requirement for Office Equipment & Machinery")
                .senderEmail("procurement@abccorp.com")
                .body("""
                        Please quote for the following 5 items:
                        1. Desktop Computer - Quantity: 20 Units
                        2. Office Printer - Quantity: 10 Units
                        3. Office Chair - Quantity: 50 Units
                        4. Air Conditioner - Quantity: 8 Units
                        5. Water Purifier - Quantity: 5 Units
                        
                        Delivery Location: ABC Corporate Office, Plot No. 25, 2nd Main Road, Whitefield Industrial Area, Bangalore, Karnataka - 560066, India
                        Delivery Date: 15-Dec-2027
                        """)
                .build();

        RFQItem i1 = RFQItem.builder().itemDescription("Desktop Computer").quantity(20.0).uom("Units").category("IT Hardware").deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Office Printer").quantity(10.0).uom("Units").category("Office Equipment").deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i3 = RFQItem.builder().itemDescription("Office Chair").quantity(50.0).uom("Units").category("Furniture").deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i4 = RFQItem.builder().itemDescription("Air Conditioner").quantity(8.0).uom("Units").category("HVAC").deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i5 = RFQItem.builder().itemDescription("Water Purifier").quantity(5.0).uom("Units").category("Water Treatment").deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("procurement@abccorp.com")
                .deliveryLocation("Bangalore")
                .deliveryDate("2027-12-15")
                .items(List.of(i1, i2, i3, i4, i5))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String status = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", status);

        // Verify exactly 1 RFQ is saved in database
        ArgumentCaptor<RFQEntity> entityCaptor = ArgumentCaptor.forClass(RFQEntity.class);
        verify(rfqRepository, times(1)).save(entityCaptor.capture());

        RFQEntity savedEntity = entityCaptor.getValue();
        assertEquals("RFQ-20260816174408", savedEntity.getRfqNumber());
        assertEquals("Bangalore", savedEntity.getDeliveryLocation());
        assertEquals("2027-12-15", savedEntity.getDeliveryDate());

        // Parse items JSON to verify 5 items exist in the single RFQ
        List<RFQItem> savedItems = objectMapper.readValue(savedEntity.getItemsJson(), new TypeReference<List<RFQItem>>() {});
        assertEquals(5, savedItems.size());
        assertEquals("Desktop Computer", savedItems.get(0).getItemDescription());
        assertEquals(20.0, savedItems.get(0).getQuantity());
        assertEquals("Office Printer", savedItems.get(1).getItemDescription());
        assertEquals(10.0, savedItems.get(1).getQuantity());
        assertEquals("Office Chair", savedItems.get(2).getItemDescription());
        assertEquals(50.0, savedItems.get(2).getQuantity());
        assertEquals("Air Conditioner", savedItems.get(3).getItemDescription());
        assertEquals(8.0, savedItems.get(3).getQuantity());
        assertEquals("Water Purifier", savedItems.get(4).getItemDescription());
        assertEquals(5.0, savedItems.get(4).getQuantity());

        // Verify ONE consolidated acknowledgement email is sent containing the RFQ number
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mailCaptor.capture());
        SimpleMailMessage sentMail = mailCaptor.getValue();
        assertEquals("procurement@abccorp.com", sentMail.getTo()[0]);
        assertTrue(sentMail.getText().contains("RFQ-20260816174408"));
    }

    @Test
    @DisplayName("TEST 2: 5 items + same location + different dates -> separate RFQs grouped by delivery date")
    void test2_FiveItemsSameLocationDifferentDates() {
        EmailData email = EmailData.builder().messageId("MSG-TEST2").senderEmail("procurement@abccorp.com").subject("Split Date").build();

        RFQItem i1 = RFQItem.builder().itemDescription("Item 1").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Item 2").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i3 = RFQItem.builder().itemDescription("Item 3").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-20").build();
        RFQItem i4 = RFQItem.builder().itemDescription("Item 4").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-20").build();
        RFQItem i5 = RFQItem.builder().itemDescription("Item 5").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-25").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("procurement@abccorp.com").items(List.of(i1, i2, i3, i4, i5)).build();
        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);

        // 3 distinct dates (15th, 20th, 25th) -> 3 RFQs
        verify(rfqRepository, times(3)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("TEST 3: 5 items + different locations + same date -> separate RFQs grouped by location")
    void test3_FiveItemsDifferentLocationsSameDate() {
        EmailData email = EmailData.builder().messageId("MSG-TEST3").senderEmail("procurement@abccorp.com").subject("Split Location").build();

        RFQItem i1 = RFQItem.builder().itemDescription("Item 1").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Item 2").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i3 = RFQItem.builder().itemDescription("Item 3").quantity(10.0).deliveryLocation("Hyderabad").deliveryDate("2027-12-15").build();
        RFQItem i4 = RFQItem.builder().itemDescription("Item 4").quantity(10.0).deliveryLocation("Chennai").deliveryDate("2027-12-15").build();
        RFQItem i5 = RFQItem.builder().itemDescription("Item 5").quantity(10.0).deliveryLocation("Chennai").deliveryDate("2027-12-15").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("procurement@abccorp.com").items(List.of(i1, i2, i3, i4, i5)).build();
        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);

        // 3 distinct locations (Bangalore, Hyderabad, Chennai) -> 3 RFQs
        verify(rfqRepository, times(3)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("TEST 4: 5 items + different locations + different dates -> separate RFQs for unique (location, date)")
    void test4_FiveItemsDifferentLocationsDifferentDates() {
        EmailData email = EmailData.builder().messageId("MSG-TEST4").senderEmail("procurement@abccorp.com").subject("Split Both").build();

        RFQItem i1 = RFQItem.builder().itemDescription("Item 1").quantity(10.0).deliveryLocation("Bangalore").deliveryDate("2027-12-15").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Item 2").quantity(10.0).deliveryLocation("Hyderabad").deliveryDate("2027-12-20").build();
        RFQItem i3 = RFQItem.builder().itemDescription("Item 3").quantity(10.0).deliveryLocation("Chennai").deliveryDate("2027-12-25").build();
        RFQItem i4 = RFQItem.builder().itemDescription("Item 4").quantity(10.0).deliveryLocation("Delhi").deliveryDate("2027-12-30").build();
        RFQItem i5 = RFQItem.builder().itemDescription("Item 5").quantity(10.0).deliveryLocation("Mumbai").deliveryDate("2028-01-05").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("procurement@abccorp.com").items(List.of(i1, i2, i3, i4, i5)).build();
        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);

        // 5 unique (location, date) pairs -> 5 RFQs
        verify(rfqRepository, times(5)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("3-ITEM EMAIL SCENARIO: 3 items with distinct brands & common location/date -> 1 RFQ, correct per-item brands & location")
    void testThreeItemEmailScenario() throws Exception {
        EmailData email = EmailData.builder()
                .messageId("MSG-THREE-ITEMS")
                .subject("Quotation Request for 3 Requirements")
                .senderEmail("procurement@abccorp.com")
                .body("""
                        Item 1 – Laptop
                        Product: Business Laptop
                        Quantity: 15 Units
                        Specifications: Intel Core i5, 16GB RAM, 512GB SSD, 14-inch display, Windows 11 Pro
                        Brand: Dell / HP / Lenovo
                        
                        Item 2 – Medical Equipment
                        Product: Multiparameter Patient Monitor
                        Quantity: 8 Units
                        Specifications: ECG, SpO2, NIBP, temperature monitoring, 12-inch display
                        Brand: Philips / GE Healthcare / Mindray
                        
                        Item 3 – Electrical Equipment
                        Product: Industrial LED Flood Light
                        Quantity: 50 Units
                        Specifications: 150W, IP66 protection, 6500K, high-efficiency LED, outdoor use
                        Brand: Philips / Havells / Bajaj
                        
                        Delivery Location:
                        ABC Procurement Warehouse
                        Plot No. 25, 2nd Main Road
                        Peenya Industrial Area, Phase 2
                        Bangalore, Karnataka – 560058
                        India
                        
                        Required Delivery Date: 25-Dec-2027
                        """)
                .build();

        RFQItem i1 = RFQItem.builder().itemDescription("Business Laptop").quantity(15.0).uom("Units").brand("Dell / HP / Lenovo").specification("Intel Core i5, 16GB RAM, 512GB SSD, 14-inch display, Windows 11 Pro").category("IT Hardware & Electronics").deliveryLocation("Bangalore, Karnataka - 560058").deliveryDate("2027-12-25").build();
        RFQItem i2 = RFQItem.builder().itemDescription("Multiparameter Patient Monitor").quantity(8.0).uom("Units").brand("Philips / GE Healthcare / Mindray").specification("ECG, SpO2, NIBP, temperature monitoring, 12-inch display").category("Medical Equipment").deliveryLocation("Bangalore, Karnataka - 560058").deliveryDate("2027-12-25").build();
        RFQItem i3 = RFQItem.builder().itemDescription("Industrial LED Flood Light").quantity(50.0).uom("Units").brand("Philips / Havells / Bajaj").specification("150W, IP66 protection, 6500K, high-efficiency LED, outdoor use").category("Lighting & Luminaires").deliveryLocation("Bangalore, Karnataka - 560058").deliveryDate("2027-12-25").build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("procurement@abccorp.com")
                .deliveryLocation("ABC Procurement Warehouse, Plot No. 25, 2nd Main Road, Peenya Industrial Area, Phase 2, Bangalore, Karnataka - 560058")
                .deliveryCity("Bangalore")
                .deliveryState("Karnataka")
                .deliveryPincode("560058")
                .deliveryDate("2027-12-25")
                .items(List.of(i1, i2, i3))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);

        // Verify exactly 1 RFQ entity is saved
        ArgumentCaptor<RFQEntity> entityCaptor = ArgumentCaptor.forClass(RFQEntity.class);
        verify(rfqRepository, times(1)).save(entityCaptor.capture());

        RFQEntity entity = entityCaptor.getValue();
        assertEquals("2027-12-25", entity.getDeliveryDate());

        // Parse items JSON to verify each item's brand is preserved independently
        List<RFQItem> savedItems = objectMapper.readValue(entity.getItemsJson(), new TypeReference<List<RFQItem>>() {});
        assertEquals(3, savedItems.size());

        assertEquals("Business Laptop", savedItems.get(0).getItemDescription());
        assertEquals("Dell / HP / Lenovo", savedItems.get(0).getBrand());
        assertEquals(15.0, savedItems.get(0).getQuantity());

        assertEquals("Multiparameter Patient Monitor", savedItems.get(1).getItemDescription());
        assertEquals("Philips / GE Healthcare / Mindray", savedItems.get(1).getBrand());
        assertEquals(8.0, savedItems.get(1).getQuantity());

        assertEquals("Industrial LED Flood Light", savedItems.get(2).getItemDescription());
        assertEquals("Philips / Havells / Bajaj", savedItems.get(2).getBrand());
        assertEquals(50.0, savedItems.get(2).getQuantity());
    }

    @Test
    @DisplayName("QUANTITY TEST 5: Email has no quantity -> RFQ NOT CREATED (no quantity = 1 fallback)")
    void testQuantityTest5_NoQuantityRfQNotCreated() {
        EmailData email = EmailData.builder().messageId("MSG-NO-QTY").senderEmail("procurement@abccorp.com").subject("No Qty").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Desktop Computer").quantity(null).build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("procurement@abccorp.com").items(List.of(i1)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-M5").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);
        RFQResponse mockResp = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-M5").build();
        when(rfqApiService.submitRFQ(mockRfqReq)).thenReturn(mockResp);
        when(rfqRepository.save(any(RFQEntity.class))).thenReturn(RFQEntity.builder().rfqNumber("RFQ-M5").buyerEmail("procurement@abccorp.com").build());

        String status = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("QUANTITY TEST 6: Quantity = 'seven' -> 7.0")
    void testQuantityTest6_SevenNormalized() {
        Double qty = QuantityNormalizer.normalize("seven");
        assertNotNull(qty);
        assertEquals(7.0, qty);
    }

    @Test
    @DisplayName("QUANTITY TEST 7: Quantity = 'ten' -> 10.0")
    void testQuantityTest7_TenNormalized() {
        Double qty = QuantityNormalizer.normalize("ten");
        assertNotNull(qty);
        assertEquals(10.0, qty);
    }

    @Test
    @DisplayName("QUANTITY TEST 8: Quantity = '1,000' -> 1000.0")
    void testQuantityTest8_FormattedThousandNormalized() {
        Double qty = QuantityNormalizer.normalize("1,000");
        assertNotNull(qty);
        assertEquals(1000.0, qty);
    }

    @Test
    @DisplayName("QUANTITY TEST 9: Quantity = 'two thousand' -> 2000.0")
    void testQuantityTest9_TwoThousandNormalized() {
        Double qty = QuantityNormalizer.normalize("two thousand");
        assertNotNull(qty);
        assertEquals(2000.0, qty);
    }

    @Test
    @DisplayName("QUANTITY TEST 10: Quantity = 0 -> defaulted to 1.0")
    void testQuantityTest10_ZeroQuantityRfqNotCreated() {
        EmailData email = EmailData.builder().messageId("MSG-ZERO-QTY").senderEmail("procurement@abccorp.com").subject("Zero Qty").build();
        RFQItem i1 = RFQItem.builder().itemDescription("Desktop Computer").quantity(0.0).build();
        ExtractedRFQ extracted = ExtractedRFQ.builder().buyerEmail("procurement@abccorp.com").items(List.of(i1)).build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-M10").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);
        RFQResponse mockResp = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-M10").build();
        when(rfqApiService.submitRFQ(mockRfqReq)).thenReturn(mockResp);
        when(rfqRepository.save(any(RFQEntity.class))).thenReturn(RFQEntity.builder().rfqNumber("RFQ-M10").buyerEmail("procurement@abccorp.com").build());

        String status = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("EXAMPLE 1: Water Treatment System with 10,000 LPH capacity -> quantity defaulted to 1.0 and RFQ created")
    void testExample1_WaterTreatmentSystemSpecificationCapacityNoQuantity() {
        EmailData email = EmailData.builder()
                .messageId("MSG-WATER-TREATMENT-SPEC-ONLY")
                .subject("Quotation Request for Industrial Water Treatment System")
                .senderEmail("procurement@abccorp.com")
                .body("""
                        Product: Industrial Water Treatment System
                        Specifications: 10,000 liters per hour capacity, RO + UV filtration, automatic control panel
                        Brand: Thermax / Ion Exchange / Pentair
                        Delivery Location: ABC Manufacturing Plant, Bangalore, Karnataka - 560058
                        Required Delivery Date: 20-Dec-2027
                        """)
                .build();

        RFQItem i1 = RFQItem.builder()
                .itemDescription("Industrial Water Treatment System")
                .specification("10,000 liters per hour capacity, RO + UV filtration")
                .brand("Thermax / Ion Exchange / Pentair")
                .quantity(null)
                .deliveryLocation("Bangalore, Karnataka - 560058")
                .deliveryDate("2027-12-20")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("procurement@abccorp.com")
                .items(List.of(i1))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-WT").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);
        RFQResponse mockResp = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-WT").build();
        when(rfqApiService.submitRFQ(mockRfqReq)).thenReturn(mockResp);
        when(rfqRepository.save(any(RFQEntity.class))).thenReturn(RFQEntity.builder().rfqNumber("RFQ-WT").buyerEmail("procurement@abccorp.com").build());

        String status = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("USER TEST EMAIL: Industrial Welding Machine (400A) with no purchase quantity -> defaulted to 1.0 and RFQ created")
    void testIndustrialWeldingMachineNoQuantityRfqNotCreated() {
        EmailData email = EmailData.builder()
                .messageId("MSG-WELDING-MACHINE-NO-QTY")
                .subject("Request for Quotation – Industrial Welding Machine")
                .senderEmail("veerababuv2002@gmail.com")
                .body("""
                        Dear Procurement Team,
                        
                        Please provide your best quotation for the following requirement:
                        
                        Product: Industrial Welding Machine
                        Specifications: 400A inverter welding machine, three-phase, digital display, suitable for heavy-duty fabrication work
                        Brand: ESAB / Lincoln Electric / Ador
                        Delivery Location: ABC Engineering Works, Peenya Industrial Area, Bangalore, Karnataka – 560058, India
                        Required Delivery Date: 30-Dec-2027
                        
                        Please include transportation, installation, applicable taxes, warranty, and delivery terms.
                        
                        Regards,
                        Veera Babu
                        """)
                .build();

        RFQItem i1 = RFQItem.builder()
                .itemDescription("Industrial Welding Machine")
                .specification("400A inverter welding machine, three-phase, digital display, suitable for heavy-duty fabrication work")
                .brand("ESAB / Lincoln Electric / Ador")
                .quantity(1.0)
                .deliveryLocation("ABC Engineering Works, Peenya Industrial Area, Bangalore, Karnataka - 560058")
                .deliveryDate("2027-12-30")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("veerababuv2002@gmail.com")
                .items(List.of(i1))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);
        RFQRequest mockRfqReq = RFQRequest.builder().rfqNumber("RFQ-WELD").build();
        when(rfqBuilderService.buildRFQRequest(any(), any(), any(), any())).thenReturn(mockRfqReq);
        RFQResponse mockResp = RFQResponse.builder().status("SUCCESS").rfqNumber("RFQ-WELD").build();
        when(rfqApiService.submitRFQ(mockRfqReq)).thenReturn(mockResp);
        when(rfqRepository.save(any(RFQEntity.class))).thenReturn(RFQEntity.builder().rfqNumber("RFQ-WELD").buyerEmail("veerababuv2002@gmail.com").build());

        String status = emailProcessorService.processSingleEmail(email);

        assertEquals("RFQ_CREATED", status);
        verify(rfqRepository, times(1)).save(any(RFQEntity.class));
    }

    @Test
    @DisplayName("2-ITEM EMAIL: Different location & different date -> 2 RFQs created, 1 consolidated email with both RFQ numbers")
    void testTwoItemsDifferentLocationsAndDatesSplitIntoTwoRfqsWithConsolidatedAcknowledgement() throws Exception {
        EmailData email = EmailData.builder()
                .messageId("MSG-TWO-ITEMS-DIFF-LOC-DATE")
                .subject("Quotation Request for IT Equipment and Plumbing Material")
                .senderEmail("veerababuv2002@gmail.com")
                .body("""
                        Item 1 – IT Equipment
                        Product: Network Server
                        Quantity: 4 Units
                        Specifications: Intel Xeon processor, 64GB RAM, 2TB SSD, RAID support, rack-mountable
                        Brand: Dell / HP / Lenovo
                        Delivery Location: ABC Technology Center, Whitefield, Bangalore, Karnataka – 560066, India
                        Required Delivery Date: 25-Feb-2028
                        
                        Item 2 – Plumbing Material
                        Product: HDPE Water Pipe
                        Quantity: 3,000 Meters
                        Specifications: 160 mm diameter, PN10 pressure rating, UV resistant, suitable for underground water supply
                        Brand: Supreme / Astral / Finolex
                        Delivery Location: ABC Infrastructure Project, Gachibowli, Hyderabad, Telangana – 500032, India
                        Required Delivery Date: 05-Mar-2028
                        """)
                .build();

        RFQItem i1 = RFQItem.builder()
                .itemDescription("Network Server")
                .quantity(4.0)
                .uom("Units")
                .brand("Dell / HP / Lenovo")
                .specification("Intel Xeon processor, 64GB RAM, 2TB SSD, RAID support, rack-mountable")
                .category("IT Equipment")
                .deliveryLocation("ABC Technology Center, Whitefield, Bangalore, Karnataka - 560066")
                .deliveryDate("2028-02-25")
                .build();

        RFQItem i2 = RFQItem.builder()
                .itemDescription("HDPE Water Pipe")
                .quantity(3000.0)
                .uom("Meters")
                .brand("Supreme / Astral / Finolex")
                .specification("160 mm diameter, PN10 pressure rating, UV resistant, suitable for underground water supply")
                .category("Plumbing Material")
                .deliveryLocation("ABC Infrastructure Project, Gachibowli, Hyderabad, Telangana - 500032")
                .deliveryDate("2028-03-05")
                .build();

        ExtractedRFQ extracted = ExtractedRFQ.builder()
                .buyerEmail("veerababuv2002@gmail.com")
                .items(List.of(i1, i2))
                .build();

        when(aiExtractionService.extractRFQFromEmail(email)).thenReturn(extracted);

        String status = emailProcessorService.processSingleEmail(email);
        assertEquals("RFQ_CREATED", status);

        // Verify exactly 2 RFQ entities are saved
        ArgumentCaptor<RFQEntity> entityCaptor = ArgumentCaptor.forClass(RFQEntity.class);
        verify(rfqRepository, times(2)).save(entityCaptor.capture());

        List<RFQEntity> savedRfqs = entityCaptor.getAllValues();
        assertEquals(2, savedRfqs.size());

        // Verify RFQ 1 (Bangalore, 2028-02-25)
        RFQEntity rfq1 = savedRfqs.get(0);
        assertEquals("2028-02-25", rfq1.getDeliveryDate());
        assertTrue(rfq1.getDeliveryLocation().contains("Bangalore"));

        // Verify RFQ 2 (Hyderabad, 2028-03-05)
        RFQEntity rfq2 = savedRfqs.get(1);
        assertEquals("2028-03-05", rfq2.getDeliveryDate());
        assertTrue(rfq2.getDeliveryLocation().contains("Hyderabad"));

        // Verify 1 consolidated acknowledgement email is sent containing both RFQ numbers
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        SimpleMailMessage sentMail = mailCaptor.getValue();

        assertEquals("🚀 Your RFQs are Live — Suppliers Notified!", sentMail.getSubject());
        String bodyText = sentMail.getText();
        assertTrue(bodyText.contains("RFQs created:"));
    }
}
