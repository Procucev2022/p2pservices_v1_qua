package com.portal.procucev.customexception;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AppExceptionTest {

    @Test
    void testAppExceptionConstructorsAndGetters() {
        AppException ex1 = new AppException(500, "Error Msg", "BusinessException", "FAILURE");
        assertEquals(500, ex1.getErrorCode());
        assertEquals("Error Msg", ex1.getErrorMessage());
        assertEquals("BusinessException", ex1.getExceptiontype());
        assertEquals("FAILURE", ex1.getStatus());
        assertNotNull(ex1.toString());
        assertNotNull(ex1.fillInStackTrace());

        AppException ex2 = new AppException(500, "Error Msg", "BusinessException", "FAILURE", LocalDateTime.now());
        assertEquals(500, ex2.getErrorCode());

        AppException ex3 = new AppException("500", "Error Msg", "Type", "FAIL");
        assertEquals("500", ex3.getStatusCode());

        AppException ex4 = new AppException(400, "Bad Request", "Validation");
        assertEquals(400, ex4.getErrorCode());
        assertEquals("Bad Request", ex4.getErrorMessage());
        assertEquals("Validation", ex4.getExceptiontype());

        AppException ex5 = new AppException("Simple Message");
        assertEquals("Simple Message", ex5.getErrorMessage());

        AppException ex6 = new AppException(404, "Not Found");
        assertEquals(404, ex6.getErrorCode());
        assertEquals("Not Found", ex6.getErrorMessage());

        ex1.setErrorCode(200);
        ex1.setErrorMessage("OK");
        ex1.setExceptiontype("None");
        ex1.setStatus("SUCCESS");
        ex1.setStatusCode("200");

        assertEquals(200, ex1.getErrorCode());
        assertEquals("OK", ex1.getErrorMessage());
        assertEquals("None", ex1.getExceptiontype());
        assertEquals("SUCCESS", ex1.getStatus());
        assertEquals("200", ex1.getStatusCode());
    }
}
