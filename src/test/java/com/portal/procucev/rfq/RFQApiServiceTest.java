package com.portal.procucev.rfq;

import com.portal.procucev.model.Rfq;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.service.RFQApiService;
import com.portal.procucev.service.AutomaticRfqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
                                .quantity(null)
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
}

