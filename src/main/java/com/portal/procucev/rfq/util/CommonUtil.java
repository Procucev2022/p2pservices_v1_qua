package com.portal.procucev.rfq.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class CommonUtil {

    private CommonUtil() {
    }

    public static String generateUniqueRfqNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "RFQ-" + timestamp + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String shortenRfqNumber(String fullRfqNumber) {
        if (fullRfqNumber == null || fullRfqNumber.isBlank()) {
            return "";
        }
        String clean = fullRfqNumber.trim().replaceAll("^[✉️📧🌐\\s]+", "").trim();
        return clean;
    }

    public static String formatRfqDisplayNumber(String fullRfqNumber) {
        String shortened = shortenRfqNumber(fullRfqNumber);
        if (shortened.isBlank()) {
            return "";
        }
        return "📧 " + shortened;
    }

    public static String formatRfqDisplayNumber(String fullRfqNumber, String sourceType) {
        String shortened = shortenRfqNumber(fullRfqNumber);
        if (shortened.isBlank()) {
            return "";
        }
        if ("W".equalsIgnoreCase(sourceType) || "WEB".equalsIgnoreCase(sourceType)) {
            return "🌐 " + shortened;
        }
        return "📧 " + shortened;
    }

    public static String getRfqIcon(String fullRfqNumber, String sourceType) {
        String shortened = shortenRfqNumber(fullRfqNumber);
        if (shortened.isBlank()) {
            return "";
        }
        if ("W".equalsIgnoreCase(sourceType) || "WEB".equalsIgnoreCase(sourceType)) {
            if (shortened.startsWith("RFQ-")) {
                return "📧";
            }
            return "🌐";
        }
        return "📧";
    }

    public static String getRfqIcon(String fullRfqNumber) {
        return getRfqIcon(fullRfqNumber, null);
    }

    public static String formatRfqDisplayId(String fullRfqNumber) {
        return formatRfqDisplayNumber(fullRfqNumber);
    }

    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String defaultIfBlank(String str, String defaultValue) {
        return isNullOrBlank(str) ? defaultValue : str.trim();
    }
}
