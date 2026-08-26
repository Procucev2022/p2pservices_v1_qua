package com.portal.procucev.customexception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void testHandleMethodArgumentNotValid() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("testHandleMethodArgumentNotValid");
        MethodParameter parameter = new MethodParameter(method, -1);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("objectName", "vendorName", "Vendor name is required");
        FieldError error2 = new FieldError("objectName", "phone1", "Phone must be 10 digits");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        HttpHeaders headers = new HttpHeaders();
        HttpStatusCode status = HttpStatus.BAD_REQUEST;
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, headers, status, request);
        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof MessageResponse);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertEquals("Validation failed", messageResponse.getMessage());
        assertEquals(2, messageResponse.getErrorMsg().size());
        assertTrue(messageResponse.getErrorMsg().contains("vendorName: Vendor name is required"));
        assertTrue(messageResponse.getErrorMsg().contains("phone1: Phone must be 10 digits"));
    }
}
