package com.portal.procucev.controller;

import com.portal.procucev.model.Organization;
import com.portal.procucev.service.SmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileValidationControllerTest {

    @Mock
    private SmsService smsService;

    @InjectMocks
    private MobileValidationController controller;

    @Test
    void testSendOtpMobile() {
        Organization org = new Organization();
        when(smsService.sendOtpToMobile(org)).thenReturn(ResponseEntity.ok("OTP sent"));

        ResponseEntity<String> response = controller.sendOtpMobile(org);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP sent", response.getBody());
    }

    @Test
    void testValidateOtp_Valid() {
        Organization org = new Organization();
        when(smsService.validateMobileOtp(org)).thenReturn(true);

        ResponseEntity<String> response = controller.validateOtp(org);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP is valid", response.getBody());
    }

    @Test
    void testValidateOtp_Invalid() {
        Organization org = new Organization();
        when(smsService.validateMobileOtp(org)).thenReturn(false);

        ResponseEntity<String> response = controller.validateOtp(org);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid or expired OTP", response.getBody());
    }
}
