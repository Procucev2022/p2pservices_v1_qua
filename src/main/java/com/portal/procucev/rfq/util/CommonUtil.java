package com.portal.procucev.rfq.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class CommonUtil {

    private CommonUtil() {
    }

    public static String generateUniqueRfqNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "RFQ-" + timestamp;
    }

    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String defaultIfBlank(String str, String defaultValue) {
        return isNullOrBlank(str) ? defaultValue : str.trim();
    }
}
