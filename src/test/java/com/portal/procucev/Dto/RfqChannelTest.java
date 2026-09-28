package com.portal.procucev.Dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RfqChannelTest {

    @Test
    void testClassifyWhatsApp() {
        assertEquals(RfqChannel.WHATSAPP, RfqChannel.classify("W", "RFQ123"));
        assertEquals(RfqChannel.WHATSAPP, RfqChannel.classify("w", "RFQ123"));
        assertEquals(RfqChannel.WHATSAPP, RfqChannel.classify("WHATSAPP", "RFQ123"));
        assertEquals(RfqChannel.WHATSAPP, RfqChannel.classify("WhatsApp", "RFQ123"));
        assertEquals(RfqChannel.WHATSAPP, RfqChannel.classify("  w  ", "RFQ123"));
        assertEquals(RfqChannel.WHATSAPP, RfqChannel.classify("  WHATSAPP  ", "RFQ123"));
    }

    @Test
    void testClassifyEmail() {
        assertEquals(RfqChannel.EMAIL, RfqChannel.classify("EMAIL", "RFQ123"));
        assertEquals(RfqChannel.EMAIL, RfqChannel.classify("email", "RFQ123"));
        assertEquals(RfqChannel.EMAIL, RfqChannel.classify("E", "RFQ123"));
        assertEquals(RfqChannel.EMAIL, RfqChannel.classify("e", "RFQ123"));
        assertEquals(RfqChannel.EMAIL, RfqChannel.classify("MAIL", "RFQ123"));
        assertEquals(RfqChannel.EMAIL, RfqChannel.classify("mail", "RFQ123"));
        assertEquals(RfqChannel.EMAIL, RfqChannel.classify("  Email  ", "RFQ123"));
    }

    @Test
    void testClassifyWebPortal() {
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify(null, "RFQ262408469792"));
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify("", "RFQ262408469792"));
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify("   ", "RFQ262408469792"));
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify("T", "RFQ262408469792"));
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify("TOOL", "RFQ262408469792"));
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify("WEB", "RFQ262408469792"));
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify("WEB_PORTAL", "RFQ262408469792"));
        assertEquals(RfqChannel.WEB_PORTAL, RfqChannel.classify("OTHER", "RFQ262408469792"));
    }

    @Test
    void testDisplayName() {
        assertEquals("Web Portal", RfqChannel.WEB_PORTAL.getDisplayName());
        assertEquals("WhatsApp", RfqChannel.WHATSAPP.getDisplayName());
        assertEquals("Email", RfqChannel.EMAIL.getDisplayName());
    }
}
