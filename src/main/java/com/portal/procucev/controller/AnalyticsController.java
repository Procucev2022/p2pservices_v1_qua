package com.portal.procucev.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.portal.procucev.service.AnalyticsService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/rest/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        return ResponseEntity.ok(analyticsService.getDashboardData());
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategoriesData() {
        return ResponseEntity.ok(analyticsService.getCategoriesData());
    }

    @GetMapping("/funnel")
    public ResponseEntity<Map<String, Object>> getFunnelData(@RequestParam(defaultValue = "buyer") String type) {
        return ResponseEntity.ok(analyticsService.getFunnelData(type));
    }

    @GetMapping("/funnel/details")
    public ResponseEntity<Map<String, Object>> getFunnelStageDetails(
            @RequestParam(defaultValue = "buyer") String type,
            @RequestParam(defaultValue = "1") Integer stage,
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(analyticsService.getFunnelStageDetails(type, stage, q));
    }

    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Object>> getCalendarData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(analyticsService.getCalendarData(year, month));
    }

    @GetMapping("/console/search")
    public ResponseEntity<Map<String, Object>> searchCompanies(@RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(analyticsService.searchCompanies(q));
    }

    @PostMapping("/console/chat")
    public ResponseEntity<Map<String, Object>> processChat(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(analyticsService.processChat(payload));
    }
}
