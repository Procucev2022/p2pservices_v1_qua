package com.portal.procucev.controller;

import com.portal.procucev.service.ReportsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsControllerTest {

    @Mock
    private ReportsService reportsService;

    @InjectMocks
    private ReportsController controller;

    @Test
    void testGetSellerReports() throws Exception {
        when(reportsService.getSellerReports(anyString(), anyString(), anyString())).thenReturn(Collections.emptyList());
        ResponseEntity<?> resp = controller.getSellerReports("2026-01-01", "2026-01-31", "sellerReport");
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testGetBuyerReports() throws Exception {
        when(reportsService.getBuyerReports(anyString(), anyString(), anyString())).thenReturn(Collections.emptyList());
        ResponseEntity<?> resp = controller.getBuyerReports("2026-01-01", "2026-01-31", "buyerReport");
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testGetRfqReports() throws Exception {
        when(reportsService.getRfqReports(anyString(), anyString(), anyString())).thenReturn(Collections.emptyList());
        ResponseEntity<?> resp = controller.getRfqReports("2026-01-01", "2026-01-31", "rfqReport");
        assertEquals(200, resp.getStatusCode().value());
    }
}
