package com.portal.procucev.rfq;

import com.portal.procucev.customexception.RfqDocumentSizeExceededException;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.service.RFQApiService;
import com.portal.procucev.service.AutomaticRfqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class RFQApiServiceTest {

    private AutomaticRfqService automaticRfqService;
    private DateParser dateParser;
    private RFQApiService rfqApiService;

    @BeforeEach
    void setUp() {
        automaticRfqService = Mockito.mock(AutomaticRfqService.class);
        dateParser = new DateParser();
        rfqApiService = new RFQApiService(automaticRfqService, dateParser);
    }

    @Test
    @DisplayName("Test submitRFQ success")
    void testSubmitRFQSuccess() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenReturn(true);

        String b64 = Base64.getEncoder().encodeToString("Sample Data".getBytes());

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-100")
                .buyerEmail("buyer@test.com")
                .projectDesc("Laptop Requirement")
                .user("5")
                .deliveryDate("2026-08-25")
                .org(RFQRequest.OrgRef.builder().id("10").build())
                .clientdeliverylocationrfq(List.of(
                        RFQRequest.LocationDto.builder().address("Address").city("City").state("State").pincode("123456").build()
                ))
                .rfqItem(List.of(
                        RFQRequest.RfqItemDto.builder().description("Laptop").quantity(2.0).unitofMeasures("NOS").brand("Dell").category("IT").itemcode("P123").remarks("Rem").serialNo(1001).build()
                ))
                .rfqDocument(List.of(
                        Map.of("fileName", "doc.txt", "file", b64)
                ))
                .build();

        RFQResponse resp = rfqApiService.submitRFQ(req);

        assertNotNull(resp);
        assertEquals("RFQ-100", resp.getRfqNumber());
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("buyer@test.com", resp.getBuyerEmail());
        assertTrue(resp.getMessage().contains("successfully"));
    }

    @Test
    @DisplayName("Test submitRFQ failure from automaticRfqService")
    void testSubmitRFQFailure() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenReturn(false);

        RFQRequest req = RFQRequest.builder().rfqNumber("RFQ-101").buyerEmail("buyer@test.com").build();

        RFQResponse resp = rfqApiService.submitRFQ(req);

        assertNotNull(resp);
        assertEquals("RFQ-101", resp.getRfqNumber());
        assertEquals("FAILED", resp.getStatus());
    }

    @Test
    @DisplayName("Test submitRFQ exception handling")
    void testSubmitRFQException() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenThrow(new RuntimeException("DB Exception"));

        RFQRequest req = RFQRequest.builder().rfqNumber("RFQ-102").buyerEmail("buyer@test.com").build();

        RFQResponse resp = rfqApiService.submitRFQ(req);

        assertNotNull(resp);
        assertEquals("RFQ-102", resp.getRfqNumber());
        assertEquals("FAILED", resp.getStatus());
        assertTrue(resp.getMessage().contains("DB Exception"));
    }

    @Test
    @DisplayName("Test submitRFQ with null user, null org, null delivery date")
    void testSubmitRFQNullFields() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenReturn(true);

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-103")
                .buyerEmail("buyer@test.com")
                .user(null)
                .org(null)
                .deliveryDate(null)
                .clientdeliverylocationrfq(null)
                .rfqItem(null)
                .rfqDocument(null)
                .build();

        RFQResponse resp = rfqApiService.submitRFQ(req);
        assertEquals("SUCCESS", resp.getStatus());
    }

    @Test
    @DisplayName("Test submitRFQ with blank delivery date and empty locations/items/documents")
    void testSubmitRFQBlankDeliveryDate() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenReturn(true);

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-104")
                .buyerEmail("buyer@test.com")
                .deliveryDate("")
                .org(RFQRequest.OrgRef.builder().id("").build())
                .clientdeliverylocationrfq(List.of())
                .rfqItem(List.of())
                .rfqDocument(List.of())
                .build();

        RFQResponse resp = rfqApiService.submitRFQ(req);
        assertEquals("SUCCESS", resp.getStatus());
    }

    @Test
    @DisplayName("Test submitRFQ with null quantity and blank encoded file skipped")
    void testSubmitRFQNullQuantityAndBlankFile() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenReturn(true);

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-105")
                .buyerEmail("buyer@test.com")
                .rfqItem(List.of(
                        RFQRequest.RfqItemDto.builder()
                                .description("Item")
                                .quantity(1.0)
                                .build()
                ))
                .rfqDocument(List.of(
                        Map.of("fileName", "doc.txt", "file", ""),
                        Map.of("fileName", "empty.txt")
                ))
                .build();

        RFQResponse resp = rfqApiService.submitRFQ(req);
        assertEquals("SUCCESS", resp.getStatus());
    }

    @Test
    @DisplayName("Test submitRFQ with org id null")
    void testSubmitRFQWithOrgIdNull() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenReturn(true);

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-106")
                .buyerEmail("buyer@test.com")
                .org(RFQRequest.OrgRef.builder().id(null).build())
                .build();

        RFQResponse resp = rfqApiService.submitRFQ(req);
        assertEquals("SUCCESS", resp.getStatus());
    }

    @Test
    @DisplayName("Test submitRFQ with explicit custom sourceType")
    void testSubmitRFQCustomSourceType() {
        ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
        Mockito.when(automaticRfqService.raiseRfq(captor.capture())).thenReturn(true);

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-107")
                .buyerEmail("buyer@test.com")
                .sourceType("PORTAL_DIRECT")
                .build();

        RFQResponse resp = rfqApiService.submitRFQ(req);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("PORTAL_DIRECT", captor.getValue().getSourceType());
    }
}


/**
 * Behaviour added when the email path was moved onto the shared RFQ creation pipeline.
 */
class RFQApiServiceSharedPipelineTest {

    private AutomaticRfqService automaticRfqService;
    private RFQApiService rfqApiService;

    @BeforeEach
    void setUp() {
        automaticRfqService = Mockito.mock(AutomaticRfqService.class);
        rfqApiService = new RFQApiService(automaticRfqService, new DateParser());
    }

    @Test
    @DisplayName("submitRFQ delegates document mapping to the shared pipeline instead of mapping them itself")
    void submitRfqDelegatesDocumentMapping() {
        Mockito.when(automaticRfqService.raiseRfq(any(Rfq.class))).thenReturn(true);

        List<Map<String, String>> documents = List.of(
                Map.of("fileName", "drawing.jpg", "file", Base64.getEncoder().encodeToString("payload".getBytes())));

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-200")
                .buyerEmail("buyer@test.com")
                .rfqDocument(documents)
                .build();

        assertEquals("SUCCESS", rfqApiService.submitRFQ(req).getStatus());
        Mockito.verify(automaticRfqService).attachDocuments(any(Rfq.class), Mockito.eq(documents));
    }

    /**
     * An oversized attachment must be reported as its own outcome. Reported as a generic failure it
     * reached the "details missing" acknowledgement, which always names a missing quantity and so
     * told the buyer their quantity was absent when it had been extracted correctly.
     */
    @Test
    @DisplayName("submitRFQ reports FILE_SIZE_EXCEEDED when the shared pipeline rejects an oversized document")
    void submitRfqReportsFileSizeExceeded() {
        Mockito.doThrow(new RfqDocumentSizeExceededException("drawing.jpg", 30_000_000L, 26214400L))
                .when(automaticRfqService).attachDocuments(any(Rfq.class), any());

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-201")
                .buyerEmail("buyer@test.com")
                .rfqDocument(List.of(Map.of("fileName", "drawing.jpg", "file", "AAAA")))
                .build();

        RFQResponse resp = rfqApiService.submitRFQ(req);

        assertEquals(RFQApiService.STATUS_FILE_SIZE_EXCEEDED, resp.getStatus());
        assertTrue(resp.getMessage().contains("drawing.jpg"));
        assertTrue(resp.getMessage().contains("exceeds"));
        Mockito.verify(automaticRfqService, Mockito.never()).raiseRfq(any(Rfq.class));
    }

    /**
     * Quantity defaulting belongs to the shared pipeline. This path must pass the extracted value
     * through unchanged so there is exactly one rule deciding what a missing quantity becomes.
     */
    @Test
    @DisplayName("submitRFQ passes a missing quantity through as 0 for the shared default to handle")
    void submitRfqPassesMissingQuantityThrough() {
        ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
        Mockito.when(automaticRfqService.raiseRfq(captor.capture())).thenReturn(true);

        RFQRequest req = RFQRequest.builder()
                .rfqNumber("RFQ-202")
                .buyerEmail("buyer@test.com")
                .rfqItem(List.of(RFQRequest.RfqItemDto.builder().description("Washer").quantity(null).build()))
                .build();

        assertEquals("SUCCESS", rfqApiService.submitRFQ(req).getStatus());
        assertEquals(0.0, captor.getValue().getRfqItem().get(0).getQuantity());
    }
}
