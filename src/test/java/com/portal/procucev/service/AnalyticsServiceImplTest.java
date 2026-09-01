package com.portal.procucev.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    // ---------------------------------------------------------
    // Reflection tests for private helper methods
    // ---------------------------------------------------------
    @Test
    void testHelperMethodsViaReflection() throws Exception {
        Method getIntMethod = AnalyticsServiceImpl.class.getDeclaredMethod("getInt", Map.class, String.class);
        getIntMethod.setAccessible(true);

        Method getLongMethod = AnalyticsServiceImpl.class.getDeclaredMethod("getLong", Map.class, String.class);
        getLongMethod.setAccessible(true);

        Method getDoubleMethod = AnalyticsServiceImpl.class.getDeclaredMethod("getDouble", Map.class, String.class);
        getDoubleMethod.setAccessible(true);

        Method getStringMethod = AnalyticsServiceImpl.class.getDeclaredMethod("getString", Map.class, String.class, String.class);
        getStringMethod.setAccessible(true);

        Method calculateGrowthMethod = AnalyticsServiceImpl.class.getDeclaredMethod("calculateGrowth", int.class, int.class);
        calculateGrowthMethod.setAccessible(true);

        Method formatShareMethod = AnalyticsServiceImpl.class.getDeclaredMethod("formatShare", int.class, int.class);
        formatShareMethod.setAccessible(true);

        Method queryForIntMethod = AnalyticsServiceImpl.class.getDeclaredMethod("queryForInt", String.class, Object[].class);
        queryForIntMethod.setAccessible(true);

        // Test getInt
        assertEquals(0, getIntMethod.invoke(analyticsService, null, "k"));
        assertEquals(0, getIntMethod.invoke(analyticsService, Map.of(), "k"));
        Map<String, Object> mapWithNull = new HashMap<>();
        mapWithNull.put("k", null);
        assertEquals(0, getIntMethod.invoke(analyticsService, mapWithNull, "k"));
        assertEquals(10, getIntMethod.invoke(analyticsService, Map.of("k", 10), "k"));
        assertEquals(1, getIntMethod.invoke(analyticsService, Map.of("k", true), "k"));
        assertEquals(0, getIntMethod.invoke(analyticsService, Map.of("k", false), "k"));
        assertEquals(25, getIntMethod.invoke(analyticsService, Map.of("k", "25.4"), "k"));
        assertEquals(0, getIntMethod.invoke(analyticsService, Map.of("k", "invalid"), "k"));

        // Test getLong
        assertEquals(0L, getLongMethod.invoke(analyticsService, null, "k"));
        assertEquals(0L, getLongMethod.invoke(analyticsService, Map.of(), "k"));
        assertEquals(0L, getLongMethod.invoke(analyticsService, mapWithNull, "k"));
        assertEquals(100L, getLongMethod.invoke(analyticsService, Map.of("k", 100L), "k"));
        assertEquals(1L, getLongMethod.invoke(analyticsService, Map.of("k", true), "k"));
        assertEquals(0L, getLongMethod.invoke(analyticsService, Map.of("k", false), "k"));
        assertEquals(50L, getLongMethod.invoke(analyticsService, Map.of("k", "50.8"), "k"));
        assertEquals(0L, getLongMethod.invoke(analyticsService, Map.of("k", "invalid"), "k"));

        // Test getDouble
        assertEquals(0.0, getDoubleMethod.invoke(analyticsService, null, "k"));
        assertEquals(0.0, getDoubleMethod.invoke(analyticsService, Map.of(), "k"));
        assertEquals(0.0, getDoubleMethod.invoke(analyticsService, mapWithNull, "k"));
        assertEquals(12.5, getDoubleMethod.invoke(analyticsService, Map.of("k", 12.5), "k"));
        assertEquals(1.0, getDoubleMethod.invoke(analyticsService, Map.of("k", true), "k"));
        assertEquals(0.0, getDoubleMethod.invoke(analyticsService, Map.of("k", false), "k"));
        assertEquals(99.9, getDoubleMethod.invoke(analyticsService, Map.of("k", "99.9"), "k"));
        assertEquals(0.0, getDoubleMethod.invoke(analyticsService, Map.of("k", "invalid"), "k"));

        // Test getString
        assertEquals("def", getStringMethod.invoke(analyticsService, null, "k", "def"));
        assertEquals("def", getStringMethod.invoke(analyticsService, Map.of(), "k", "def"));
        assertEquals("def", getStringMethod.invoke(analyticsService, mapWithNull, "k", "def"));
        assertEquals("def", getStringMethod.invoke(analyticsService, Map.of("k", "   "), "k", "def"));
        assertEquals("val", getStringMethod.invoke(analyticsService, Map.of("k", " val "), "k", "def"));

        // Test calculateGrowth
        @SuppressWarnings("unchecked")
        Map<String, Object> g1 = (Map<String, Object>) calculateGrowthMethod.invoke(analyticsService, 0, 0);
        assertEquals("0%", g1.get("change"));
        assertTrue((Boolean) g1.get("isPositive"));

        @SuppressWarnings("unchecked")
        Map<String, Object> g2 = (Map<String, Object>) calculateGrowthMethod.invoke(analyticsService, 10, 0);
        assertEquals("+100%", g2.get("change"));
        assertTrue((Boolean) g2.get("isPositive"));

        @SuppressWarnings("unchecked")
        Map<String, Object> g3 = (Map<String, Object>) calculateGrowthMethod.invoke(analyticsService, 20, 10);
        assertEquals("+100.0%", g3.get("change"));
        assertTrue((Boolean) g3.get("isPositive"));

        @SuppressWarnings("unchecked")
        Map<String, Object> g4 = (Map<String, Object>) calculateGrowthMethod.invoke(analyticsService, 5, 10);
        assertEquals("-50.0%", g4.get("change"));
        assertFalse((Boolean) g4.get("isPositive"));

        // Test formatShare
        assertEquals("0% share", formatShareMethod.invoke(analyticsService, 5, 0));
        assertEquals("0% share", formatShareMethod.invoke(analyticsService, 5, -1));
        assertEquals("50.0% share", formatShareMethod.invoke(analyticsService, 5, 10));

        // Test queryForInt with and without params, null, non-null, and exception
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        assertEquals(1, queryForIntMethod.invoke(analyticsService, "SELECT 1", new Object[0]));

        when(jdbcTemplate.queryForObject(eq("SELECT 2"), eq(Integer.class))).thenReturn(null);
        assertEquals(0, queryForIntMethod.invoke(analyticsService, "SELECT 2", new Object[0]));

        when(jdbcTemplate.queryForObject(eq("SELECT ?"), eq(Integer.class), any(Object[].class))).thenReturn(7);
        assertEquals(7, queryForIntMethod.invoke(analyticsService, "SELECT ?", new Object[]{1}));

        when(jdbcTemplate.queryForObject(eq("SELECT ? null"), eq(Integer.class), any(Object[].class))).thenReturn(null);
        assertEquals(0, queryForIntMethod.invoke(analyticsService, "SELECT ? null", new Object[]{1}));

        when(jdbcTemplate.queryForObject(eq("SELECT empty"), eq(Integer.class))).thenThrow(new EmptyResultDataAccessException(1));
        assertEquals(0, queryForIntMethod.invoke(analyticsService, "SELECT empty", new Object[0]));

        when(jdbcTemplate.queryForObject(eq("SELECT error"), eq(Integer.class))).thenThrow(new RuntimeException("DB error"));
        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> queryForIntMethod.invoke(analyticsService, "SELECT error", new Object[0]));
        assertEquals("DB error", ite.getCause().getMessage());
    }

    // ---------------------------------------------------------
    // getDashboardData Tests
    // ---------------------------------------------------------
    @Test
    void testGetDashboardDataSuccess1() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(10);

        List<Map<String, Object>> topCat = List.of(Map.of("category", "Electronics", "count", 5));
        List<Map<String, Object>> credits = List.of(Map.of("total_credits", 500L, "org_count", 20L));
        List<Map<String, Object>> subRev = List.of(Map.of("total_rev", 150000.0, "active_subs", 10));
        List<Map<String, Object>> sources = List.of(
                Map.of("source_type", "W", "buyers_count", 3, "sellers_count", 2),
                Map.of("source_type", "WEB", "buyers_count", 4, "sellers_count", 5),
                Map.of("source_type", "PARTNER", "buyers_count", 1, "sellers_count", 1)
        );

        when(jdbcTemplate.queryForList(anyString())).thenReturn(topCat, credits, subRev, sources);

        Map<String, Object> result = analyticsService.getDashboardData();
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertTrue(result.containsKey("metrics"));
        assertTrue(result.containsKey("sources"));
        assertTrue(result.containsKey("lifecycleStages"));
    }

    @Test
    void testGetDashboardDataSuccess2EdgeCases() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        List<Map<String, Object>> topCat = Collections.emptyList();
        List<Map<String, Object>> credits = Collections.emptyList();
        List<Map<String, Object>> subRev = List.of(Map.of("total_rev", 5000.0, "active_subs", 2));
        List<Map<String, Object>> sources = Collections.emptyList();

        when(jdbcTemplate.queryForList(anyString())).thenReturn(topCat, credits, subRev, sources);

        Map<String, Object> result = analyticsService.getDashboardData();
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
    }

    @Test
    void testGetDashboardDataSuccess3SmallSubRevAndActiveClamp() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(5)   // totalBuyers
                .thenReturn(2)   // buyerRecent
                .thenReturn(1)   // buyerPrev
                .thenReturn(5)   // totalSellers
                .thenReturn(2)   // sellerRecent
                .thenReturn(1)   // sellerPrev
                .thenReturn(5)   // totalUsers
                .thenReturn(20)  // activeBuyers > totalBuyers (triggers clamping)
                .thenReturn(0)   // inactiveRecent
                .thenReturn(0)   // inactivePrev
                .thenReturn(10)  // totalRfqs
                .thenReturn(3)   // rfqRecent
                .thenReturn(2)   // rfqPrev
                .thenReturn(4)   // rfqsWithQuotes
                .thenReturn(1)   // gmtSubmissions
                .thenReturn(1)   // rfqVendorSubmissions
                .thenReturn(1)   // subRecentGmt
                .thenReturn(1)   // subPrevGmt
                .thenReturn(5)   // rfqsWithin24h
                .thenReturn(2)   // rfqsWithin48h < rfqsWithin24h (triggers clamp)
                .thenReturn(2);  // repeatBuyersCount

        List<Map<String, Object>> topCat = Collections.emptyList();
        List<Map<String, Object>> credits = Collections.emptyList();
        List<Map<String, Object>> subRev = List.of(Map.of("total_rev", 500.0, "active_subs", 1));
        List<Map<String, Object>> sources = Collections.emptyList();

        when(jdbcTemplate.queryForList(anyString())).thenReturn(topCat, credits, subRev, sources);

        Map<String, Object> result = analyticsService.getDashboardData();
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
    }

    @Test
    void testGetDashboardDataException() {
        when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("SQL failure"));

        Map<String, Object> result = analyticsService.getDashboardData();
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    @Test
    void testGetDashboardDataScalarQueryException() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenThrow(new RuntimeException("Database down"));

        Map<String, Object> result = analyticsService.getDashboardData();
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertEquals("Database down", result.get("error"));
        assertNull(result.get("source"));
    }

    // ---------------------------------------------------------
    // getCategoriesData Tests
    // ---------------------------------------------------------
    @Test
    void testGetCategoriesDataSuccess() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(5);

        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("category", "Steel");
        cat1.put("activeRfqs", 12);
        cat1.put("buyersCount", 4);
        cat1.put("totalQty", 1000L);
        cat1.put("avgQty", 250.0);
        cat1.put("avgAmount", 250000.0);

        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("category", "Cement");
        cat2.put("activeRfqs", 4);
        cat2.put("buyersCount", 2);
        cat2.put("totalQty", 500L);
        cat2.put("avgQty", 50.0);
        cat2.put("avgAmount", 5000.0);

        Map<String, Object> cat3 = new HashMap<>();
        cat3.put("category", "Bricks");
        cat3.put("activeRfqs", 1);
        cat3.put("buyersCount", 1);
        cat3.put("totalQty", 100L);
        cat3.put("avgQty", 10.0);
        cat3.put("avgAmount", 0.0);

        Map<String, Object> cat4 = new HashMap<>();
        cat4.put("category", "Pipes");
        cat4.put("activeRfqs", 0);
        cat4.put("buyersCount", 0);
        cat4.put("totalQty", 0L);
        cat4.put("avgQty", 0.0);
        cat4.put("avgAmount", 0.0);

        List<Map<String, Object>> catRows = List.of(cat1, cat2, cat3, cat4);
        List<Map<String, Object>> catVendorRows = List.of(
                Map.of("category", "Steel", "sellersCount", 3),
                Map.of("category", "Cement", "sellersCount", 0),
                Map.of("category", "Bricks", "sellersCount", 0),
                Map.of("category", "Pipes", "sellersCount", 0)
        );

        List<Map<String, Object>> subRows1 = List.of(
                Map.of("description", "TMT Bars", "cnt", 6, "distinctBuyers", 3),
                Map.of("description", "Wire Rods", "cnt", 2, "distinctBuyers", 1),
                Map.of("description", "Sheets", "cnt", 1, "distinctBuyers", 1)
        );
        List<Map<String, Object>> recentRfqs1 = List.of(
                Map.of("rfq_id", "RFQ-101", "quote_count", 2, "quotation_received", 0),
                Map.of("rfq_id", "RFQ-102", "quote_count", 0, "quotation_received", 1),
                Map.of("rfq_id", "RFQ-103", "quote_count", 0, "quotation_received", 0)
        );

        List<Map<String, Object>> subRows2 = List.of(Map.of("description", "OPC Cement", "cnt", 3, "distinctBuyers", 2));
        List<Map<String, Object>> recentRfqs2 = List.of(
                Map.of("rfq_id", "RFQ-201", "quote_count", 0, "quotation_received", 0),
                Map.of("rfq_id", "RFQ-202", "quote_count", 1, "quotation_received", 0)
        );

        List<Map<String, Object>> subRows3 = Collections.emptyList();
        List<Map<String, Object>> recentRfqs3 = Collections.emptyList();

        List<Map<String, Object>> subRows4 = Collections.emptyList();
        List<Map<String, Object>> recentRfqs4 = Collections.emptyList();

        when(jdbcTemplate.queryForList(anyString()))
                .thenReturn(catRows, catVendorRows);

        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(subRows1, recentRfqs1, subRows2, recentRfqs2, subRows3, recentRfqs3, subRows4, recentRfqs4);

        Map<String, Object> result = analyticsService.getCategoriesData();
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertTrue(result.containsKey("categories"));
    }

    @Test
    void testGetCategoriesDataEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList(), Collections.emptyList());

        Map<String, Object> result = analyticsService.getCategoriesData();
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertEquals(0, result.get("totalCount"));
    }

    @Test
    void testGetCategoriesDataException() {
        when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("DB Exception"));

        Map<String, Object> result = analyticsService.getCategoriesData();
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    // ---------------------------------------------------------
    // getFunnelData Tests
    // ---------------------------------------------------------
    @Test
    void testGetFunnelDataSeller() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(10);

        Map<String, Object> result = analyticsService.getFunnelData("seller");
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertTrue(result.containsKey("stages"));
        assertTrue(result.containsKey("summary"));
    }

    @Test
    void testGetFunnelDataSellerQuotedZeroFallback() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(10)  // totalSellers
                .thenReturn(8)   // categoryLinked
                .thenReturn(0)   // quoted initial = 0 triggers fallback
                .thenReturn(5)   // quoted fallback
                .thenReturn(3)   // subs
                .thenReturn(2);  // depletedCreditSellers

        Map<String, Object> result = analyticsService.getFunnelData("seller");
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
    }

    @Test
    void testGetFunnelDataSellerZeroCounts() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        Map<String, Object> result = analyticsService.getFunnelData("seller");
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertTrue(result.containsKey("stages"));
    }

    @Test
    void testGetFunnelDataBuyer() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(20);

        Map<String, Object> result = analyticsService.getFunnelData("buyer");
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertTrue(result.containsKey("stages"));
        assertTrue(result.containsKey("summary"));
    }

    @Test
    void testGetFunnelDataBuyerZeroCounts() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        Map<String, Object> result = analyticsService.getFunnelData("buyer");
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertTrue(result.containsKey("stages"));
    }

    // ---------------------------------------------------------
    // getFunnelStageDetails Tests
    // ---------------------------------------------------------
    @Test
    void testGetFunnelStageDetailsSellerStages() {
        List<Map<String, Object>> mockList = List.of(Map.of("name", "Org1"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(mockList);

        for (int stage = 1; stage <= 5; stage++) {
            Map<String, Object> result = analyticsService.getFunnelStageDetails("seller", stage, "test");
            assertNotNull(result);
            assertEquals(1, result.get("totalRecords"));
        }
    }

    @Test
    void testGetFunnelStageDetailsBuyerStages() {
        List<Map<String, Object>> mockList = List.of(Map.of("name", "User1"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(mockList);

        for (int stage = 1; stage <= 4; stage++) {
            Map<String, Object> result = analyticsService.getFunnelStageDetails("buyer", stage, null);
            assertNotNull(result);
            assertEquals(1, result.get("totalRecords"));
        }

        // Test stageNumber null
        Map<String, Object> resultNullStage = analyticsService.getFunnelStageDetails("buyer", null, "");
        assertNotNull(resultNullStage);
    }

    @Test
    void testGetFunnelStageDetailsException() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenThrow(new RuntimeException("SQL error"));

        Map<String, Object> result = analyticsService.getFunnelStageDetails("seller", 1, "test");
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    // ---------------------------------------------------------
    // getFunnelDropoffDetails Tests
    // ---------------------------------------------------------
    @Test
    void testGetFunnelDropoffDetailsSellerStages() {
        List<Map<String, Object>> mockList = List.of(Map.of("name", "Org1"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(mockList);

        for (int stage = 2; stage <= 4; stage++) {
            Map<String, Object> result = analyticsService.getFunnelDropoffDetails("seller", stage, "test");
            assertNotNull(result);
            assertEquals(1, result.get("totalRecords"));
        }

        // Stage number null default to 2
        Map<String, Object> resNull = analyticsService.getFunnelDropoffDetails("seller", null, null);
        assertNotNull(resNull);
    }

    @Test
    void testGetFunnelDropoffDetailsBuyerStages() {
        List<Map<String, Object>> mockList = List.of(Map.of("name", "User1"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(mockList);

        for (int stage = 2; stage <= 4; stage++) {
            Map<String, Object> result = analyticsService.getFunnelDropoffDetails("buyer", stage, "test");
            assertNotNull(result);
            assertEquals(1, result.get("totalRecords"));
        }
    }

    @Test
    void testGetFunnelDropoffDetailsException() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenThrow(new RuntimeException("Dropoff error"));

        Map<String, Object> result = analyticsService.getFunnelDropoffDetails("buyer", 2, "test");
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    // ---------------------------------------------------------
    // getCalendarData Tests
    // ---------------------------------------------------------
    @Test
    void testGetCalendarDataSuccess() {
        List<Map<String, Object>> dailyRfqs = List.of(
                Map.of("day", 1, "rfqs", 15),
                Map.of("day", 2, "rfqs", 5),
                Map.of("day", 3, "rfqs", 0),
                Map.of("day", 4, "rfqs", 2)
        );
        List<Map<String, Object>> dailyOrgs = List.of(
                Map.of("day", 1, "regB", 5, "regS", 6, "totalCount", 11, "whatsappCount", 5, "webCount", 4, "otherCount", 2),
                Map.of("day", 2, "regB", 15, "regS", 0, "totalCount", 15, "whatsappCount", 0, "webCount", 15, "otherCount", 0),
                Map.of("day", 3, "regB", 0, "regS", 0, "totalCount", 0, "whatsappCount", 0, "webCount", 0, "otherCount", 0),
                Map.of("day", 4, "regB", 2, "regS", 1, "totalCount", 3, "whatsappCount", 1, "webCount", 1, "otherCount", 1)
        );
        List<Map<String, Object>> dailySubs = List.of(Map.of("day", 1, "subs", 2));
        List<Map<String, Object>> dailySubmissions = List.of(
                Map.of("day", 1, "submissions", 6),
                Map.of("day", 3, "submissions", 4)
        );
        List<Map<String, Object>> rfqLogs = List.of(
                Map.of("day", 1, "rfq_id", "RFQ-1", "project_desc", "Desc 1", "created_time", "10:00 AM"),
                Map.of("day", 1, "rfq_id", "RFQ-2", "project_desc", "Desc 2", "created_time", "11:00 AM"),
                Map.of("day", 1, "rfq_id", "RFQ-3", "project_desc", "Desc 3", "created_time", "12:00 PM"),
                Map.of("day", 1, "rfq_id", "RFQ-4", "project_desc", "Desc 4", "created_time", "01:00 PM")
        );

        when(jdbcTemplate.queryForList(anyString(), anyInt(), anyInt()))
                .thenReturn(dailyRfqs, dailyOrgs, dailySubs, dailySubmissions, rfqLogs);

        List<Map<String, Object>> avail = List.of(Map.of("yr", 2026, "mo", 8, "cnt", 50));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(avail);

        Map<String, Object> result = analyticsService.getCalendarData(2026, 8);
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertTrue(result.containsKey("days"));
        assertTrue(result.containsKey("weeks"));
    }

    @Test
    void testGetCalendarDataEdgeDefaultsAndFallbacks() {
        when(jdbcTemplate.queryForList(anyString(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList());

        LocalDate now = LocalDate.now();

        Map<String, Object> result1 = analyticsService.getCalendarData(1990, 15);
        assertNotNull(result1);
        assertEquals(now.getYear(), result1.get("year"));
        assertEquals(now.getMonthValue(), result1.get("monthNumber"));

        Map<String, Object> result2 = analyticsService.getCalendarData(null, null);
        assertNotNull(result2);
        assertEquals(now.getYear(), result2.get("year"));
        assertEquals(now.getMonthValue(), result2.get("monthNumber"));
    }

    @Test
    void testGetCalendarDataException() {
        when(jdbcTemplate.queryForList(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException("Calendar error"));

        Map<String, Object> result = analyticsService.getCalendarData(2026, 8);
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    // ---------------------------------------------------------
    // searchCompanies Tests
    // ---------------------------------------------------------
    @Test
    void testSearchCompaniesWithQueryAndFullData() {
        Map<String, Object> org1 = new HashMap<>();
        org1.put("uuid", "org-1");
        org1.put("organization_name", "Alpha Corp");
        org1.put("email", "alpha@test.com");
        org1.put("organization_phonenumber", "9876543210");
        org1.put("contact_person", "John Doe");
        org1.put("rfq_credits", 10);
        org1.put("rfq_used_count", 2);
        org1.put("created_ts", "2026-08-01 10:00:00");
        org1.put("source_type", "W");
        org1.put("bfs_name", "Enterprise Plan");
        org1.put("org_type_uuid", "3003");
        org1.put("client_vendor", 1);

        Map<String, Object> org2 = new HashMap<>();
        org2.put("uuid", "org-2");
        org2.put("organization_name", "Beta Corp");
        org2.put("email", "beta@test.com");
        org2.put("source_type", "WEB");
        org2.put("subscription_plan_uuid", "plan-uuid-1");
        org2.put("org_type_uuid", "3003");
        org2.put("client_vendor", 0);

        Map<String, Object> org3 = new HashMap<>();
        org3.put("uuid", "org-3");
        org3.put("organization_name", "Gamma Corp");
        org3.put("source_type", "OTHER");
        org3.put("org_type_uuid", "3001");
        org3.put("client_vendor", 0);
        org3.put("subscription_plan_uuid", "missing-plan");

        Map<String, Object> org4 = new HashMap<>();
        org4.put("uuid", "org-4");
        org4.put("organization_name", "Delta Corp");
        org4.put("source_type", "OTHER");
        org4.put("org_type_uuid", "3001");
        org4.put("client_vendor", 0);
        org4.put("created_ts", null);

        List<Map<String, Object>> orgRows = List.of(org1, org2, org3, org4);
        List<Map<String, Object>> planRows = List.of(Map.of("uuid", "plan-uuid-1", "plan_name", "Custom Plan", "subscription_price", 9999.0));
        List<Map<String, Object>> accountRows = List.of(Map.of("org_uuid", "org-1", "name", "Account 1"));

        Map<String, Object> rfqMap1 = new HashMap<>();
        rfqMap1.put("uuid", "rfq-uuid-1");
        rfqMap1.put("org_uuid", "org-1");
        rfqMap1.put("rfq_id", "RFQ-101");
        rfqMap1.put("quote_count", 2);
        rfqMap1.put("quotation_received", 1);
        rfqMap1.put("project_desc", "Requirement 1");
        rfqMap1.put("created_ts", "2026-08-10 10:00:00");
        rfqMap1.put("user", "org-1");

        Map<String, Object> rfqMap2 = new HashMap<>();
        rfqMap2.put("uuid", "rfq-uuid-2");
        rfqMap2.put("rfq_id", "RFQ-102");
        rfqMap2.put("quote_count", 0);
        rfqMap2.put("quotation_received", 0);
        rfqMap2.put("project_desc", "Requirement 2");
        rfqMap2.put("created_ts", null);
        rfqMap2.put("org_uuid", "org-2");
        rfqMap2.put("user", "org-2");

        List<Map<String, Object>> rfqRows = List.of(rfqMap1, rfqMap2);

        List<Map<String, Object>> growthRows = List.of(
            Map.of("org_uuid", "org-1", "user", "org-1", "recent_cnt", 2, "prev_cnt", 1)
        );

        List<Map<String, Object>> itemRows = List.of(
            Map.of("rfq_uuid", "rfq-uuid-1", "category", "Metals", "totalamount", 55000.0)
        );

        Map<String, Object> quoteMap1 = new HashMap<>();
        quoteMap1.put("quote_uuid", "q-1");
        quoteMap1.put("vendor_uuid", "org-1");
        quoteMap1.put("rfq_id", "RFQ-101");
        quoteMap1.put("vendor_name", "Vendor Alpha");
        quoteMap1.put("quote_amount", 54000.0);
        quoteMap1.put("quotation_received", 1);
        quoteMap1.put("sub_date", "15 Aug 2026");
        quoteMap1.put("org_uuid", "org-1");

        Map<String, Object> quoteMap2 = new HashMap<>();
        quoteMap2.put("quote_uuid", "q-2");
        quoteMap2.put("rfq_id", "RFQ-102");
        quoteMap2.put("vendor_name", "Vendor Beta");
        quoteMap2.put("quote_amount", 0.0);
        quoteMap2.put("quotation_received", 0);
        quoteMap2.put("sub_date", "16 Aug 2026");
        quoteMap2.put("vendor_uuid", "org-2");
        quoteMap2.put("org_uuid", "org-2");

        List<Map<String, Object>> quoteRows = List.of(quoteMap1, quoteMap2);
        when(jdbcTemplate.queryForList(contains("FROM organization WHERE organization_name"), any(Object[].class)))
                .thenReturn(orgRows);
        when(jdbcTemplate.queryForList(contains("FROM subscription_plan WHERE uuid IN"), any(Object[].class)))
                .thenReturn(planRows);
        when(jdbcTemplate.queryForList(contains("FROM user WHERE org_uuid IN"), any(Object[].class)))
                .thenReturn(accountRows);
        when(jdbcTemplate.queryForList(contains("GROUP BY org_uuid, user"), any(Object[].class)))
                .thenReturn(growthRows);
        when(jdbcTemplate.queryForList(contains("SELECT uuid, rfq_id, project_desc"), any(Object[].class)))
                .thenReturn(rfqRows);
        when(jdbcTemplate.queryForList(contains("FROM rfq_items WHERE rfq_uuid IN"), any(Object[].class)))
                .thenReturn(itemRows);
        when(jdbcTemplate.queryForList(contains("FROM gmt_rfq_vendors v"), any(Object[].class)))
                .thenReturn(quoteRows);

        Map<String, Object> result = analyticsService.searchCompanies("Alpha");
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertEquals(4, result.get("total"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> companies = (List<Map<String, Object>>) result.get("companies");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> quotes = (List<Map<String, Object>>) companies.get(1).get("quotes");
        assertEquals("₹0", quotes.get(0).get("amount"));
    }

    @Test
    void testSearchCompaniesNullQueryAndEmptyFallbacks() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(Collections.emptyList());

        Map<String, Object> result = analyticsService.searchCompanies(null);
        assertNotNull(result);
        assertEquals("live_database", result.get("source"));
        assertEquals(0, result.get("total"));

        Map<String, Object> resultEmpty = analyticsService.searchCompanies("   ");
        assertNotNull(resultEmpty);
        assertEquals(0, resultEmpty.get("total"));
    }

    @Test
    void testSearchCompaniesException() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenThrow(new RuntimeException("Search error"));

        Map<String, Object> result = analyticsService.searchCompanies("Error");
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    // ---------------------------------------------------------
    // processChat Tests
    // ---------------------------------------------------------
    @Test
    void testProcessChatVariousPrompts() {
        // Null payload
        Map<String, Object> resNull = analyticsService.processChat(null);
        assertNotNull(resNull);
        assertEquals("agent", resNull.get("sender"));

        // Prompt with "rfq"
        Map<String, Object> resRfq = analyticsService.processChat(Map.of("prompt", "I have an rfq requirement", "companyId", "org-1"));
        assertTrue(((String) resRfq.get("text")).contains("matching suppliers for your RFQ"));
        assertEquals("org-1", resRfq.get("companyId"));

        // Prompt with "price" only
        Map<String, Object> resPrice = analyticsService.processChat(Map.of("prompt", "What is the price of steel?"));
        assertTrue(((String) resPrice.get("text")).contains("Supplier quotations have been received"));

        // Prompt with "quote" only
        Map<String, Object> resQuote = analyticsService.processChat(Map.of("prompt", "Can I get an estimate quote?"));
        assertTrue(((String) resQuote.get("text")).contains("Supplier quotations have been received"));

        // Prompt general
        Map<String, Object> resGeneral = analyticsService.processChat(Map.of("prompt", "Hello there"));
        assertTrue(((String) resGeneral.get("text")).contains("Received your message"));
    }
}
