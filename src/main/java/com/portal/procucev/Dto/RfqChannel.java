package com.portal.procucev.Dto;

import java.util.Locale;

public enum RfqChannel {
    WEB_PORTAL("Web Portal"),
    WHATSAPP("WhatsApp"),
    EMAIL("Email");

    private final String displayName;

    RfqChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Classifies an RFQ into one of the three intake channels based on sourceType and rfqId.
     * 1. WhatsApp (WHATSAPP): UPPER(TRIM(source_type)) IN ('W', 'WHATSAPP')
     * 2. Email (EMAIL): UPPER(TRIM(source_type)) IN ('EMAIL', 'E', 'MAIL')
     * 3. Web Portal (WEB_PORTAL): Default channel for all standard portal RFQs.
     *
     * @param sourceType the source_type column value
     * @param rfqId the rfq_id column value
     * @return the classified RfqChannel
     */
    public static RfqChannel classify(String sourceType, String rfqId) {
        if (sourceType != null) {
            String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
            if ("W".equals(normalized) || "WHATSAPP".equals(normalized)) {
                return WHATSAPP;
            }
            if ("EMAIL".equals(normalized) || "E".equals(normalized) || "MAIL".equals(normalized)) {
                return EMAIL;
            }
        }
        return WEB_PORTAL;
    }
}
