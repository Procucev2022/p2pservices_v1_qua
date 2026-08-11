package com.portal.procucev.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberUtilsTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<PhoneNumberUtils> constructor = PhoneNumberUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        PhoneNumberUtils instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void testNormalize() {
        assertNull(PhoneNumberUtils.normalize(null));
        assertNull(PhoneNumberUtils.normalize(""));
        assertNull(PhoneNumberUtils.normalize("   "));
        assertNull(PhoneNumberUtils.normalize("abc"));

        assertEquals("+919876543210", PhoneNumberUtils.normalize("+919876543210"));
        assertEquals("+919876543210", PhoneNumberUtils.normalize("919876543210"));
        assertEquals("+919876543210", PhoneNumberUtils.normalize("9876543210"));
        assertEquals("+19876543210", PhoneNumberUtils.normalize("+19876543210"));
        assertEquals("+123456789012", PhoneNumberUtils.normalize("123456789012"));
        assertEquals("+12345", PhoneNumberUtils.normalize("12345"));
        assertEquals("+9198765432100", PhoneNumberUtils.normalize("+9198765432100"));
        assertEquals("+889876543210", PhoneNumberUtils.normalize("889876543210"));
    }
}
