package com.portal.procucev.customexception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

class CustomGlobalHandlerTest {

    private final CustomGlobalHandler handler = new CustomGlobalHandler();

    @Test
    void testCustomException() {
        AppException ex = new AppException(400, "Bad Request", "CustomError", "FAILED");
        ResponseEntity<Object> response = handler.customexception(ex);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(ex, response.getBody());
    }

    @Test
    void testHandleValidationError() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Database constraint error");
        ResponseEntity<AppException> response = handler.handleValidationError(ex);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        AppException body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getErrorCode());
        assertEquals("DATA_ACCESS_ERROR", body.getErrorMessage());
    }
}
