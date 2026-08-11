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
}
