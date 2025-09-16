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

        // Remove spaces, dashes, brackets, etc.
        String digitsOnly = phone.replaceAll("[^0-9]", "");

        if (digitsOnly.isBlank()) {
            return null;
        }

        // Case 1: already starts with country code "91" but missing "+"
        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        }

        // Case 2: only 10-digit local number
        if (digitsOnly.length() == 10) {
            return DEFAULT_COUNTRY_CODE + digitsOnly;
        }

        // Case 3: already in correct format (+91XXXXXXXXXX)
        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return DEFAULT_COUNTRY_CODE + digitsOnly.substring(2);
        }

        // Case 4: fallback — return with + if missing
        if (!digitsOnly.startsWith("91")) {
            return DEFAULT_COUNTRY_CODE + digitsOnly;
        }

        return "+" + digitsOnly;
    }
}
