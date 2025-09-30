package com.portal.procucev.utils;

public class PhoneNumberUtils {

    private static final String DEFAULT_COUNTRY_CODE = "+91";

    private PhoneNumberUtils() {
        // prevent instantiation
    }

    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        // Keep only digits
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.isBlank()) {
            return null;
        }

        // Case 1: already 12-digit with 91 (e.g., 919876543210)
        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        }

        // Case 2: only 10-digit local number (e.g., 9876543210)
        if (digitsOnly.length() == 10) {
            return "+91" + digitsOnly;
        }

        // Case 3: already in correct format (e.g., +919876543210) → no change
        if (phone.startsWith("+91") && digitsOnly.length() == 12) {
            return phone;
        }

        // Fallback: return digits with +
        return "+" + digitsOnly;
    }

}
