package com.portal.procucev.rfq;

import com.portal.procucev.rfq.util.CommonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
