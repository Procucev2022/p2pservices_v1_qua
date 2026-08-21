package com.portal.procucev.rfq;

import com.portal.procucev.model.Organization;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.rfq.service.RFQApiService;
import com.portal.procucev.service.AutomaticRfqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RFQApiServiceTest {

    @Mock
    private AutomaticRfqService automaticRfqService;

    @Mock
    private DateParser dateParser;

    private RFQApiService rfqApiService;

    @BeforeEach
    void setUp() {
        rfqApiService = new RFQApiService(automaticRfqService, dateParser);
    }

    @Test
    void testSubmitRFQSuccess() {
        RFQRequest request = new RFQRequest();
        request.setRfqNumber("RFQ-2026-001");
        request.setProjectDesc("Project");
        request.setUser("User123");
        request.setBuyerEmail("buyer@test.com");

        RFQRequest.OrgRef org = RFQRequest.OrgRef.builder().id("ORG1").build();
        request.setOrg(org);

        request.setDeliveryDate("2026-12-31");
        when(dateParser.parseToDate("2026-12-31")).thenReturn(new Date());

        RFQRequest.LocationDto loc = new RFQRequest.LocationDto();
        loc.setAddress("Addr");
        loc.setCity("City");
        loc.setState("State");
        loc.setPincode("560001");
        request.setClientdeliverylocationrfq(List.of(loc));

        RFQRequest.RfqItemDto item = new RFQRequest.RfqItemDto();
        item.setDescription("Item 1");
        item.setCategory("Cat");
        item.setQuantity(5.0);
        item.setUnitofMeasures("NOS");
        item.setBrand("Brand");
        item.setItemcode("CODE");
        item.setRemarks("Rem");
        item.setSerialNo(1);
        request.setRfqItem(List.of(item));

        Map<String, String> docMap = Map.of(
                "fileName", "doc.pdf",
                "file", Base64.getEncoder().encodeToString("data".getBytes())
        );
        request.setRfqDocument(List.of(docMap));

        when(automaticRfqService.raiseRfq(any())).thenReturn(true);

        RFQResponse response = rfqApiService.submitRFQ(request);
        assertNotNull(response);
        assertEquals("RFQ-2026-001", response.getRfqNumber());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("buyer@test.com", response.getBuyerEmail());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testSubmitRFQFailure() {
        RFQRequest request = new RFQRequest();
        request.setRfqNumber("RFQ-2026-002");
        request.setBuyerEmail("buyer@test.com");

        when(automaticRfqService.raiseRfq(any())).thenReturn(false);

        RFQResponse response = rfqApiService.submitRFQ(request);
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());

        when(automaticRfqService.raiseRfq(any())).thenThrow(new RuntimeException("DB Exception"));
        RFQResponse errorResponse = rfqApiService.submitRFQ(request);
        assertNotNull(errorResponse);
        assertEquals("FAILED", errorResponse.getStatus());
        assertTrue(errorResponse.getMessage().contains("DB Exception"));
    }
}
