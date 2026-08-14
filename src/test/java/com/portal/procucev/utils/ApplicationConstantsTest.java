package com.portal.procucev.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationConstantsTest {

    @Test
    void testConstants() {
        assertEquals("Success", ApplicationConstants.SUCCESS);
        assertEquals("Failure", ApplicationConstants.FAILURE);
        assertEquals("CLIENT", ApplicationConstants.BUYER);
        assertEquals("VENDOR", ApplicationConstants.VENDOR);
        assertNotNull(ApplicationConstants.CLIENT_GRANT);
        assertNotNull(ApplicationConstants.CLIENT_AUTHORITY);
        assertNotNull(ApplicationConstants.CLIENT_SCOPE);
    }
}
