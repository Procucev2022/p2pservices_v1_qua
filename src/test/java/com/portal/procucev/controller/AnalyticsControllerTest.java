package com.portal.procucev.controller;

import com.portal.procucev.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    void testGetDashboardData() {
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("status", "success");
        when(analyticsService.getDashboardData()).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.getDashboardData();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void testGetCategoriesData() {
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("categories", "data");
        when(analyticsService.getCategoriesData()).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.getCategoriesData();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void testGetFunnelData() {
        Map<String, Object> mockData = new HashMap<>();
        when(analyticsService.getFunnelData("buyer")).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.getFunnelData("buyer");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void testGetFunnelStageDetails() {
        Map<String, Object> mockData = new HashMap<>();
        when(analyticsService.getFunnelStageDetails("buyer", 1, "test")).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.getFunnelStageDetails("buyer", 1, "test");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void testGetFunnelDropoffDetails() {
        Map<String, Object> mockData = new HashMap<>();
        when(analyticsService.getFunnelDropoffDetails("seller", 2, "query")).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.getFunnelDropoffDetails("seller", 2, "query");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void testGetCalendarData() {
        Map<String, Object> mockData = new HashMap<>();
        when(analyticsService.getCalendarData(2026, 8)).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.getCalendarData(2026, 8);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void testSearchCompanies() {
        Map<String, Object> mockData = new HashMap<>();
        when(analyticsService.searchCompanies("Tata")).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.searchCompanies("Tata");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }

    @Test
    void testProcessChat() {
        Map<String, Object> payload = Map.of("prompt", "hello", "companyId", "org-1");
        Map<String, Object> mockData = new HashMap<>();
        when(analyticsService.processChat(payload)).thenReturn(mockData);

        ResponseEntity<Map<String, Object>> response = analyticsController.processChat(payload);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockData, response.getBody());
    }
}
