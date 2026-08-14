package com.portal.procucev.rfq;

import com.portal.procucev.rfq.util.CommonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

public class CommonUtilTest {

    @Test
    @DisplayName("Test generateUniqueRfqNumber returns non-null valid format")
    void testGenerateUniqueRfqNumber() {
        String rfqNumber = CommonUtil.generateUniqueRfqNumber();
        assertNotNull(rfqNumber);
        assertTrue(rfqNumber.startsWith("RFQ-"));
    }

    @Test
    @DisplayName("Test shortenRfqNumber strips 8-char suffix")
    void testShortenRfqNumber() {
        assertEquals("RFQ-20260813153514", CommonUtil.shortenRfqNumber("RFQ-20260813153514-bbc27322"));
        assertEquals("RFQ-20260813153514", CommonUtil.shortenRfqNumber("RFQ-20260813153514"));
        assertEquals("RFQ-101", CommonUtil.shortenRfqNumber("RFQ-101"));
        assertEquals("", CommonUtil.shortenRfqNumber(null));
        assertEquals("", CommonUtil.shortenRfqNumber("   "));
    }

    @Test
    @DisplayName("Test formatRfqDisplayNumber adds email icon and strips 8-char suffix")
    void testFormatRfqDisplayNumber() {
        assertEquals("✉️ RFQ-20260813162614", CommonUtil.formatRfqDisplayNumber("RFQ-20260813162614-5ababe44"));
        assertEquals("✉️ RFQ-20260813162547", CommonUtil.formatRfqDisplayNumber("RFQ-20260813162547-ffe67f9f"));
        assertEquals("✉️ RFQ-20260813153514", CommonUtil.formatRfqDisplayNumber("RFQ-20260813153514-bbc27322"));
        assertEquals("✉️ RFQ-101", CommonUtil.formatRfqDisplayNumber("RFQ-101"));
        assertEquals("", CommonUtil.formatRfqDisplayNumber(null));
        assertEquals("", CommonUtil.formatRfqDisplayNumber("   "));

        // Also verify formatRfqDisplayId
        assertEquals("✉️ RFQ-20260813162614", CommonUtil.formatRfqDisplayId("RFQ-20260813162614-5ababe44"));
        assertEquals("✉️ RFQ-20260813162547", CommonUtil.formatRfqDisplayId("RFQ-20260813162547-ffe67f9f"));
        assertEquals("✉️ RFQ-20260813153514", CommonUtil.formatRfqDisplayId("RFQ-20260813153514-bbc27322"));
    }

    @Test
    @DisplayName("Test isNullOrBlank")
    void testIsNullOrBlank() {
        assertTrue(CommonUtil.isNullOrBlank(null));
        assertTrue(CommonUtil.isNullOrBlank(""));
        assertTrue(CommonUtil.isNullOrBlank("   "));
        assertFalse(CommonUtil.isNullOrBlank("valid"));
    }

    @Test
    @DisplayName("Test defaultIfBlank")
    void testDefaultIfBlank() {
        assertEquals("default", CommonUtil.defaultIfBlank(null, "default"));
        assertEquals("default", CommonUtil.defaultIfBlank("", "default"));
        assertEquals("default", CommonUtil.defaultIfBlank("   ", "default"));
        assertEquals("value", CommonUtil.defaultIfBlank(" value ", "default"));
    }

    @Test
    @DisplayName("Test private constructor execution")
    void testPrivateConstructor() throws Exception {
        Constructor<CommonUtil> constructor = CommonUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        CommonUtil instance = constructor.newInstance();
        assertNotNull(instance);
    }
}
