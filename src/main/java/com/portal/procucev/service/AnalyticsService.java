package com.portal.procucev.service;

import java.util.Map;

public interface AnalyticsService {
    Map<String, Object> getDashboardData();
    Map<String, Object> getCategoriesData();
    Map<String, Object> getFunnelData(String type);
    Map<String, Object> getCalendarData(Integer year, Integer month);
    Map<String, Object> searchCompanies(String query);
    Map<String, Object> processChat(Map<String, Object> requestPayload);
}
