package com.portal.procucev.service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int getInt(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object obj = map.get(key);
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof Boolean) return ((Boolean) obj) ? 1 : 0;
        try {
            return (int) Double.parseDouble(obj.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private long getLong(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0L;
        Object obj = map.get(key);
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof Boolean) return ((Boolean) obj) ? 1L : 0L;
        try {
            return (long) Double.parseDouble(obj.toString().trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private double getDouble(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0.0;
        Object obj = map.get(key);
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof Boolean) return ((Boolean) obj) ? 1.0 : 0.0;
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        if (map == null || !map.containsKey(key)) return defaultVal;
        Object obj = map.get(key);
        return (obj != null && !obj.toString().trim().isEmpty()) ? obj.toString().trim() : defaultVal;
    }

    private Map<String, Object> calculateGrowth(int current, int previous) {
        Map<String, Object> res = new HashMap<>();
        if (previous == 0) {
            if (current == 0) {
                res.put("change", "0%");
                res.put("isPositive", true);
                return res;
            }
            res.put("change", "+100%");
            res.put("isPositive", true);
            return res;
        }
        double diff = current - previous;
        double pct = Math.round((diff / (double) previous) * 1000.0) / 10.0;
        String sign = pct >= 0 ? "+" : "";
        res.put("change", sign + pct + "%");
        res.put("isPositive", pct >= 0);
        return res;
    }

    @Override
    public Map<String, Object> getDashboardData() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Buyers & Sellers counts
            int totalBuyers = queryForInt("SELECT count(*) FROM organization WHERE org_type_uuid = '3001' OR client_vendor = 0");
            int buyerRecent = queryForInt("SELECT count(*) FROM organization WHERE (org_type_uuid = '3001' OR client_vendor = 0) AND created_ts >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            int buyerPrev = queryForInt("SELECT count(*) FROM organization WHERE (org_type_uuid = '3001' OR client_vendor = 0) AND created_ts >= DATE_SUB(NOW(), INTERVAL 60 DAY) AND created_ts < DATE_SUB(NOW(), INTERVAL 30 DAY)");

            int totalSellers = queryForInt("SELECT count(*) FROM organization WHERE org_type_uuid = '3003' OR client_vendor = 1");
            int sellerRecent = queryForInt("SELECT count(*) FROM organization WHERE (org_type_uuid = '3003' OR client_vendor = 1) AND created_ts >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            int sellerPrev = queryForInt("SELECT count(*) FROM organization WHERE (org_type_uuid = '3003' OR client_vendor = 1) AND created_ts >= DATE_SUB(NOW(), INTERVAL 60 DAY) AND created_ts < DATE_SUB(NOW(), INTERVAL 30 DAY)");

            int totalUsers = queryForInt("SELECT count(*) FROM user");
            int activeBuyers = queryForInt("SELECT count(*) FROM user WHERE is_active = 1");
            int inactiveBuyers = Math.max(0, totalUsers - activeBuyers);
            int inactiveRecent = queryForInt("SELECT count(*) FROM user WHERE (is_active = 0 OR is_active IS NULL) AND created_ts >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            int inactivePrev = queryForInt("SELECT count(*) FROM user WHERE (is_active = 0 OR is_active IS NULL) AND created_ts >= DATE_SUB(NOW(), INTERVAL 60 DAY) AND created_ts < DATE_SUB(NOW(), INTERVAL 30 DAY)");

            // 2. RFQ counts & period growth
            int totalRfqs = queryForInt("SELECT count(*) FROM rfq_header");
            int rfqRecent = queryForInt("SELECT count(*) FROM rfq_header WHERE created_ts >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            int rfqPrev = queryForInt("SELECT count(*) FROM rfq_header WHERE created_ts >= DATE_SUB(NOW(), INTERVAL 60 DAY) AND created_ts < DATE_SUB(NOW(), INTERVAL 30 DAY)");

            int openRfqs = queryForInt("SELECT count(*) FROM rfq_header WHERE quote_count = 0");
            int noQuoteCount = queryForInt("SELECT count(*) FROM rfq_header WHERE quote_count = 0 AND (quotation_received = 0 OR quotation_received IS NULL)");
            int stagnantCount = queryForInt("SELECT count(*) FROM rfq_header WHERE quote_count = 0 AND created_ts <= DATE_SUB(NOW(), INTERVAL 48 HOUR)");
            int awardedRfqs = queryForInt("SELECT count(*) FROM rfq_header WHERE quote_count > 0 OR quotation_received = 1");
            int evalRfqs = queryForInt("SELECT count(*) FROM rfq_header WHERE quote_count > 0 AND quotation_received = 0");
            int draftRfqs = Math.max(0, totalRfqs - openRfqs - awardedRfqs - evalRfqs);
            int repeatBuyersCount = queryForInt("SELECT count(*) FROM (SELECT user, count(*) as cnt FROM rfq_header GROUP BY user HAVING cnt > 1) as t");

            int repeatPercent = totalUsers > 0 ? (int) Math.round(((double) repeatBuyersCount / totalUsers) * 100) : 0;
            int activePercent = totalBuyers > 0 ? (int) Math.round(((double) activeBuyers / totalBuyers) * 100) : 0;

            // 3. Top category
            String topCategoryName = "None";
            int topCategoryCount = 0;
            List<Map<String, Object>> topCatRows = jdbcTemplate.queryForList(
                "SELECT category, count(*) as count FROM rfq_items WHERE category IS NOT NULL AND category != '' AND LOWER(category) NOT IN ('other', 'others') GROUP BY category ORDER BY count DESC LIMIT 1"
            );
            if (!topCatRows.isEmpty()) {
                topCategoryName = getString(topCatRows.get(0), "category", "None");
                topCategoryCount = getInt(topCatRows.get(0), "count");
            }

            // 4. Credits & Subscriptions Revenue
            List<Map<String, Object>> creditRows = jdbcTemplate.queryForList(
                "SELECT COALESCE(SUM(rfq_credits), 0) as total_credits, count(CASE WHEN rfq_credits > 0 THEN 1 END) as org_count FROM organization"
            );
            long totalCredits = !creditRows.isEmpty() ? getLong(creditRows.get(0), "total_credits") : 0L;
            long affectedOrgs = !creditRows.isEmpty() ? getLong(creditRows.get(0), "org_count") : 0L;

            List<Map<String, Object>> subRevRows = jdbcTemplate.queryForList(
                "SELECT COALESCE(SUM(p.subscription_price), 0) as total_rev, count(o.uuid) as active_subs " +
                "FROM organization o " +
                "LEFT JOIN subscription_plan p ON o.subscription_plan_uuid = p.uuid " +
                "WHERE o.subscription_plan_uuid IS NOT NULL OR o.bfs_name IS NOT NULL"
            );
            double totalSubRev = !subRevRows.isEmpty() ? getDouble(subRevRows.get(0), "total_rev") : 0.0;
            String sellerSubs = totalSubRev >= 100000.0
                ? String.format("₹%.1fL", totalSubRev / 100000.0)
                : totalSubRev >= 1000.0
                ? String.format("₹%.1fk", totalSubRev / 1000.0)
                : String.format("₹%.0f", totalSubRev);

            int subRecent = queryForInt("SELECT count(*) FROM organization WHERE (subscription_plan_uuid IS NOT NULL OR bfs_name IS NOT NULL) AND created_ts >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            int subPrev = queryForInt("SELECT count(*) FROM organization WHERE (subscription_plan_uuid IS NOT NULL OR bfs_name IS NOT NULL) AND created_ts >= DATE_SUB(NOW(), INTERVAL 60 DAY) AND created_ts < DATE_SUB(NOW(), INTERVAL 30 DAY)");

            Map<String, Object> buyerGrowth = calculateGrowth(buyerRecent, buyerPrev);
            Map<String, Object> sellerGrowth = calculateGrowth(sellerRecent, sellerPrev);
            Map<String, Object> inactiveGrowth = calculateGrowth(inactiveRecent, inactivePrev);
            Map<String, Object> rfqGrowth = calculateGrowth(rfqRecent, rfqPrev);
            Map<String, Object> subGrowth = calculateGrowth(subRecent, subPrev);

            // 5. Source distribution per channel
            List<Map<String, Object>> sourceBreakdown = jdbcTemplate.queryForList(
                "SELECT " +
                "  COALESCE(source_type, 'WEB') as source_type, " +
                "  SUM(CASE WHEN org_type_uuid = '3001' OR client_vendor = 0 THEN 1 ELSE 0 END) as buyers_count, " +
                "  SUM(CASE WHEN org_type_uuid = '3003' OR client_vendor = 1 THEN 1 ELSE 0 END) as sellers_count, " +
                "  count(*) as total_count " +
                "FROM organization GROUP BY source_type"
            );

            int wBuyers = 0, wSellers = 0;
            int webBuyers = 0, webSellers = 0;
            int otherBuyers = 0, otherSellers = 0;

            for (Map<String, Object> s : sourceBreakdown) {
                String st = getString(s, "source_type", "WEB");
                int bc = getInt(s, "buyers_count");
                int sc = getInt(s, "sellers_count");
                if ("W".equalsIgnoreCase(st)) {
                    wBuyers += bc;
                    wSellers += sc;
                } else if ("WEB".equalsIgnoreCase(st)) {
                    webBuyers += bc;
                    webSellers += sc;
                } else {
                    otherBuyers += bc;
                    otherSellers += sc;
                }
            }

            int safeTotalBuyers = Math.max(totalBuyers, 1);
            int safeTotalSellers = Math.max(totalSellers, 1);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalBuyers", Map.of("value", totalBuyers, "change", buyerGrowth.get("change"), "isPositive", buyerGrowth.get("isPositive")));
            metrics.put("totalSellers", Map.of("value", totalSellers, "change", sellerGrowth.get("change"), "isPositive", sellerGrowth.get("isPositive")));
            metrics.put("activeBuyers", Map.of("value", activeBuyers, "percentOfTotal", activePercent + "% of Total"));
            metrics.put("inactiveBuyers", Map.of("value", inactiveBuyers, "change", inactiveGrowth.get("change")));
            metrics.put("totalRfqs", Map.of("value", totalRfqs, "change", rfqGrowth.get("change"), "isPositive", rfqGrowth.get("isPositive")));
            metrics.put("openRfqs", Map.of("value", openRfqs, "tag", openRfqs > 0 ? "Active in market" : "All quoted"));
            metrics.put("newRfqs", Map.of("value", rfqRecent, "period", "Last 30 Days"));
            metrics.put("rfqsWithoutQuotes", Map.of("value", noQuoteCount, "tag", noQuoteCount + " RFQs pending initial quote", "isAlert", noQuoteCount > 0));
            metrics.put("sellerSubs", Map.of("value", sellerSubs, "change", subGrowth.get("change"), "isPositive", subGrowth.get("isPositive")));
            metrics.put("pendingCredits", Map.of("value", totalCredits, "tag", affectedOrgs + " orgs with balance"));
            metrics.put("repeatBuyers", Map.of("value", repeatPercent + "%", "tag", repeatBuyersCount + " active repeat buyers"));
            metrics.put("topCategory", Map.of(
                "name", topCategoryName,
                "volumePercent", totalRfqs > 0 ? ((int) Math.round(((double) topCategoryCount / totalRfqs) * 100)) + "% of Vol" : "0%"
            ));

            List<Map<String, Object>> sources = List.of(
                Map.of("channel", "WhatsApp Bot Channel (W)", "buyersPercent", (int) Math.round(((double) wBuyers / safeTotalBuyers) * 100), "sellersPercent", (int) Math.round(((double) wSellers / safeTotalSellers) * 100)),
                Map.of("channel", "Web Portal Direct (WEB)", "buyersPercent", (int) Math.round(((double) webBuyers / safeTotalBuyers) * 100), "sellersPercent", (int) Math.round(((double) webSellers / safeTotalSellers) * 100)),
                Map.of("channel", "Referral & Partner Ingestion", "buyersPercent", (int) Math.round(((double) otherBuyers / safeTotalBuyers) * 100), "sellersPercent", (int) Math.round(((double) otherSellers / safeTotalSellers) * 100))
            );

            String draftShare = totalRfqs > 0 ? String.format("%.1f%% share", ((double) draftRfqs / totalRfqs) * 100.0) : "0% share";
            String openShare = totalRfqs > 0 ? String.format("%.1f%% share", ((double) openRfqs / totalRfqs) * 100.0) : "0% share";
            String evalShare = totalRfqs > 0 ? String.format("%.1f%% share", ((double) evalRfqs / totalRfqs) * 100.0) : "0% share";
            String awardedShare = totalRfqs > 0 ? String.format("%.1f%% share", ((double) awardedRfqs / totalRfqs) * 100.0) : "0% share";

            List<Map<String, Object>> lifecycleStages = List.of(
                Map.of("stage", "Draft / Incomplete", "volume", draftRfqs, "trend", draftShare, "isPositive", true, "colorDot", "bg-[#0058be]"),
                Map.of("stage", "Open for Bidding", "volume", openRfqs, "trend", openShare, "isPositive", true, "colorDot", "bg-[#2170e4]"),
                Map.of("stage", "Under Evaluation", "volume", evalRfqs, "trend", evalShare, "isPositive", true, "colorDot", "bg-[#b7c8e1]"),
                Map.of("stage", "Awarded / Closed", "volume", awardedRfqs, "trend", awardedShare, "isPositive", true, "colorDot", "bg-[#191c1e]")
            );

            response.put("metrics", metrics);
            response.put("sources", sources);
            response.put("lifecycleStages", lifecycleStages);
            response.put("source", "live_database");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> getCategoriesData() {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer totalCats = queryForInt("SELECT count(distinct category) FROM rfq_items WHERE category IS NOT NULL AND category != '' AND LOWER(category) NOT IN ('other', 'others')");
            Integer activeBuyers = queryForInt("SELECT count(*) FROM user WHERE is_active = 1");
            Integer regSellers = queryForInt("SELECT count(*) FROM organization WHERE org_type_uuid = '3003' OR client_vendor = 1");
            Integer openRfqs = queryForInt("SELECT count(*) FROM rfq_header WHERE quote_count = 0");

            List<Map<String, Object>> catRows = jdbcTemplate.queryForList(
                "SELECT i.category, " +
                "count(distinct i.rfq_uuid) as activeRfqs, " +
                "count(distinct r.user) as buyersCount, " +
                "COALESCE(SUM(CAST(i.quantity AS DECIMAL(10,2))), 0) as totalQty, " +
                "COALESCE(AVG(NULLIF(CAST(i.quantity AS DECIMAL(10,2)), 0)), 0) as avgQty, " +
                "COALESCE(AVG(NULLIF(i.totalamount, 0)), 0) as avgAmount " +
                "FROM rfq_items i " +
                "LEFT JOIN rfq_header r ON i.rfq_uuid = r.uuid " +
                "WHERE i.category IS NOT NULL AND i.category != '' AND LOWER(i.category) NOT IN ('other', 'others') " +
                "GROUP BY i.category ORDER BY activeRfqs DESC LIMIT 12"
            );

            Map<String, Integer> sellerCategoryMap = new HashMap<>();
            List<Map<String, Object>> catVendorRows = jdbcTemplate.queryForList(
                "SELECT i.category, count(distinct v.vendor_uuid) as sellersCount " +
                "FROM rfq_items i " +
                "JOIN gmt_rfq_vendors v ON i.rfq_uuid = v.rfq_uuid " +
                "WHERE i.category IS NOT NULL AND i.category != '' " +
                "GROUP BY i.category"
            );
            for (Map<String, Object> cv : catVendorRows) {
                sellerCategoryMap.put(getString(cv, "category", ""), getInt(cv, "sellersCount"));
            }

            List<Map<String, Object>> categories = new ArrayList<>();
            for (int i = 0; i < catRows.size(); i++) {
                Map<String, Object> row = catRows.get(i);
                String catName = getString(row, "category", "Category");
                int activeRfqCount = getInt(row, "activeRfqs");
                int catBuyers = getInt(row, "buyersCount");
                int catSellers = sellerCategoryMap.getOrDefault(catName, 0);
                double avgAmount = getDouble(row, "avgAmount");
                double avgQty = getDouble(row, "avgQty");
                long totalQty = getLong(row, "totalQty");

                List<Map<String, Object>> subRows = jdbcTemplate.queryForList(
                    "SELECT i.description, count(*) as cnt, count(distinct r.user) as distinctBuyers " +
                    "FROM rfq_items i " +
                    "LEFT JOIN rfq_header r ON i.rfq_uuid = r.uuid " +
                    "WHERE i.category = ? AND i.description IS NOT NULL AND i.description != '' " +
                    "GROUP BY i.description ORDER BY cnt DESC LIMIT 4",
                    catName
                );

                List<Map<String, Object>> recentRfqRows = jdbcTemplate.queryForList(
                    "SELECT DISTINCT r.rfq_id, r.quote_count, r.quotation_received " +
                    "FROM rfq_items i " +
                    "JOIN rfq_header r ON i.rfq_uuid = r.uuid " +
                    "WHERE i.category = ? AND r.rfq_id IS NOT NULL AND r.rfq_id != '' " +
                    "ORDER BY r.created_ts DESC LIMIT 3",
                    catName
                );

                List<Map<String, Object>> realRecentRfqs = new ArrayList<>();
                for (Map<String, Object> rfq : recentRfqRows) {
                    int qc = getInt(rfq, "quote_count");
                    int qr = getInt(rfq, "quotation_received");
                    String rfqIdVal = getString(rfq, "rfq_id", "RFQ");
                    realRecentRfqs.add(Map.of(
                        "id", rfqIdVal,
                        "status", (qc > 0 || qr == 1) ? "Quotes Received" : "Open for Bids"
                    ));
                }

                List<Map<String, Object>> subcategories = new ArrayList<>();
                List<String> subDescriptions = new ArrayList<>();
                for (int j = 0; j < subRows.size(); j++) {
                    Map<String, Object> s = subRows.get(j);
                    String desc = getString(s, "description", catName + " Item");
                    int cnt = getInt(s, "cnt");
                    int subBuyers = getInt(s, "distinctBuyers");
                    subDescriptions.add(desc);

                    String demandStatus = cnt >= 5 ? "High Demand" : cnt >= 2 ? "Growing" : "Stable";
                    int subSellers = catSellers > 0 ? Math.min(catSellers, Math.max(1, cnt)) : 0;

                    subcategories.add(Map.of(
                        "id", "sub-" + i + "-" + j,
                        "name", desc,
                        "demandStatus", demandStatus,
                        "buyers", subBuyers,
                        "sellers", subSellers,
                        "rfqs", cnt,
                        "recentRfqs", realRecentRfqs.isEmpty() ? List.of() : realRecentRfqs.subList(0, Math.min(realRecentRfqs.size(), 2))
                    ));
                }

                String avgValStr = avgAmount > 0
                    ? (avgAmount >= 100000.0 ? String.format("₹%.1fL", avgAmount / 100000.0) : String.format("₹%.1fk", avgAmount / 1000.0))
                    : avgQty > 0 ? Math.round(avgQty) + " Units" : activeRfqCount + " RFQs";

                String status = activeRfqCount >= 10 ? "High Vol" : activeRfqCount >= 3 ? "Active" : "Stable";

                Map<String, Object> catObj = new HashMap<>();
                catObj.put("id", "cat-" + (i + 1));
                catObj.put("name", catName);
                catObj.put("subDescription", String.join(", ", subDescriptions));
                catObj.put("buyers", catBuyers);
                catObj.put("sellers", catSellers);
                catObj.put("activeRfqs", activeRfqCount);
                catObj.put("avgValue", avgValStr);
                catObj.put("totalUnits", totalQty);
                catObj.put("status", status);
                catObj.put("subcategories", subcategories);

                categories.add(catObj);
            }

            Map<String, Object> stats = Map.of(
                "totalCategories", categories.size() > 0 ? categories.size() : totalCats,
                "activeBuyers", activeBuyers,
                "registeredSellers", regSellers,
                "openRfqs", openRfqs
            );

            response.put("categories", categories);
            response.put("totalCount", categories.size());
            response.put("stats", stats);
            response.put("source", "live_database");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> getFunnelData(String type) {
        Map<String, Object> response = new HashMap<>();
        try {
            if ("seller".equalsIgnoreCase(type)) {
                int totalSellers = queryForInt("SELECT count(*) FROM organization WHERE org_type_uuid = '3003' OR client_vendor = 1");
                int catalogue = queryForInt("SELECT count(distinct vendor_uuid) FROM gmt_rfq_vendors");
                int quoted = queryForInt("SELECT count(distinct vendor_uuid) FROM gmt_rfq_vendors WHERE quote_submitted_date IS NOT NULL");
                int subs = queryForInt("SELECT count(*) FROM organization WHERE subscription_plan_uuid IS NOT NULL OR bfs_name IS NOT NULL");
                subs = Math.min(quoted, subs);

                int drop1 = Math.max(0, totalSellers - catalogue);
                int drop2 = Math.max(0, catalogue - quoted);
                int drop3 = Math.max(0, quoted - subs);

                List<Map<String, Object>> stages = List.of(
                    Map.of("stageNumber", 1, "name", "Seller Onboarded", "usersEntered", totalSellers, "dropOffVolume", 0, "dropOffRate", "0%", "convRatePrev", "100%", "convRateTotal", "100%", "icon", "store"),
                    Map.of("stageNumber", 2, "name", "Catalog Linked", "usersEntered", catalogue, "dropOffVolume", drop1, "dropOffRate", totalSellers > 0 ? Math.round(((double) drop1 / totalSellers) * 100) + "%" : "0%", "convRatePrev", totalSellers > 0 ? Math.round(((double) catalogue / totalSellers) * 100) + "%" : "0%", "convRateTotal", totalSellers > 0 ? Math.round(((double) catalogue / totalSellers) * 100) + "%" : "0%", "icon", "inventory_2"),
                    Map.of("stageNumber", 3, "name", "First Quote Placed", "usersEntered", quoted, "dropOffVolume", drop2, "dropOffRate", catalogue > 0 ? Math.round(((double) drop2 / catalogue) * 100) + "%" : "0%", "convRatePrev", catalogue > 0 ? Math.round(((double) quoted / catalogue) * 100) + "%" : "0%", "convRateTotal", totalSellers > 0 ? Math.round(((double) quoted / totalSellers) * 100) + "%" : "0%", "icon", "request_quote"),
                    Map.of("stageNumber", 4, "name", "Subscription Subscribed", "usersEntered", subs, "dropOffVolume", drop3, "dropOffRate", quoted > 0 ? Math.round(((double) drop3 / quoted) * 100) + "%" : "0%", "convRatePrev", quoted > 0 ? Math.round(((double) subs / quoted) * 100) + "%" : "0%", "convRateTotal", totalSellers > 0 ? Math.round(((double) subs / totalSellers) * 100) + "%" : "0%", "isGoal", true, "icon", "workspace_premium")
                );

                response.put("stages", stages);
                response.put("summary", Map.of(
                    "totalEntered", totalSellers,
                    "totalConverted", subs,
                    "conversionRate", totalSellers > 0 ? Math.round(((double) subs / totalSellers) * 100) + "%" : "0%"
                ));
            } else {
                int totalUsers = queryForInt("SELECT count(*) FROM user");
                int active = queryForInt("SELECT count(*) FROM user WHERE is_active = 1");
                int rfqUsers = queryForInt("SELECT count(distinct user) FROM rfq_header");
                int repeat = queryForInt("SELECT count(*) FROM (SELECT user, count(*) as cnt FROM rfq_header GROUP BY user HAVING cnt > 1) as t");

                int drop1 = Math.max(0, totalUsers - active);
                int drop2 = Math.max(0, active - rfqUsers);
                int drop3 = Math.max(0, rfqUsers - repeat);

                List<Map<String, Object>> stages = List.of(
                    Map.of("stageNumber", 1, "name", "Registered", "usersEntered", totalUsers, "dropOffVolume", 0, "dropOffRate", "0%", "convRatePrev", "100%", "convRateTotal", "100%", "icon", "person"),
                    Map.of("stageNumber", 2, "name", "Active Users", "usersEntered", active, "dropOffVolume", drop1, "dropOffRate", totalUsers > 0 ? Math.round(((double) drop1 / totalUsers) * 100) + "%" : "0%", "convRatePrev", totalUsers > 0 ? Math.round(((double) active / totalUsers) * 100) + "%" : "0%", "convRateTotal", totalUsers > 0 ? Math.round(((double) active / totalUsers) * 100) + "%" : "0%", "icon", "manage_search"),
                    Map.of("stageNumber", 3, "name", "RFQ Created", "usersEntered", rfqUsers, "dropOffVolume", drop2, "dropOffRate", active > 0 ? Math.round(((double) drop2 / active) * 100) + "%" : "0%", "convRatePrev", active > 0 ? Math.round(((double) rfqUsers / active) * 100) + "%" : "0%", "convRateTotal", totalUsers > 0 ? Math.round(((double) rfqUsers / totalUsers) * 100) + "%" : "0%", "icon", "description"),
                    Map.of("stageNumber", 4, "name", "Repeat Buyer", "usersEntered", repeat, "dropOffVolume", drop3, "dropOffRate", rfqUsers > 0 ? Math.round(((double) drop3 / rfqUsers) * 100) + "%" : "0%", "convRatePrev", rfqUsers > 0 ? Math.round(((double) repeat / rfqUsers) * 100) + "%" : "0%", "convRateTotal", totalUsers > 0 ? Math.round(((double) repeat / totalUsers) * 100) + "%" : "0%", "isGoal", true, "icon", "autorenew")
                );

                response.put("stages", stages);
                response.put("summary", Map.of(
                    "totalEntered", totalUsers,
                    "totalConverted", repeat,
                    "conversionRate", totalUsers > 0 ? Math.round(((double) repeat / totalUsers) * 100) + "%" : "0%"
                ));
            }
            response.put("source", "live_database");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> getCalendarData(Integer year, Integer month) {
        Map<String, Object> response = new HashMap<>();
        try {
            int y = (year != null && year > 2000) ? year : 2026;
            int m = (month != null && month >= 1 && month <= 12) ? month : 8;

            LocalDate firstDay = LocalDate.of(y, m, 1);
            String monthName = firstDay.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + y;
            int totalDays = firstDay.lengthOfMonth();
            int startDayOffset = firstDay.getDayOfWeek().getValue() % 7; // Sunday = 0

            List<Map<String, Object>> dailyRfqs = jdbcTemplate.queryForList(
                "SELECT DAY(created_ts) as day, count(*) as rfqs FROM rfq_header WHERE YEAR(created_ts) = ? AND MONTH(created_ts) = ? GROUP BY DAY(created_ts)",
                y, m
            );

            List<Map<String, Object>> dailyOrgs = jdbcTemplate.queryForList(
                "SELECT " +
                "  DAY(created_ts) as day, " +
                "  SUM(CASE WHEN org_type_uuid = '3001' OR client_vendor = 0 THEN 1 ELSE 0 END) as regB, " +
                "  SUM(CASE WHEN org_type_uuid = '3003' OR client_vendor = 1 THEN 1 ELSE 0 END) as regS, " +
                "  SUM(CASE WHEN source_type = 'W' THEN 1 ELSE 0 END) as whatsappCount, " +
                "  SUM(CASE WHEN source_type = 'WEB' OR source_type IS NULL THEN 1 ELSE 0 END) as webCount, " +
                "  SUM(CASE WHEN source_type NOT IN ('W', 'WEB') AND source_type IS NOT NULL THEN 1 ELSE 0 END) as otherCount, " +
                "  count(*) as totalCount " +
                "FROM organization " +
                "WHERE YEAR(created_ts) = ? AND MONTH(created_ts) = ? " +
                "GROUP BY DAY(created_ts)",
                y, m
            );

            List<Map<String, Object>> dailySubs = jdbcTemplate.queryForList(
                "SELECT DAY(created_ts) as day, count(*) as subs " +
                "FROM organization " +
                "WHERE YEAR(created_ts) = ? AND MONTH(created_ts) = ? AND (subscription_plan_uuid IS NOT NULL OR bfs_name IS NOT NULL) " +
                "GROUP BY DAY(created_ts)",
                y, m
            );

            List<Map<String, Object>> rfqLogs = jdbcTemplate.queryForList(
                "SELECT DAY(created_ts) as day, rfq_id, project_desc, DATE_FORMAT(created_ts, '%H:%i %p') as created_time " +
                "FROM rfq_header " +
                "WHERE YEAR(created_ts) = ? AND MONTH(created_ts) = ? AND rfq_id IS NOT NULL " +
                "ORDER BY created_ts DESC",
                y, m
            );

            List<Map<String, Object>> availRows = jdbcTemplate.queryForList(
                "SELECT YEAR(created_ts) as yr, MONTH(created_ts) as mo, count(*) as cnt FROM rfq_header WHERE created_ts IS NOT NULL GROUP BY YEAR(created_ts), MONTH(created_ts) ORDER BY yr DESC, mo DESC"
            );

            List<Map<String, Object>> availableMonths = new ArrayList<>();
            for (Map<String, Object> row : availRows) {
                int yr = getInt(row, "yr");
                int mo = getInt(row, "mo");
                LocalDate ld = LocalDate.of(yr, mo, 1);
                availableMonths.add(Map.of(
                    "year", yr,
                    "month", mo,
                    "label", ld.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + yr
                ));
            }

            List<Map<String, Object>> days = new ArrayList<>();
            for (int i = 1; i <= totalDays; i++) {
                final int currentDay = i;
                int rfqs = 0;
                for (Map<String, Object> r : dailyRfqs) {
                    if (getInt(r, "day") == currentDay) {
                        rfqs = getInt(r, "rfqs");
                        break;
                    }
                }

                int regB = 0, regS = 0, totalReg = 0;
                int whatsappReg = 0, webReg = 0, otherReg = 0;
                for (Map<String, Object> o : dailyOrgs) {
                    if (getInt(o, "day") == currentDay) {
                        regB = getInt(o, "regB");
                        regS = getInt(o, "regS");
                        totalReg = getInt(o, "totalCount");
                        whatsappReg = getInt(o, "whatsappCount");
                        webReg = getInt(o, "webCount");
                        otherReg = getInt(o, "otherCount");
                        break;
                    }
                }

                int subs = 0;
                for (Map<String, Object> s : dailySubs) {
                    if (getInt(s, "day") == currentDay) {
                        subs = getInt(s, "subs");
                        break;
                    }
                }

                String statusLevel = (rfqs > 10 || totalReg > 10) ? "high" : (rfqs == 0 && totalReg == 0) ? "low" : "avg";
                int totalSource = whatsappReg + webReg + otherReg;
                int whatsappPct = totalSource > 0 ? (int) Math.round(((double) whatsappReg / totalSource) * 100) : 0;
                int webPct = totalSource > 0 ? (int) Math.round(((double) webReg / totalSource) * 100) : 0;
                int partnerPct = totalSource > 0 ? (int) Math.round(((double) otherReg / totalSource) * 100) : 0;

                List<Map<String, Object>> dayLogs = new ArrayList<>();
                for (Map<String, Object> log : rfqLogs) {
                    if (getInt(log, "day") == currentDay) {
                        String timeStr = getString(log, "created_time", "12:00 PM");
                        String rfqId = getString(log, "rfq_id", "RFQ");
                        String desc = getString(log, "project_desc", "RFQ initiated");
                        dayLogs.add(Map.of(
                            "time", timeStr,
                            "text", rfqId + ": " + desc,
                            "type", "rfq"
                        ));
                        if (dayLogs.size() >= 3) break;
                    }
                }

                if (dayLogs.isEmpty()) {
                    String fallbackText = rfqs > 0 ? rfqs + " RFQs logged in database for this date."
                        : (totalReg > 0 ? totalReg + " new organizations registered." : "No activity recorded on this day.");
                    dayLogs.add(Map.of(
                        "time", "—",
                        "text", fallbackText,
                        "type", "rfq"
                    ));
                }

                days.add(Map.of(
                    "day", i,
                    "dateStr", firstDay.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + String.format("%02d", i) + ", " + y,
                    "regB", regB,
                    "regS", regS,
                    "rfqs", rfqs,
                    "subs", subs,
                    "statusLevel", statusLevel,
                    "sources", List.of(
                        Map.of("source", "WhatsApp Bot (W)", "percent", whatsappPct, "colorClass", "bg-[#0058be]"),
                        Map.of("source", "Web Portal Direct", "percent", webPct, "colorClass", "bg-[#2170e4]"),
                        Map.of("source", "Partner Ingestion", "percent", partnerPct, "colorClass", "bg-[#b7c8e1]")
                    ),
                    "activityLogs", dayLogs
                ));
            }

            response.put("month", monthName);
            response.put("year", y);
            response.put("monthNumber", m);
            response.put("totalDays", totalDays);
            response.put("startDayOffset", startDayOffset);
            response.put("days", days);
            response.put("availableMonths", availableMonths);
            response.put("source", "live_database");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> searchCompanies(String query) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sql = "SELECT uuid, organization_name, email, organization_phonenumber, contact_person, rfq_credits, rfq_used_count, created_ts, source_type, bfs_name, subscription_plan_uuid FROM organization WHERE organization_name IS NOT NULL AND organization_name != ''";
            List<Object> params = new ArrayList<>();
            if (query != null && !query.trim().isEmpty()) {
                sql += " AND (organization_name LIKE ? OR email LIKE ? OR organization_phonenumber LIKE ? OR contact_person LIKE ?)";
                String term = "%" + query.trim() + "%";
                params.add(term);
                params.add(term);
                params.add(term);
                params.add(term);
            }
            sql += " ORDER BY created_ts DESC LIMIT 20";

            List<Map<String, Object>> orgRows = jdbcTemplate.queryForList(sql, params.toArray());
            List<Map<String, Object>> companies = new ArrayList<>();

            for (Map<String, Object> org : orgRows) {
                String uuid = getString(org, "uuid", "");
                String orgName = getString(org, "organization_name", "Organization");
                String email = getString(org, "email", "");
                String phone = getString(org, "organization_phonenumber", "");
                String contact = getString(org, "contact_person", orgName);
                String sourceType = getString(org, "source_type", "WEB");
                int credits = getInt(org, "rfq_credits");
                String tier = org.get("bfs_name") != null ? "Enterprise Tier" : (org.get("subscription_plan_uuid") != null ? "Pro Tier" : "Growth Tier");

                // Real RFQs for this organization
                List<Map<String, Object>> rfqRows = jdbcTemplate.queryForList(
                    "SELECT uuid, rfq_id, project_desc, created_ts, quote_count, quotation_received FROM rfq_header WHERE org_uuid = ? OR user = ? ORDER BY created_ts DESC LIMIT 10",
                    uuid, uuid
                );

                List<Map<String, Object>> rfqs = new ArrayList<>();
                for (Map<String, Object> r : rfqRows) {
                    String rfqUuid = getString(r, "uuid", "");
                    String rfqId = getString(r, "rfq_id", "");
                    int qc = getInt(r, "quote_count");
                    int qr = getInt(r, "quotation_received");

                    List<Map<String, Object>> itemRows = jdbcTemplate.queryForList(
                        "SELECT category, totalamount FROM rfq_items WHERE rfq_uuid = ? LIMIT 1",
                        rfqUuid
                    );
                    String itemCat = !itemRows.isEmpty() ? getString(itemRows.get(0), "category", "General Procurement") : "General Procurement";
                    double itemAmt = !itemRows.isEmpty() ? getDouble(itemRows.get(0), "totalamount") : 0.0;
                    String valStr = itemAmt > 0 ? String.format("₹%,.0f", itemAmt) : "RFQ Pending Value";

                    String projDesc = getString(r, "project_desc", itemCat + " Requirement");
                    String crDate = r.get("created_ts") != null ? r.get("created_ts").toString().substring(0, Math.min(10, r.get("created_ts").toString().length())) : "";

                    rfqs.add(Map.of(
                        "id", !rfqId.isEmpty() ? rfqId : "RFQ-LIVE",
                        "title", projDesc,
                        "createdDate", crDate,
                        "status", (qc > 0 || qr == 1) ? "Quotes Received" : "Open for Bidding",
                        "category", itemCat,
                        "value", valStr
                    ));
                }

                // Real 30-day RFQ growth for this org
                int recentOrgRfqs = queryForInt("SELECT count(*) FROM rfq_header WHERE (org_uuid = ? OR user = ?) AND created_ts >= DATE_SUB(NOW(), INTERVAL 30 DAY)", uuid, uuid);
                int prevOrgRfqs = queryForInt("SELECT count(*) FROM rfq_header WHERE (org_uuid = ? OR user = ?) AND created_ts >= DATE_SUB(NOW(), INTERVAL 60 DAY) AND created_ts < DATE_SUB(NOW(), INTERVAL 30 DAY)", uuid, uuid);
                Map<String, Object> orgGrowth = calculateGrowth(recentOrgRfqs, prevOrgRfqs);

                // Real user account count for this org
                int orgUsers = queryForInt("SELECT count(*) FROM user WHERE org_uuid = ?", uuid);
                int totalAccounts = Math.max(orgUsers, 1);

                // Real quotes submitted if vendor
                List<Map<String, Object>> quotesRows = jdbcTemplate.queryForList(
                    "SELECT " +
                    "  v.uuid as quote_uuid, " +
                    "  COALESCE(r.rfq_id, 'RFQ') as rfq_id, " +
                    "  COALESCE(o.organization_name, 'Vendor') as vendor_name, " +
                    "  (SELECT COALESCE(SUM(i.totalamount), 0) FROM rfq_items i WHERE i.rfq_uuid = v.rfq_uuid) as quote_amount, " +
                    "  v.quote_submitted_date, " +
                    "  v.quotation_received " +
                    "FROM gmt_rfq_vendors v " +
                    "LEFT JOIN rfq_header r ON v.rfq_uuid = r.uuid " +
                    "LEFT JOIN organization o ON v.vendor_uuid = o.uuid " +
                    "WHERE v.vendor_uuid = ? AND v.quote_submitted_date IS NOT NULL " +
                    "ORDER BY v.quote_submitted_date DESC LIMIT 5",
                    uuid
                );
                List<Map<String, Object>> quotes = new ArrayList<>();
                for (int qIdx = 0; qIdx < quotesRows.size(); qIdx++) {
                    Map<String, Object> q = quotesRows.get(qIdx);
                    String rfqIdStr = getString(q, "rfq_id", "RFQ");
                    String vName = getString(q, "vendor_name", orgName);
                    double qAmt = getDouble(q, "quote_amount");
                    int qr = getInt(q, "quotation_received");
                    String subDate = q.get("quote_submitted_date") != null ? q.get("quote_submitted_date").toString().substring(0, Math.min(10, q.get("quote_submitted_date").toString().length())) : "Submitted";
                    quotes.add(Map.of(
                        "id", "QT-" + rfqIdStr,
                        "rfqId", rfqIdStr,
                        "vendorName", vName,
                        "amount", qAmt > 0 ? String.format("₹%,.0f", qAmt) : "N/A",
                        "submittedDate", subDate,
                        "status", qr == 1 ? "Accepted" : "Submitted"
                    ));
                }

                // Real comments from rfq_comments
                List<Map<String, Object>> commentRows = jdbcTemplate.queryForList(
                    "SELECT uuid, comment, commented_user, created_ts FROM rfq_comments " +
                    "WHERE rfq_uuid IN (SELECT uuid FROM rfq_header WHERE org_uuid = ? OR user = ?) " +
                    "ORDER BY created_ts DESC LIMIT 5",
                    uuid, uuid
                );
                List<Map<String, Object>> chatMessages = new ArrayList<>();
                for (Map<String, Object> c : commentRows) {
                    String commentId = getString(c, "uuid", "msg-" + uuid);
                    String uName = getString(c, "commented_user", orgName.substring(0, Math.min(orgName.length(), 2)).toUpperCase());
                    String commentText = getString(c, "comment", "Active on platform.");
                    String cTime = c.get("created_ts") != null ? c.get("created_ts").toString().substring(0, Math.min(16, c.get("created_ts").toString().length())) : "";
                    chatMessages.add(Map.of(
                        "id", commentId,
                        "sender", "client",
                        "senderName", uName,
                        "text", commentText,
                        "time", cTime
                    ));
                }

                Map<String, Object> comp = new HashMap<>();
                comp.put("id", uuid);
                comp.put("name", orgName);
                comp.put("tier", tier);
                comp.put("isVerified", true);
                comp.put("pocName", contact);
                comp.put("pocRole", "Primary Contact");
                comp.put("email", email);
                comp.put("phone", phone);
                comp.put("source", "W".equalsIgnoreCase(sourceType) ? "WhatsApp Ingestion" : "Web Portal");
                comp.put("stats", Map.of(
                    "totalRfqs", rfqs.size(),
                    "rfqsChange", orgGrowth.get("change") + " in 30d",
                    "totalAccounts", totalAccounts,
                    "totalCredits", credits,
                    "currentTier", tier,
                    "renewsDate", "Active"
                ));
                comp.put("rfqs", rfqs);
                comp.put("quotes", quotes);
                comp.put("chatMessages", chatMessages);

                companies.add(comp);
            }

            response.put("companies", companies);
            response.put("total", companies.size());
            response.put("source", "live_database");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, Object> processChat(Map<String, Object> requestPayload) {
        Map<String, Object> response = new HashMap<>();
        String prompt = requestPayload != null && requestPayload.get("prompt") != null ? String.valueOf(requestPayload.get("prompt")) : "";
        String companyId = requestPayload != null && requestPayload.get("companyId") != null ? String.valueOf(requestPayload.get("companyId")) : "default";

        String replyText = "Received your message: \"" + prompt + "\". Our procurement team will assist you shortly.";
        if (prompt.toLowerCase().contains("rfq")) {
            replyText = "We found matching suppliers for your RFQ requirements. Quotes are being processed.";
        } else if (prompt.toLowerCase().contains("price") || prompt.toLowerCase().contains("quote")) {
            replyText = "Supplier quotations have been received and are ready for comparison in your dashboard.";
        }

        response.put("id", "msg-" + System.currentTimeMillis());
        response.put("companyId", companyId);
        response.put("sender", "agent");
        response.put("senderName", "Procucev Bot");
        response.put("text", replyText);
        response.put("time", "Just now");
        return response;
    }

    private Integer queryForInt(String sql, Object... params) {
        try {
            Integer val;
            if (params != null && params.length > 0) {
                val = jdbcTemplate.queryForObject(sql, Integer.class, params);
            } else {
                val = jdbcTemplate.queryForObject(sql, Integer.class);
            }
            return val != null ? val : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
