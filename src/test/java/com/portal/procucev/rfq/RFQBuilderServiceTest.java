package com.portal.procucev.rfq;

import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.model.RFQItem;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.service.RFQBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

public class RFQBuilderServiceTest {

    @TempDir
    Path tempDir;

    private DateParser dateParser;
    private PincodeDao pincodeDao;
    private RFQBuilderService rfqBuilderService;

    @BeforeEach
    void setUp() {
        dateParser = new DateParser();
        pincodeDao = Mockito.mock(PincodeDao.class);
        rfqBuilderService = new RFQBuilderService(dateParser, pincodeDao);
    }

    @Test
    @DisplayName("Test buildRFQRequest with single item and explicit pincode location")
    void testBuildRFQRequestSingleItem() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Bangalore, 560001, Karnataka")
                .deliveryDate("2026-08-25")
                .items(List.of(
                        RFQItem.builder()
                                .itemDescription("Dell Laptop")
                                .quantity(1.0)
                                .uom("NOS")
                                .brand("Dell")
                                .partCode("P123")
                                .specification("16GB RAM")
                                .category("IT Hardware")
                                .build()
                ))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John Doe")
                .email("john@test.com")
                .orgId("10")
                .userId("20")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Subject", null);

        assertNotNull(req);
        assertTrue(req.getRfqNumber().startsWith("RFQ-"));
        assertEquals("John Doe", req.getCreatedBy());
        assertEquals("Dell Laptop", req.getProjectDesc());
        assertEquals("2026-08-25", req.getDeliveryDate());
        assertEquals("10", req.getOrg().getId());
        assertEquals("20", req.getUser());

        assertEquals(1, req.getClientdeliverylocationrfq().size());
        assertEquals("Bangalore", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Karnataka", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("560001", req.getClientdeliverylocationrfq().get(0).getPincode());

        assertEquals(1, req.getRfqItem().size());
        RFQRequest.RfqItemDto itemDto = req.getRfqItem().get(0);
        // rfq_items.brand is presented as "Specification" and rfq_items.remarks as "Remarks".
        assertEquals("P/N: P123, 16GB RAM", itemDto.getBrand());
        assertEquals("NOS", itemDto.getUnitofMeasures());
        assertEquals(1.0, itemDto.getQuantity());
        assertEquals("Dell Laptop", itemDto.getDescription());
        assertEquals("IT Hardware", itemDto.getCategory());
        assertEquals("P123", itemDto.getItemcode());
        assertEquals("Brand: Dell", itemDto.getRemarks());
    }

    @Test
    @DisplayName("Test buildRFQRequest with multiple items and pincodeDao lookup")
    void testBuildRFQRequestMultipleItemsWithPincodeDaoLookup() {
        PincodeData pinData = new PincodeData();
        pinData.setCity("Hyderabad");
        pinData.setState("Telangana");
        pinData.setPincode("500001");
        Mockito.when(pincodeDao.findByCityIgnoreCase(eq("hyderabad"))).thenReturn(pinData);

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryCity("hyderabad")
                .items(List.of(
                        RFQItem.builder().itemDescription("Computer Monitor").quantity(2.5).uom("NOS").remarks("Remark 1").build(),
                        RFQItem.builder().itemDescription("Mechanical Keyboard").quantity(1.0).uom("NOS").brand("Logitech").build()
                ))
                .build();

        Buyer buyer = Buyer.builder().name("Jane").email("jane@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Subject", List.of());

        assertNotNull(req);
        assertEquals("Computer Monitors", req.getProjectDesc());
        assertEquals(2, req.getRfqItem().size());
        assertEquals(2.5, req.getRfqItem().get(0).getQuantity());
        assertEquals(1.0, req.getRfqItem().get(1).getQuantity());
        assertEquals("Brand: Logitech", req.getRfqItem().get(1).getRemarks());
        assertEquals("Mechanical Keyboard", req.getRfqItem().get(1).getBrand());
    }

    @Test
    @DisplayName("Test buildRFQRequest with attachments Base64 encoding")
    void testBuildRFQRequestWithAttachments() throws Exception {
        File attachFile = new File(tempDir.toFile(), "test_attachment.txt");
        try (FileOutputStream fos = new FileOutputStream(attachFile)) {
            fos.write("Attachment Content".getBytes());
        }

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Subject", List.of(attachFile));

        assertEquals(1, req.getRfqDocument().size());
        assertEquals("test_attachment.txt", req.getRfqDocument().get(0).get("fileName"));
        assertNotNull(req.getRfqDocument().get(0).get("file"));
    }

    @Test
    @DisplayName("Test buildRFQRequest with null buyer location and null item fields")
    void testBuildRFQRequestNullLocationAndFields() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).uom(null).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, null, null);
        assertNotNull(req);
        assertEquals(1, req.getClientdeliverylocationrfq().size());
        assertEquals("Registered Profile Address", req.getClientdeliverylocationrfq().get(0).getAddress());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("Test buildRFQRequest with pincode in location text extracts pincode without hallucinating city/state")
    void testBuildRFQRequestBangalorePincode() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Office in 560037 area")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("560037", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("Test buildRFQRequest with state detection from location text")
    void testBuildRFQRequestStateDetection() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Mumbai, Maharashtra")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Maharashtra", req.getClientdeliverylocationrfq().get(0).getState());
    }

    @Test
    @DisplayName("Test buildRFQRequest with pincode only keeps city and state unspecified")
    void testBuildRFQRequestPincodeOnly() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryPincode("500001")
                .deliveryLocation("500001")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("500001", req.getClientdeliverylocationrfq().get(0).getPincode());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getState());
    }

    @Test
    @DisplayName("Test buildRFQRequest with buyer address fallback when location is empty")
    void testBuildRFQRequestBuyerAddressFallback() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("User").email("user@test.com")
                .address("123 Street").city("Chennai").state("Tamil Nadu").pincode("600001")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("123 Street", req.getClientdeliverylocationrfq().get(0).getAddress());
        assertEquals("Chennai", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Tamil Nadu", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("600001", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("Test buildRFQRequest with multiple items and various brand edge cases")
    void testBuildRFQRequestBrandEdgeCases() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("Item1").quantity(1.0).brand("null").build(),
                        RFQItem.builder().itemDescription("Item2").quantity(1.0).brand("Not Specified").build(),
                        RFQItem.builder().itemDescription("Item3").quantity(1.0).brand("Brand: Already Prefixed").build(),
                        RFQItem.builder().itemDescription("Item4").quantity(5.0).build()
                ))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        // Brand normalisation now lands in remarks, which is presented as "Remarks".
        assertEquals("Brand: Not Specified", req.getRfqItem().get(0).getRemarks());
        assertEquals("Brand: Not Specified", req.getRfqItem().get(1).getRemarks());
        assertEquals("Brand: Already Prefixed", req.getRfqItem().get(2).getRemarks());
        // Specification falls back to the description and must never contain the brand.
        assertEquals("Item1", req.getRfqItem().get(0).getBrand());
        assertEquals("Item3", req.getRfqItem().get(2).getBrand());
        assertEquals(5.0, req.getRfqItem().get(3).getQuantity());
    }

    @Test
    @DisplayName("Test buildRFQRequest with multiple items spec fallbacks and null orgId/userId")
    void testBuildRFQRequestSpecsFallbacks() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("Item1").quantity(1.0).specification("Spec1").build(),
                        RFQItem.builder().itemDescription("Item2").quantity(1.0).remarks("Remark2").build(),
                        RFQItem.builder().itemDescription("Item3").quantity(1.0).build()
                ))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").orgId(null).userId(null).build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Spec1", req.getRfqItem().get(0).getBrand());
        assertEquals("Remark2", req.getRfqItem().get(1).getBrand());
        assertEquals("1", req.getOrg().getId());
        assertEquals("1", req.getUser());
    }

    @Test
    @DisplayName("Test buildRFQRequest with single item spec fallback")
    void testBuildRFQRequestSingleItemSpecFallback() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).remarks("SingleRemark").build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("SingleRemark", req.getRfqItem().get(0).getBrand());
    }

    @Test
    @DisplayName("Test buildRFQRequest with multiple items and blank first description")
    void testBuildRFQRequestMultipleItemsBlankFirstDesc() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("").quantity(1.0).build(),
                        RFQItem.builder().itemDescription("Item2").quantity(1.0).build()
                ))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Procurement Items", req.getProjectDesc());
    }

    @Test
    @DisplayName("Test buildRFQRequest with pincodeDao exception handling")
    void testBuildRFQRequestPincodeDaoException() {
        Mockito.when(pincodeDao.findByCityIgnoreCase("raipur")).thenThrow(new RuntimeException("DB error"));

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Raipur, Chhattisgarh")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertNotNull(req);
        assertEquals("Chhattisgarh", req.getClientdeliverylocationrfq().get(0).getState());
    }

    @Test
    @DisplayName("Test buildRFQRequest with various state detections")
    void testBuildRFQRequestVariousStates() {
        // Telangana
        testStateDetection("Hyderabad, Telangana", "Telangana");
        testStateDetection("Vijayawada, Andhra Pradesh", "Andhra Pradesh");
        testStateDetection("Chennai, Tamil Nadu", "Tamil Nadu");
        testStateDetection("New Delhi, Delhi", "Delhi");
        testStateDetection("Ahmedabad, Gujarat", "Gujarat");
        testStateDetection("Kolkata, West Bengal", "West Bengal");
        testStateDetection("Jaipur, Rajasthan", "Rajasthan");
        testStateDetection("Bhopal, Madhya Pradesh", "Madhya Pradesh");
        testStateDetection("Noida, Uttar Pradesh", "Uttar Pradesh");
        testStateDetection("Kochi, Kerala", "Kerala");
        testStateDetection("Ludhiana, Punjab", "Punjab");
        testStateDetection("Gurgaon, Haryana", "Haryana");
        testStateDetection("Patna, Bihar", "Bihar");
        testStateDetection("Bhubaneswar, Odisha", "Odisha");
        testStateDetection("Guwahati, Assam", "Assam");
        testStateDetection("Ranchi, Jharkhand", "Jharkhand");
    }

    @Test
    @DisplayName("Test sanitizeText with typographic characters transliteration")
    void testSanitizeTextTypographicChars() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder()
                        .itemDescription("O-Ring 40\u00D752\u00D77 \u2715 \u2716 \u2018single\u2019 \u201Cdouble\u201D \u00B5m \u2032 \u2033")
                        .specification("Dimension \u2013 \u2014 100\u00A0mm")
                        .quantity(1.0)
                        .build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();
        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);

        assertNotNull(req);
        assertTrue(req.getRfqItem().get(0).getDescription().contains("40x52x7"));
        assertTrue(req.getRfqItem().get(0).getBrand().contains("100 mm"));
    }

    private void testStateDetection(String location, String expectedState) {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation(location)
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();
        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals(expectedState, req.getClientdeliverylocationrfq().get(0).getState());
    }

    @Test
    @DisplayName("Test buildRFQRequest with null attachment file in list")
    void testBuildRFQRequestNullAttachmentFile() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        File nullFile = null;
        File emptyFile = new File(tempDir.toFile(), "empty.txt");
        // emptyFile doesn't exist, so it should be skipped

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", List.of(emptyFile));
        assertTrue(req.getRfqDocument().isEmpty());
    }

    @Test
    @DisplayName("Test buildRFQRequest with delivery city/state/pincode from extracted RFQ fields")
    void testBuildRFQRequestExplicitCityStatePincode() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryCity("CustomCity")
                .deliveryState("CustomState")
                .deliveryPincode("999999")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("CustomCity", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("CustomState", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("999999", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("Test buildRFQRequest single item with null description uses 'RFQ Requirement'")
    void testBuildRFQRequestSingleItemNullDesc() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("RFQ Requirement", req.getProjectDesc());
    }

    @Test
    @DisplayName("STRICT LOCATION RULE 1: No location -> complete buyer default location")
    void testLocationRule1_NoLocation_CompleteBuyerDefaultLocation() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Not Specified")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(5.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John").email("john@test.com")
                .city("Kakinada").state("Andhra Pradesh").pincode("533431").address("Main Street")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Kakinada", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Andhra Pradesh", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("533431", req.getClientdeliverylocationrfq().get(0).getPincode());
        assertEquals("Main Street", req.getClientdeliverylocationrfq().get(0).getAddress());
    }

    @Test
    @DisplayName("STRICT LOCATION RULE 2: Only city -> city only; no buyer-default state/pincode")
    void testLocationRule2_OnlyCity_NoBuyerDefaultStateOrPincode() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryCity("Whitefield")
                .deliveryLocation("Whitefield")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(5.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John").email("john@test.com")
                .city("Kakinada").state("Andhra Pradesh").pincode("533431")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Whitefield", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("STRICT LOCATION RULE 3: Only state -> state only; no buyer-default city/pincode")
    void testLocationRule3_OnlyState_NoBuyerDefaultCityOrPincode() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryState("Karnataka")
                .deliveryLocation("Karnataka")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(5.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John").email("john@test.com")
                .city("Kakinada").state("Andhra Pradesh").pincode("533431")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Karnataka", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("STRICT LOCATION RULE 4: Only pincode -> pincode only; no buyer-default city/state")
    void testLocationRule4_OnlyPincode_NoBuyerDefaultCityOrState() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryPincode("560066")
                .deliveryLocation("560066")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(5.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John").email("john@test.com")
                .city("Kakinada").state("Andhra Pradesh").pincode("533431")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("560066", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("STRICT LOCATION RULE 5: City + state -> preserve both; pincode remains unspecified")
    void testLocationRule5_CityAndState_PincodeUnspecified() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryCity("Whitefield")
                .deliveryState("Karnataka")
                .deliveryLocation("Whitefield, Karnataka")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(5.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John").email("john@test.com")
                .city("Kakinada").state("Andhra Pradesh").pincode("533431")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Whitefield", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Karnataka", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("STRICT LOCATION RULE 6: Complete location -> use email location")
    void testLocationRule6_CompleteLocation_UseEmailLocation() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Whitefield Industrial Area, Bangalore, Karnataka - 560066")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(5.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John").email("john@test.com")
                .city("Kakinada").state("Andhra Pradesh").pincode("533431")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Bangalore", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Karnataka", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("560066", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("STRICT LOCATION RULE 7: Email location different from buyer default -> email location wins")
    void testLocationRule7_EmailLocationDifferentFromBuyerDefault_EmailWins() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Peenya, Bangalore, Karnataka - 560058")
                .items(List.of(RFQItem.builder().itemDescription("Laptop").quantity(5.0).build()))
                .build();

        Buyer buyer = Buyer.builder()
                .name("John").email("john@test.com")
                .city("Kakinada").state("Andhra Pradesh").pincode("533431")
                .build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Bangalore", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Karnataka", req.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("560058", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("Test state detection branches for all Indian states in RFQBuilderService")
    void testAllIndianStatesDetection() {
        String[] stateKeywords = {
                "chhattisgarh", "karnataka", "telangana", "andhra", "maharashtra",
                "tamil nadu", "tamilnadu", "delhi", "gujarat", "west bengal", "bengal",
                "rajasthan", "madhya pradesh", "uttar pradesh", "kerala", "punjab",
                "haryana", "bihar", "odisha", "assam", "jharkhand"
        };

        for (String kw : stateKeywords) {
            ExtractedRFQ rfq = ExtractedRFQ.builder()
                    .deliveryLocation("Plant site in " + kw + ", 123456")
                    .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                    .build();

            Buyer buyer = Buyer.builder().name("User").email("u@test.com").build();
            RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
            assertFalse(req.getClientdeliverylocationrfq().get(0).getState().isBlank());
        }
    }

    @Test
    @DisplayName("Test sanitizeText transliterations and length caps")
    void testSanitizeTextAndLengthCaps() {
        String inputWithAllChars = "Size: 40\u00D752\u00D77 \u2715 \u2716 \u00A0 \u2013 \u2014 \u2018single\u2019 \u201Cdouble\u201D \u00B5m \u2032 \u2033";
        String longText250 = "A".repeat(250);

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder()
                                .itemDescription(longText250)
                                .brand("Brand: " + longText250)
                                .specification(inputWithAllChars + " " + longText250)
                                .partCode("PC999")
                                .quantity(1.0)
                                .build()
                ))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("u@test.com").build();
        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);

        assertNotNull(req);
        assertTrue(req.getProjectDesc().length() <= 100);
        assertTrue(req.getRfqItem().get(0).getRemarks().length() <= 50);
        assertTrue(req.getRfqItem().get(0).getBrand().length() <= 200);
    }

    @Test
    @DisplayName("Test null items, blank descriptions, null buyer, and token split edge cases")
    void testNullItemsAndEdgeCases() throws Exception {
        // 1. null items and null buyer
        ExtractedRFQ rfqNullItems = ExtractedRFQ.builder().build();
        RFQRequest req1 = rfqBuilderService.buildRFQRequest(rfqNullItems, null, "Sub", null);
        assertEquals("RFQ Requirement", req1.getProjectDesc());
        assertEquals("User", req1.getCreatedBy());
        assertEquals("1", req1.getOrg().getId());
        assertEquals("1", req1.getUser());
        assertTrue(req1.getRfqItem().isEmpty());

        // 2. empty items
        ExtractedRFQ rfqEmptyItems = ExtractedRFQ.builder().items(List.of()).build();
        RFQRequest req2 = rfqBuilderService.buildRFQRequest(rfqEmptyItems, null, "Sub", null);
        assertEquals("RFQ Requirement", req2.getProjectDesc());

        // 3. multiple items with blank first item description
        RFQItem blankItem1 = RFQItem.builder().itemDescription("").quantity(1.0).build();
        RFQItem blankItem2 = RFQItem.builder().itemDescription("Item 2").quantity(2.0).build();
        ExtractedRFQ rfqMultiBlank = ExtractedRFQ.builder().items(List.of(blankItem1, blankItem2)).build();
        RFQRequest req3 = rfqBuilderService.buildRFQRequest(rfqMultiBlank, Buyer.builder().name("Buyer").build(), "Sub", null);
        assertEquals("Procurement Items", req3.getProjectDesc());

        // 4. single item with blank description
        ExtractedRFQ rfqSingleBlank = ExtractedRFQ.builder().items(List.of(blankItem1)).build();
        RFQRequest req4 = rfqBuilderService.buildRFQRequest(rfqSingleBlank, Buyer.builder().name("Buyer").build(), "Sub", null);
        assertEquals("RFQ Requirement", req4.getProjectDesc());

        // 5. sanitizeText(null)
        assertEquals("", org.springframework.test.util.ReflectionTestUtils.invokeMethod(rfqBuilderService, "sanitizeText", (String) null));

        // 6. partCode already inside specs
        RFQItem itemWithPartInSpec = RFQItem.builder()
                .itemDescription("Pump")
                .partCode("P123")
                .specification("P123 high pressure")
                .quantity(1.0)
                .build();
        ExtractedRFQ rfqPartInSpec = ExtractedRFQ.builder().items(List.of(itemWithPartInSpec)).build();
        RFQRequest req5 = rfqBuilderService.buildRFQRequest(rfqPartInSpec, Buyer.builder().name("Buyer").build(), "Sub", null);
        assertEquals("P123 high pressure", req5.getRfqItem().get(0).getBrand());

        // 7. locStr token is state name or digits
        ExtractedRFQ rfqStateToken = ExtractedRFQ.builder()
                .deliveryLocation("Karnataka, Bangalore")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest req6 = rfqBuilderService.buildRFQRequest(rfqStateToken, Buyer.builder().name("Buyer").build(), "Sub", null);
        assertEquals("Karnataka", req6.getClientdeliverylocationrfq().get(0).getState());

        ExtractedRFQ rfqDigitToken = ExtractedRFQ.builder()
                .deliveryLocation("560001, Bangalore")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest req7 = rfqBuilderService.buildRFQRequest(rfqDigitToken, Buyer.builder().name("Buyer").build(), "Sub", null);
        assertEquals("560001", req7.getClientdeliverylocationrfq().get(0).getPincode());

        // 8. Test all remaining state extraction branches
        String[] stateKeywords = {
                "chhattisgarh", "telangana", "andhra", "maharashtra", "delhi", "gujarat",
                "west bengal", "rajasthan", "madhya pradesh", "uttar pradesh", "kerala",
                "punjab", "haryana", "bihar", "odisha", "assam", "jharkhand"
        };
        for (String stateKw : stateKeywords) {
            ExtractedRFQ rfqState = ExtractedRFQ.builder()
                    .deliveryLocation("Industrial Zone, " + stateKw)
                    .items(List.of(RFQItem.builder().itemDescription("Part").quantity(1.0).build()))
                    .build();
            RFQRequest reqState = rfqBuilderService.buildRFQRequest(rfqState, Buyer.builder().name("Buyer").build(), "Sub", null);
            assertFalse(reqState.getClientdeliverylocationrfq().get(0).getState().isBlank());
        }

        // 9. Item field edge cases: qty <= 0, uom blank
        RFQItem customItem = RFQItem.builder()
                .itemDescription("Bolts")
                .quantity(0.0)
                .uom("   ")
                .category("Fasteners")
                .build();
        ExtractedRFQ rfqCustom = ExtractedRFQ.builder().items(List.of(customItem)).build();
        RFQRequest reqCustom = rfqBuilderService.buildRFQRequest(rfqCustom, Buyer.builder().name("Buyer").build(), "Sub", null);
        assertEquals(1.0, reqCustom.getRfqItem().get(0).getQuantity());
        assertEquals("Nos", reqCustom.getRfqItem().get(0).getUnitofMeasures());
        assertEquals("Fasteners", reqCustom.getRfqItem().get(0).getCategory());

        // 10. null / empty items list
        ExtractedRFQ rfqNullItemsList = ExtractedRFQ.builder().items(null).build();
        RFQRequest reqNullItems = rfqBuilderService.buildRFQRequest(rfqNullItemsList, null, "Sub", null);
        assertEquals("RFQ Requirement", reqNullItems.getProjectDesc());
        assertTrue(reqNullItems.getRfqItem().isEmpty());

        // 11. long description (>100 chars) & long specs (>200 chars) & long brand (>50 chars)
        String longDesc = "A".repeat(120);
        String longSpec = "B".repeat(250);
        String longBrand = "C".repeat(60);
        RFQItem longItem = RFQItem.builder()
                .itemDescription(longDesc)
                .specification(longSpec)
                .brand(longBrand)
                .partCode("PART-99")
                .uom("MTR")
                .quantity(5.0)
                .build();
        ExtractedRFQ rfqLong = ExtractedRFQ.builder()
                .items(List.of(longItem))
                .deliveryCity("Not Specified")
                .deliveryState("Not Specified")
                .deliveryPincode("Not Specified")
                .deliveryLocation("Not Specified")
                .build();
        Buyer fullBuyer = Buyer.builder()
                .name("Full Buyer")
                .city("Chennai")
                .state("Tamil Nadu")
                .pincode("600001")
                .address("100 Anna Salai")
                .orgId("200")
                .userId("300")
                .email("buyer@org.com")
                .token("tok123")
                .build();
        RFQRequest reqLong = rfqBuilderService.buildRFQRequest(rfqLong, fullBuyer, "Sub", null);
        assertEquals(100, reqLong.getProjectDesc().length());
        assertEquals("Chennai", reqLong.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Tamil Nadu", reqLong.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("600001", reqLong.getClientdeliverylocationrfq().get(0).getPincode());
        assertEquals("100 Anna Salai", reqLong.getClientdeliverylocationrfq().get(0).getAddress());
        assertTrue(reqLong.getRfqItem().get(0).getRemarks().startsWith("Brand: "));

        // 12. Location fallback when address is blank or "Not Specified" and buyer is null
        ExtractedRFQ rfqBlankLoc = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .deliveryLocation("N/A")
                .build();
        RFQRequest reqBlankLoc = rfqBuilderService.buildRFQRequest(rfqBlankLoc, null, "Sub", null);
        assertEquals("Registered Profile Address", reqBlankLoc.getClientdeliverylocationrfq().get(0).getAddress());

        // 13. Brand variants: "null", "Not Specified", "Brand: Already"
        RFQItem bNull = RFQItem.builder().itemDescription("Item 1").quantity(1.0).brand("null").build();
        RFQItem bNotSpec = RFQItem.builder().itemDescription("Item 2").quantity(1.0).brand("Not Specified").build();
        RFQItem bAlready = RFQItem.builder().itemDescription("Item 3").quantity(1.0).brand("Brand: Siemens").build();
        ExtractedRFQ rfqBrands = ExtractedRFQ.builder().items(List.of(bNull, bNotSpec, bAlready)).build();
        RFQRequest reqBrands = rfqBuilderService.buildRFQRequest(rfqBrands, fullBuyer, "Sub", null);
        assertEquals("Brand: Not Specified", reqBrands.getRfqItem().get(0).getRemarks());
        assertEquals("Brand: Not Specified", reqBrands.getRfqItem().get(1).getRemarks());
        assertEquals("Brand: Siemens", reqBrands.getRfqItem().get(2).getRemarks());

        // 14. Spec fallback to cleanItemDesc and partCode inclusion
        RFQItem itemNoSpec = RFQItem.builder()
                .itemDescription("Butterfly Valve")
                .quantity(2.0)
                .partCode("BFV-50")
                .build();
        ExtractedRFQ rfqNoSpec = ExtractedRFQ.builder().items(List.of(itemNoSpec)).build();
        RFQRequest reqNoSpec = rfqBuilderService.buildRFQRequest(rfqNoSpec, fullBuyer, "Sub", null);
        assertTrue(reqNoSpec.getRfqItem().get(0).getBrand().contains("P/N: BFV-50"));

        // 15. Attachment handling with null / empty / missing files
        File missingFile = new File("missing_file_xyz_123.pdf");
        File emptyFile = File.createTempFile("empty", ".tmp");
        emptyFile.deleteOnExit();
        rfqBuilderService.buildRFQRequest(rfqBrands, fullBuyer, "Sub", List.of(missingFile, emptyFile));

        // 16. Valid attachment file encoding
        File validDoc = File.createTempFile("rfq_spec", ".txt");
        java.nio.file.Files.writeString(validDoc.toPath(), "Attachment Spec Document");
        RFQRequest reqDoc = rfqBuilderService.buildRFQRequest(rfqBrands, fullBuyer, "Sub", List.of(validDoc));
        assertNotNull(reqDoc.getRfqDocument());
        assertFalse(reqDoc.getRfqDocument().isEmpty());
        validDoc.delete();

        // 17. Delivery dates in various formats
        String[] dates = {"2026-11-30", "15/12/2026", "31-12-2026", "invalid-date-string"};
        for (String d : dates) {
            ExtractedRFQ rfqD = ExtractedRFQ.builder()
                    .deliveryDate(d)
                    .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                    .build();
            RFQRequest reqD = rfqBuilderService.buildRFQRequest(rfqD, fullBuyer, "Sub", null);
            assertNotNull(reqD.getDeliveryDate());
        }

        // 18. Multi-token location parsing
        ExtractedRFQ rfq3Tokens = ExtractedRFQ.builder()
                .deliveryLocation("Plant 4, Industrial Area Phase 2, Bangalore")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest req3Tokens = rfqBuilderService.buildRFQRequest(rfq3Tokens, fullBuyer, "Sub", null);
        assertEquals("Bangalore", req3Tokens.getClientdeliverylocationrfq().get(0).getCity());

        // 19. All state extraction branches
        String[] stateLocs = {
                "Raipur, Chhattisgarh", "Bangalore, Karnataka", "Hyderabad, Telangana",
                "Vijayawada, Andhra", "Mumbai, Maharashtra", "Chennai, Tamilnadu",
                "New Delhi, Delhi", "Ahmedabad, Gujarat", "Kolkata, Bengal",
                "Jaipur, Rajasthan", "Bhopal, Madhya Pradesh", "Lucknow, Uttar Pradesh",
                "Kochi, Kerala", "Ludhiana, Punjab", "Gurugram, Haryana",
                "Patna, Bihar", "Bhubaneswar, Odisha", "Guwahati, Assam", "Ranchi, Jharkhand"
        };
        for (String loc : stateLocs) {
            ExtractedRFQ rfqState = ExtractedRFQ.builder()
                    .deliveryLocation(loc)
                    .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                    .build();
            RFQRequest reqState = rfqBuilderService.buildRFQRequest(rfqState, fullBuyer, "Sub", null);
            assertNotNull(reqState.getClientdeliverylocationrfq().get(0).getState());
        }

        // 20. Multi-item primary description with blank first item or ending with s
        ExtractedRFQ rfqBlankFirst = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("").quantity(1.0).build(),
                        RFQItem.builder().itemDescription("Second Item").quantity(2.0).build()
                ))
                .build();
        RFQRequest reqBlankFirst = rfqBuilderService.buildRFQRequest(rfqBlankFirst, fullBuyer, "Sub", null);
        assertEquals("Procurement Items", reqBlankFirst.getProjectDesc());

        ExtractedRFQ rfqEndsWithS = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("Centrifugal Pumps").quantity(1.0).build(),
                        RFQItem.builder().itemDescription("Pipes").quantity(2.0).build()
                ))
                .build();
        RFQRequest reqEndsWithS = rfqBuilderService.buildRFQRequest(rfqEndsWithS, fullBuyer, "Sub", null);
        assertEquals("Centrifugal Pumps", reqEndsWithS.getProjectDesc());

        // 21. Location with first token matching digits or state
        ExtractedRFQ rfqDigitToken2 = ExtractedRFQ.builder()
                .deliveryLocation("12345, Maharashtra")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest reqDigit = rfqBuilderService.buildRFQRequest(rfqDigitToken2, fullBuyer, "Sub", null);
        assertNotNull(reqDigit);

        ExtractedRFQ rfqStateToken2 = ExtractedRFQ.builder()
                .deliveryLocation("Maharashtra, 400001")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest reqStateToken = rfqBuilderService.buildRFQRequest(rfqStateToken2, fullBuyer, "Sub", null);
        assertNotNull(reqStateToken);

        // 22. buyer with null orgId/userId/name and token present
        Buyer buyerNoIds = Buyer.builder().email("buyer@corp.com").name(null).orgId(null).userId(null).token("TOKEN-XYZ").build();
        ExtractedRFQ rfqBuyerNoIds = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest reqNoIds = rfqBuilderService.buildRFQRequest(rfqBuyerNoIds, buyerNoIds, "Sub", null);
        assertEquals("1", reqNoIds.getOrg().getId());
        assertEquals("1", reqNoIds.getUser());
        assertEquals("User", reqNoIds.getCreatedBy());
        assertEquals("TOKEN-XYZ", reqNoIds.getToken());

        // 23. buyer == null
        RFQRequest reqNullBuyer = rfqBuilderService.buildRFQRequest(rfqBuyerNoIds, null, "Sub", null);
        assertNull(reqNullBuyer.getBuyerEmail());
        assertNull(reqNullBuyer.getToken());
        assertEquals("User", reqNullBuyer.getCreatedBy());

        // 24. item with specification "Not Specified" and non-empty remarks
        ExtractedRFQ rfqRemarks = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder()
                        .itemDescription("Hex Bolt")
                        .specification("Not Specified")
                        .remarks("Special Zinc Coating")
                        .quantity(10.0)
                        .build()))
                .build();
        RFQRequest reqRemarks = rfqBuilderService.buildRFQRequest(rfqRemarks, fullBuyer, "Sub", null);
        assertEquals("Special Zinc Coating", reqRemarks.getRfqItem().get(0).getBrand());

        // 25. item with brand already starting with "Brand:"
        ExtractedRFQ rfqBrandPrefix = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder()
                        .itemDescription("Hex Bolt")
                        .brand("Brand: Siemens")
                        .quantity(10.0)
                        .build()))
                .build();
        RFQRequest reqBrandPrefix = rfqBuilderService.buildRFQRequest(rfqBrandPrefix, fullBuyer, "Sub", null);
        assertEquals("Brand: Siemens", reqBrandPrefix.getRfqItem().get(0).getRemarks());

        // 26. item with empty specification and description fallback for part code
        ExtractedRFQ rfqPnOnly = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder()
                        .itemDescription("")
                        .specification("")
                        .remarks("")
                        .partCode("PN-999")
                        .quantity(10.0)
                        .build()))
                .build();
        RFQRequest reqPnOnly = rfqBuilderService.buildRFQRequest(rfqPnOnly, fullBuyer, "Sub", null);
        assertTrue(reqPnOnly.getRfqItem().get(0).getBrand().contains("P/N: PN-999"));

        // 27. mixed attachment files (null, empty, non-existent, valid)
        File emptyFile2 = File.createTempFile("empty", ".txt");
        File nonExistent = new File("non_existent_file_123.txt");
        File validAtt2 = File.createTempFile("valid", ".txt");
        try (FileOutputStream fos = new FileOutputStream(validAtt2)) {
            fos.write(new byte[]{1, 2, 3});
        }
        List<File> mixedFiles = new java.util.ArrayList<>();
        mixedFiles.add(null);
        mixedFiles.add(emptyFile2);
        mixedFiles.add(nonExistent);
        mixedFiles.add(validAtt2);

        RFQRequest reqMixedFiles = rfqBuilderService.buildRFQRequest(rfqPnOnly, fullBuyer, "Sub", mixedFiles);
        assertEquals(1, reqMixedFiles.getRfqDocument().size());
        emptyFile2.delete();
        validAtt2.delete();

        // 28. location fields with locStr blank but city/state/pincode provided
        ExtractedRFQ rfqCityOnly = ExtractedRFQ.builder()
                .deliveryCity("Bangalore")
                .deliveryLocation("")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest reqCityOnly = rfqBuilderService.buildRFQRequest(rfqCityOnly, fullBuyer, "Sub", null);
        assertEquals("Bangalore", reqCityOnly.getClientdeliverylocationrfq().get(0).getCity());

        ExtractedRFQ rfqStateOnly = ExtractedRFQ.builder()
                .deliveryState("Karnataka")
                .deliveryLocation("")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest reqStateOnly = rfqBuilderService.buildRFQRequest(rfqStateOnly, fullBuyer, "Sub", null);
        assertEquals("Karnataka", reqStateOnly.getClientdeliverylocationrfq().get(0).getState());

        ExtractedRFQ rfqPinOnlyLoc = ExtractedRFQ.builder()
                .deliveryPincode("560001")
                .deliveryLocation("")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();
        RFQRequest reqPinOnlyLoc = rfqBuilderService.buildRFQRequest(rfqPinOnlyLoc, fullBuyer, "Sub", null);
        assertEquals("560001", reqPinOnlyLoc.getClientdeliverylocationrfq().get(0).getPincode());

        // 29. item UOM variations
        ExtractedRFQ rfqUomVariants = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("Item 1").uom("null").build(),
                        RFQItem.builder().itemDescription("Item 2").uom("Not Specified").build(),
                        RFQItem.builder().itemDescription("Item 3").uom("   ").build(),
                        RFQItem.builder().itemDescription("Item 4").uom("Kgs").build()
                ))
                .build();
        RFQRequest reqUoms = rfqBuilderService.buildRFQRequest(rfqUomVariants, fullBuyer, "Sub", null);
        assertEquals("Nos", reqUoms.getRfqItem().get(0).getUnitofMeasures());
        assertEquals("Nos", reqUoms.getRfqItem().get(1).getUnitofMeasures());
        assertEquals("Nos", reqUoms.getRfqItem().get(2).getUnitofMeasures());
        assertEquals("Kgs", reqUoms.getRfqItem().get(3).getUnitofMeasures());

        // 30. item specification "null" string
        ExtractedRFQ rfqSpecNullStr = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("Item").specification("null").build()))
                .build();
        RFQRequest reqSpecNull = rfqBuilderService.buildRFQRequest(rfqSpecNullStr, fullBuyer, "Sub", null);
        assertEquals("Item", reqSpecNull.getRfqItem().get(0).getBrand());

        // 31. Delivery city, state, pincode provided without locStr
        ExtractedRFQ rfqCityStatePinNoLoc = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryCity("Pune")
                .deliveryState("Maharashtra")
                .deliveryPincode("411001")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqCityNoLoc = rfqBuilderService.buildRFQRequest(rfqCityStatePinNoLoc, fullBuyer, "Sub", null);
        assertEquals("Pune", reqCityNoLoc.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Maharashtra", reqCityNoLoc.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("411001", reqCityNoLoc.getClientdeliverylocationrfq().get(0).getPincode());

        // 32. Registered Profile Address location string with buyer address
        ExtractedRFQ rfqRegAddr = ExtractedRFQ.builder()
                .deliveryLocation("Registered Profile Address")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        Buyer buyerWithAddr = Buyer.builder().address("123 Main St, City").city("City").state("State").pincode("123456").build();
        RFQRequest reqRegAddr = rfqBuilderService.buildRFQRequest(rfqRegAddr, buyerWithAddr, "Sub", null);
        assertEquals("123 Main St, City", reqRegAddr.getClientdeliverylocationrfq().get(0).getAddress());

        // 33. Part code with blank item description and blank specs
        ExtractedRFQ rfqPartOnly = ExtractedRFQ.builder()
                .items(List.of(RFQItem.builder().itemDescription("").specification("").remarks("").partCode("PART-XYZ-99").build()))
                .build();
        RFQRequest reqPartOnly = rfqBuilderService.buildRFQRequest(rfqPartOnly, fullBuyer, "Sub", null);
        assertTrue(reqPartOnly.getRfqItem().get(0).getBrand().contains("PART-XYZ-99"));

        // 34. Email Delivery Location string
        ExtractedRFQ rfqEmailLocStr = ExtractedRFQ.builder()
                .deliveryLocation("Email Delivery Location")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqEmailLocStr = rfqBuilderService.buildRFQRequest(rfqEmailLocStr, buyerWithAddr, "Sub", null);
        assertEquals("123 Main St, City", reqEmailLocStr.getClientdeliverylocationrfq().get(0).getAddress());

        // 35. Buyer with blank city, state, pincode, address strings
        Buyer buyerBlankFields = Buyer.builder().address("   ").city("   ").state("   ").pincode("   ").name("   ").build();
        ExtractedRFQ rfqNoLocBuyerBlank = ExtractedRFQ.builder()
                .deliveryLocation("")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqBuyerBlank = rfqBuilderService.buildRFQRequest(rfqNoLocBuyerBlank, buyerBlankFields, "Sub", null);
        assertEquals("Registered Profile Address", reqBuyerBlank.getClientdeliverylocationrfq().get(0).getAddress());

        // 36. Multi-item with null first item description
        ExtractedRFQ rfqMultiNullFirst = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription(null).build(),
                        RFQItem.builder().itemDescription("Item 2").build()
                ))
                .build();
        RFQRequest reqMultiNullFirst = rfqBuilderService.buildRFQRequest(rfqMultiNullFirst, fullBuyer, "Sub", null);
        assertEquals("Procurement Items", reqMultiNullFirst.getProjectDesc());

        // 37. LocStr "NotSpecified" and "N/A", with "Not Specified" city/state/pin
        ExtractedRFQ rfqLocNotSpecVariants = ExtractedRFQ.builder()
                .deliveryLocation("NotSpecified")
                .deliveryCity("Not Specified")
                .deliveryState("Not Specified")
                .deliveryPincode("Not Specified")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqNotSpecVariants = rfqBuilderService.buildRFQRequest(rfqLocNotSpecVariants, fullBuyer, "Sub", null);
        assertEquals("100 Anna Salai", reqNotSpecVariants.getClientdeliverylocationrfq().get(0).getAddress());

        ExtractedRFQ rfqLocNA = ExtractedRFQ.builder()
                .deliveryLocation("N/A")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqLocNA = rfqBuilderService.buildRFQRequest(rfqLocNA, fullBuyer, "Sub", null);
        assertEquals("100 Anna Salai", reqLocNA.getClientdeliverylocationrfq().get(0).getAddress());

        // 38. LocStr exactly matching buyer address
        ExtractedRFQ rfqMatchingBuyerAddr = ExtractedRFQ.builder()
                .deliveryLocation("123 Main St, City")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqMatchBuyer = rfqBuilderService.buildRFQRequest(rfqMatchingBuyerAddr, buyerWithAddr, "Sub", null);
        assertEquals("123 Main St, City", reqMatchBuyer.getClientdeliverylocationrfq().get(0).getAddress());

        // 39. State name scanning variations in locStr
        String[] states = {"Madhya Pradesh", "Uttar Pradesh", "Kerala", "Punjab", "Haryana", "Bihar", "Odisha", "Assam", "Jharkhand"};
        for (String st : states) {
            ExtractedRFQ rfqSt = ExtractedRFQ.builder()
                    .deliveryLocation("Plant Site, " + st)
                    .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                    .build();
            RFQRequest reqSt = rfqBuilderService.buildRFQRequest(rfqSt, fullBuyer, "Sub", null);
            assertEquals(st, reqSt.getClientdeliverylocationrfq().get(0).getState());
        }

        // 40. LocStr starting with state name or numeric digits
        ExtractedRFQ rfqStateStart = ExtractedRFQ.builder()
                .deliveryLocation("Kerala, Kochi")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqStateStart = rfqBuilderService.buildRFQRequest(rfqStateStart, fullBuyer, "Sub", null);
        assertEquals("Kerala", reqStateStart.getClientdeliverylocationrfq().get(0).getState());

        ExtractedRFQ rfqNumStart = ExtractedRFQ.builder()
                .deliveryLocation("560001, Industrial Area")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqNumStart = rfqBuilderService.buildRFQRequest(rfqNumStart, fullBuyer, "Sub", null);
        assertNotNull(reqNumStart);

        // 41. Item with remarks fallback, long spec truncation, and UOM edge cases
        String longSpecStr = "A".repeat(250) + ",.-";
        ExtractedRFQ rfqEdgeItems = ExtractedRFQ.builder()
                .items(List.of(
                        RFQItem.builder().itemDescription("Item 1").specification(null).remarks("Remarks As Spec").uom("null").brand("Very Long Brand Name ".repeat(5)).build(),
                        RFQItem.builder().itemDescription("Item 2").specification(longSpecStr).uom("Not Specified").build()
                ))
                .build();
        RFQRequest reqEdgeItems = rfqBuilderService.buildRFQRequest(rfqEdgeItems, null, "Sub", null);
        assertEquals("Remarks As Spec", reqEdgeItems.getRfqItem().get(0).getBrand());
        assertEquals("Nos", reqEdgeItems.getRfqItem().get(0).getUnitofMeasures());
        assertEquals("User", reqEdgeItems.getRfqItem().get(0).getCreatedBy());
        assertTrue(reqEdgeItems.getRfqItem().get(1).getBrand().length() <= 200);

        // 42. Buyer with null name, blank orgId, blank userId
        Buyer buyerNullName = Buyer.builder().name(null).orgId("   ").userId("   ").email(null).token(null).build();
        RFQRequest reqBuyerNullName = rfqBuilderService.buildRFQRequest(rfqEdgeItems, buyerNullName, "Sub", null);
        assertEquals("User", reqBuyerNullName.getRfqItem().get(0).getCreatedBy());
        assertEquals("1", reqBuyerNullName.getOrg().getId());
        assertEquals("1", reqBuyerNullName.getUser());
        assertNull(reqBuyerNullName.getBuyerEmail());
        assertNull(reqBuyerNullName.getToken());

        // 43. Clean locStr with city, state, pin all extracted together
        ExtractedRFQ rfqFullLoc = ExtractedRFQ.builder()
                .deliveryLocation("Ship to Mumbai Port, Maharashtra 400001")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqFullLoc = rfqBuilderService.buildRFQRequest(rfqFullLoc, fullBuyer, "Sub", null);
        assertEquals("Mumbai", reqFullLoc.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Maharashtra", reqFullLoc.getClientdeliverylocationrfq().get(0).getState());
        assertEquals("400001", reqFullLoc.getClientdeliverylocationrfq().get(0).getPincode());

        // 44. hasEmailLocation with locStr blank but city present
        ExtractedRFQ rfqCityOnly2 = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryCity("Bangalore")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqCityOnly2 = rfqBuilderService.buildRFQRequest(rfqCityOnly2, fullBuyer, "Sub", null);
        assertEquals("Bangalore", reqCityOnly2.getClientdeliverylocationrfq().get(0).getCity());

        // 45. hasEmailLocation with locStr and city blank but state present
        ExtractedRFQ rfqStateOnly2 = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryCity("")
                .deliveryState("Karnataka")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqStateOnly2 = rfqBuilderService.buildRFQRequest(rfqStateOnly2, fullBuyer, "Sub", null);
        assertEquals("Karnataka", reqStateOnly2.getClientdeliverylocationrfq().get(0).getState());

        // 46. hasEmailLocation with locStr, city, state blank but pincode present
        ExtractedRFQ rfqPinOnly2 = ExtractedRFQ.builder()
                .deliveryLocation("")
                .deliveryCity("")
                .deliveryState("")
                .deliveryPincode("560001")
                .items(List.of(RFQItem.builder().itemDescription("Item").build()))
                .build();
        RFQRequest reqPinOnly2 = rfqBuilderService.buildRFQRequest(rfqPinOnly2, fullBuyer, "Sub", null);
        assertEquals("560001", reqPinOnly2.getClientdeliverylocationrfq().get(0).getPincode());
    }
}



