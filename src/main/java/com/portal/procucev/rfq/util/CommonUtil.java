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
        String clean = fullRfqNumber.trim().replaceAll("^[✉️🌐\\s]+", "").trim();
        if (clean.matches("(?i)^RFQ-\\d{14}-[a-z0-9_-]{1,12}$")) {
            return clean.substring(0, clean.lastIndexOf('-'));
        }
        return clean;
    }

    public static String formatRfqDisplayNumber(String fullRfqNumber) {
        String shortened = shortenRfqNumber(fullRfqNumber);
        if (shortened.isBlank()) {
            return "";
        }
        return "✉️ " + shortened;
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
