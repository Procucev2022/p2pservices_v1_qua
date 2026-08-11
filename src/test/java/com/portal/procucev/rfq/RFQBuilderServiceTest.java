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
        assertEquals("Brand: Dell", itemDto.getBrand());
        assertEquals("NOS", itemDto.getUnitofMeasures());
        assertEquals(1.0, itemDto.getQuantity());
        assertEquals("Dell Laptop", itemDto.getDescription());
        assertEquals("IT Hardware", itemDto.getCategory());
        assertEquals("P123", itemDto.getItemcode());
        assertEquals("16GB RAM", itemDto.getRemarks());
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
                        RFQItem.builder().itemDescription("Mechanical Keyboard").quantity(null).uom("NOS").brand("Logitech").build()
                ))
                .build();

        Buyer buyer = Buyer.builder().name("Jane").email("jane@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Subject", List.of());

        assertNotNull(req);
        assertEquals("Computer Monitors", req.getProjectDesc());
        assertEquals(2, req.getRfqItem().size());
        assertEquals(2.5, req.getRfqItem().get(0).getQuantity());
        assertEquals(1.0, req.getRfqItem().get(1).getQuantity());
        assertEquals("Brand: Logitech", req.getRfqItem().get(1).getBrand());
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
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(null).uom(null).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, null, null);
        assertNotNull(req);
        assertEquals(1, req.getClientdeliverylocationrfq().size());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getAddress());
        assertEquals("", req.getClientdeliverylocationrfq().get(0).getPincode());
    }

    @Test
    @DisplayName("Test buildRFQRequest with 560xxx pincode triggers Bangalore detection")
    void testBuildRFQRequestBangalorePincode() {
        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryLocation("Office in 560037 area")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Bangalore", req.getClientdeliverylocationrfq().get(0).getCity());
        assertEquals("Karnataka", req.getClientdeliverylocationrfq().get(0).getState());
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
    @DisplayName("Test buildRFQRequest with pincode DAO lookup for state")
    void testBuildRFQRequestPincodeDaoLookup() {
        PincodeData pinData = new PincodeData();
        pinData.setState("Telangana");
        pinData.setCity("Hyderabad");
        Mockito.when(pincodeDao.findByPincode("500001")).thenReturn(pinData);

        ExtractedRFQ rfq = ExtractedRFQ.builder()
                .deliveryPincode("500001")
                .items(List.of(RFQItem.builder().itemDescription("Item").quantity(1.0).build()))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Telangana", req.getClientdeliverylocationrfq().get(0).getState());
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
                        RFQItem.builder().itemDescription("Item4").quantity(-5.0).build()
                ))
                .build();

        Buyer buyer = Buyer.builder().name("User").email("user@test.com").build();

        RFQRequest req = rfqBuilderService.buildRFQRequest(rfq, buyer, "Sub", null);
        assertEquals("Brand: Not Specified", req.getRfqItem().get(0).getBrand());
        assertEquals("Brand: Not Specified", req.getRfqItem().get(1).getBrand());
        assertEquals("Brand: Already Prefixed", req.getRfqItem().get(2).getBrand());
        assertEquals(1.0, req.getRfqItem().get(3).getQuantity()); // negative resets to 1
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
        assertEquals("Spec1", req.getRfqItem().get(0).getRemarks());
        assertEquals("Remark2", req.getRfqItem().get(1).getRemarks());
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
        assertEquals("SingleRemark", req.getRfqItem().get(0).getRemarks());
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
}

