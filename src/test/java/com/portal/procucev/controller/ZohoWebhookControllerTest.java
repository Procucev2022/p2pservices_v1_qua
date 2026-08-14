package com.portal.procucev.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.ZohoPaymentLinkWebhookRequest;
import com.portal.procucev.service.ZohoWebhookService;
import com.portal.procucev.service.ZohoWebhookSignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZohoWebhookControllerTest {

    @Mock
    private ZohoWebhookService zohoWebhookService;

    @Mock
    private ZohoWebhookSignatureUtil verifier;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ZohoWebhookController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "expectedAccountId", 12345L);
    }

    @Test
    void testHandleWebhook_InvalidSignature() throws Exception {
        when(request.getHeader("X-Zoho-Webhook-Signature")).thenReturn("invalid_sig");
        when(verifier.verifyZohoSignature("invalid_sig", "{}")).thenReturn(false);

        ResponseEntity<String> response = controller.handleWebhook(request, "{}");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testHandleWebhook_InvalidAccount() throws Exception {
        when(request.getHeader("X-Zoho-Webhook-Signature")).thenReturn("valid_sig");
        when(verifier.verifyZohoSignature("valid_sig", "{}")).thenReturn(true);

        ZohoPaymentLinkWebhookRequest req = new ZohoPaymentLinkWebhookRequest();
        req.setAccount_id(999L);
        when(objectMapper.readValue("{}", ZohoPaymentLinkWebhookRequest.class)).thenReturn(req);

        ResponseEntity<String> response = controller.handleWebhook(request, "{}");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testHandleWebhook_Success() throws Exception {
        when(request.getHeader("X-Zoho-Webhook-Signature")).thenReturn("valid_sig");
        when(verifier.verifyZohoSignature("valid_sig", "{}")).thenReturn(true);

        ZohoPaymentLinkWebhookRequest req = new ZohoPaymentLinkWebhookRequest();
        req.setAccount_id(12345L);
        when(objectMapper.readValue("{}", ZohoPaymentLinkWebhookRequest.class)).thenReturn(req);

        ResponseEntity<String> response = controller.handleWebhook(request, "{}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
