package com.portal.procucev.controller;

import com.portal.procucev.Dto.PaymentLinkGenerateRequest;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.model.PaymentLink;
import com.portal.procucev.service.PaymentLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentLinkControllerTest {

    @Mock
    private PaymentLinkService paymentLinkService;

    @InjectMocks
    private PaymentLinkController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "redirectUrl", "https://test.com");
    }

    @Test
    void testGeneratePaymentLink_NullPlanId() {
        PaymentLinkGenerateRequest req = new PaymentLinkGenerateRequest();
        assertThrows(AppException.class, () -> controller.generatePaymentLink(req));
    }

    @Test
    void testGeneratePaymentLink_NullUserPhone() {
        PaymentLinkGenerateRequest req = new PaymentLinkGenerateRequest();
        req.setPlanId(1L);
        assertThrows(AppException.class, () -> controller.generatePaymentLink(req));
    }

    @Test
    void testGeneratePaymentLink_EmptyUserPhone() {
        PaymentLinkGenerateRequest req = new PaymentLinkGenerateRequest();
        req.setPlanId(1L);
        req.setUserPhone("");
        assertThrows(AppException.class, () -> controller.generatePaymentLink(req));
    }

    @Test
    void testGeneratePaymentLink_NullUserEmail() {
        PaymentLinkGenerateRequest req = new PaymentLinkGenerateRequest();
        req.setPlanId(1L);
        req.setUserPhone("9876543210");
        assertThrows(AppException.class, () -> controller.generatePaymentLink(req));
    }

    @Test
    void testGeneratePaymentLink_EmptyUserEmail() {
        PaymentLinkGenerateRequest req = new PaymentLinkGenerateRequest();
        req.setPlanId(1L);
        req.setUserPhone("9876543210");
        req.setUserEmail("");
        assertThrows(AppException.class, () -> controller.generatePaymentLink(req));
    }

    @Test
    void testGeneratePaymentLink_InvalidEmail() {
        PaymentLinkGenerateRequest req = new PaymentLinkGenerateRequest();
        req.setPlanId(1L);
        req.setUserPhone("9876543210");
        req.setUserEmail("invalid-email");
        assertThrows(AppException.class, () -> controller.generatePaymentLink(req));
    }

    @Test
    void testGeneratePaymentLink_Success() {
        PaymentLinkGenerateRequest req = new PaymentLinkGenerateRequest();
        req.setPlanId(1L);
        req.setUserPhone("9876543210");
        req.setUserEmail("user@gmail.com");

        PaymentLink expected = new PaymentLink();
        when(paymentLinkService.createPaymentLink(eq(1L), anyString(), eq("user@gmail.com"), anyString()))
                .thenReturn(expected);

        PaymentLink actual = controller.generatePaymentLink(req);
        assertEquals(expected, actual);
    }
}
