package com.portal.procucev.Dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnalyticsDashboardDtoTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        AnalyticsDashboardDto dto = new AnalyticsDashboardDto();
        assertNull(dto.getMetrics());
        assertNull(dto.getSources());
        assertNull(dto.getLifecycleStages());
        assertNull(dto.getNoQuoteAlerts());

        Object metrics = new Object();
        List<Object> sources = new ArrayList<>();
        List<Object> lifecycleStages = new ArrayList<>();
        List<Object> noQuoteAlerts = new ArrayList<>();

        dto.setMetrics(metrics);
        dto.setSources(sources);
        dto.setLifecycleStages(lifecycleStages);
        dto.setNoQuoteAlerts(noQuoteAlerts);

        assertEquals(metrics, dto.getMetrics());
        assertEquals(sources, dto.getSources());
        assertEquals(lifecycleStages, dto.getLifecycleStages());
        assertEquals(noQuoteAlerts, dto.getNoQuoteAlerts());
    }

    @Test
    void testAllArgsConstructor() {
        Object metrics = "sampleMetrics";
        List<Object> sources = List.of("source1");
        List<Object> lifecycleStages = List.of("stage1");
        List<Object> noQuoteAlerts = List.of("alert1");

        AnalyticsDashboardDto dto = new AnalyticsDashboardDto(metrics, sources, lifecycleStages, noQuoteAlerts);

        assertEquals(metrics, dto.getMetrics());
        assertEquals(sources, dto.getSources());
        assertEquals(lifecycleStages, dto.getLifecycleStages());
        assertEquals(noQuoteAlerts, dto.getNoQuoteAlerts());
    }
}
