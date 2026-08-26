package com.portal.procucev.rfq.util;

public final class CommonUtil {

    private CommonUtil() {
    }

    public static String shortenRfqNumber(String fullRfqNumber) {
        if (fullRfqNumber == null || fullRfqNumber.isBlank()) {
            return "";
        }
        String clean = fullRfqNumber.trim().replaceAll("^[✉️🌐\\s]+", "").trim();
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
