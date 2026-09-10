package com.portal.procucev.rfq;

import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.controller.BuyerProfileCompletionController;
import com.portal.procucev.rfq.dto.ApiResponse;
import com.portal.procucev.rfq.dto.BuyerOtpRequest;
import com.portal.procucev.rfq.dto.BuyerProfileCompletionRequest;
import com.portal.procucev.rfq.entity.BuyerEntity;
import com.portal.procucev.rfq.repository.BuyerRepository;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.service.PendingRfqResumeService;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.service.SmsService;
import com.portal.procucev.utils.StatusConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerProfileCompletionControllerTest {

    @Mock
    private UserDao userDao;

    @Mock
    private ClientDao clientDao;

    @Mock
    private PincodeDao pincodeDao;

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private EmailTransactionRepository emailTransactionRepository;

    @Mock
    private SelfRegistrationService selfRegistrationService;

    @Mock
    private SmsService smsService;

    @Mock
    private PendingRfqResumeService pendingRfqResumeService;

    @InjectMocks
    private BuyerProfileCompletionController controller;

    @Test
    @DisplayName("getBuyerStatus: returns buyer data when found")
    void testGetBuyerStatus_Found() {
        User user = new User();
        user.setUsername("buyer@example.com");
        user.setFullName("Buyer One");
        user.setPhone(StatusConstants.DEMO_PHONE_NUMBER);
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);

        when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);
        when(emailTransactionRepository.findBySenderEmailIgnoreCaseAndStatus("buyer@example.com", StatusConstants.PENDING_BUYER_REGISTRATION))
                .thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.getBuyerStatus("buyer@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("buyer@example.com", response.getBody().getData().get("email"));
        assertTrue((Boolean) response.getBody().getData().get("isDemoPhone"));
    }

    @Test
    @DisplayName("getBuyerStatus: returns 404 when buyer not found")
    void testGetBuyerStatus_NotFound() {
        when(userDao.findByUsernameAndActive("missing@example.com", true)).thenReturn(null);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.getBuyerStatus("missing@example.com");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    @DisplayName("sendEmailOtp: dispatches OTP to buyer email")
    void testSendEmailOtp_Success() {
        User user = new User();
        user.setUsername("buyer@example.com");
        user.setPhone(StatusConstants.DEMO_PHONE_NUMBER);
        when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);
        when(selfRegistrationService.generateEmailOtp(eq("buyer@example.com"), any(), eq(StatusConstants.DEMO_PHONE_NUMBER)))
                .thenReturn(true);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BuyerOtpRequest request = BuyerOtpRequest.builder().email("buyer@example.com").build();

        ResponseEntity<ApiResponse<String>> response = controller.sendEmailOtp(request, mockRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    @DisplayName("verifyEmailOtp: verifies valid email OTP")
    void testVerifyEmailOtp_Success() {
        User user = new User();
        user.setUsername("buyer@example.com");
        user.setPhone(StatusConstants.DEMO_PHONE_NUMBER);
        when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);
        when(selfRegistrationService.isEmailOtpValid(any(Organization.class))).thenReturn(true);

        BuyerOtpRequest request = BuyerOtpRequest.builder()
                .email("buyer@example.com")
                .otp("123456")
                .build();

        ResponseEntity<ApiResponse<String>> response = controller.verifyEmailOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(selfRegistrationService).removeEmailOtp(any(Organization.class));
    }

    @Test
    @DisplayName("sendPhoneOtp: rejects demo phone number")
    void testSendPhoneOtp_RejectsDemoPhone() {
        BuyerOtpRequest request = BuyerOtpRequest.builder()
                .email("buyer@example.com")
                .phone(StatusConstants.DEMO_PHONE_NUMBER)
                .build();

        ResponseEntity<ApiResponse<String>> response = controller.sendPhoneOtp(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        verifyNoInteractions(smsService);
    }

    @Test
    @DisplayName("verifyPhoneOtp: updates phone and sets PHONE_VERIFIED")
    void testVerifyPhoneOtp_Success() {
        User user = new User();
        user.setUsername("buyer@example.com");
        user.setPhone(StatusConstants.DEMO_PHONE_NUMBER);

        Organization org = new Organization();
        user.setOrg(org);

        when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);
        when(smsService.validateMobileOtp(any(Organization.class))).thenReturn(true);

        BuyerOtpRequest request = BuyerOtpRequest.builder()
                .email("buyer@example.com")
                .phone("9876543210")
                .otp("654321")
                .build();

        ResponseEntity<ApiResponse<String>> response = controller.verifyPhoneOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("+919876543210", user.getPhone());
        assertEquals(StatusConstants.PHONE_VERIFIED, user.getVerificationStatus());
        verify(userDao).save(user);
    }

    @Test
    @DisplayName("completeProfile: rejects invalid pincode")
    void testCompleteProfile_InvalidPincode() {
        User user = new User();
        user.setUsername("buyer@example.com");
        when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);
        when(pincodeDao.existsByPincode("999999")).thenReturn(false);

        BuyerProfileCompletionRequest request = BuyerProfileCompletionRequest.builder()
                .email("buyer@example.com")
                .phone("9876543210")
                .pincode("999999")
                .build();

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.completeProfile(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Invalid delivery pincode"));
        verifyNoInteractions(pendingRfqResumeService);
    }

    @Test
    @DisplayName("completeProfile: updates profile, sets PROFILE_COMPLETED, resumes pending RFQs")
    void testCompleteProfile_Success() {
        User user = new User();
        user.setUsername("buyer@example.com");
        user.setPhone("9876543210");

        Organization org = new Organization();
        org.setId("ORG-1");
        user.setOrg(org);

        PincodeData pincodeData = new PincodeData();
        pincodeData.setPincode("560001");
        pincodeData.setCity("Bengaluru");
        pincodeData.setState("Karnataka");

        when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);
        when(pincodeDao.existsByPincode("560001")).thenReturn(true);
        when(pincodeDao.findByPincode("560001")).thenReturn(pincodeData);
        when(buyerRepository.findByEmailIgnoreCase("buyer@example.com")).thenReturn(Optional.empty());
        when(pendingRfqResumeService.resumePendingRfqs("buyer@example.com")).thenReturn(2);

        BuyerProfileCompletionRequest request = BuyerProfileCompletionRequest.builder()
                .email("buyer@example.com")
                .fullName("John Doe")
                .companyName("Acme Corp")
                .phone("9876543210")
                .address1("MG Road")
                .pincode("560001")
                .build();

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.completeProfile(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(StatusConstants.PROFILE_COMPLETED, user.getVerificationStatus());
        assertEquals("Bengaluru", org.getCity());
        assertEquals("Karnataka", org.getState());
        assertEquals("Acme Corp", org.getCompanyName());

        verify(userDao).save(user);
        verify(clientDao).save(org);
        verify(buyerRepository).save(any(BuyerEntity.class));
        verify(pendingRfqResumeService).resumePendingRfqs("buyer@example.com");
        assertEquals(2, response.getBody().getData().get("resumedRfqsCount"));
    }
}
