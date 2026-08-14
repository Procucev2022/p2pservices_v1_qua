package com.portal.procucev.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusCodesTest {

    @Test
    void testStatusCodes() {
        StatusCodes statusCodes = new StatusCodes();
        assertNotNull(statusCodes);
        assertEquals("1001", StatusCodes.NEW_VENDOR_CODE);
        assertEquals("1002", StatusCodes.CLIENT_PR_CLOSED_code);
        assertEquals("1003", StatusCodes.SEND_RFQ_CODE);
        assertEquals("1004", StatusCodes.MAIL_SEND_ERROR);
        assertEquals("200", StatusCodes.OK_VENDOR_CODE);
        assertEquals("500", StatusCodes.SERVER_ERROR);
        assertEquals("400", StatusCodes.VALIDATION_FAILED);
    }
}
