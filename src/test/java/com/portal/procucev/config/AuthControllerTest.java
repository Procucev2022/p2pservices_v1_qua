package com.portal.procucev.config;

import com.portal.procucev.model.AuthRequest;
import com.portal.procucev.service.SelfRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private SelfRegistrationService userService;
    @Mock
    private CustomUserDetailsService customDetailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthController controller;

    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser@test.com");
        authRequest.setPhone("9876543210");
        authRequest.setPassword("rawpass");
    }

    @Test
    void testAuthenticate_InvalidUser() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("error", response.getBody().get("status"));
        assertEquals("Invalid phone number and username", response.getBody().get("message"));
    }

    @Test
    void testAuthenticate_UnapprovedUser() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("error", response.getBody().get("status"));
        assertTrue(((String) response.getBody().get("message")).contains("under validation"));
    }

    @Test
    void testAuthenticate_OtpBranch_WithTempEmail_SuccessAndFailure() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(true);
        authRequest.setOtp(true);
        authRequest.setTempEmail("temp@test.com");

        when(userService.generateEmailOtp("temp@test.com", request, "9876543210")).thenReturn(true);

        ResponseEntity<Map<String, Object>> res1 = controller.authenticate(authRequest, request);
        assertEquals("success", res1.getBody().get("status"));

        when(userService.generateEmailOtp("temp@test.com", request, "9876543210")).thenReturn(false);
        ResponseEntity<Map<String, Object>> res2 = controller.authenticate(authRequest, request);
        assertEquals("error", res2.getBody().get("status"));
    }

    @Test
    void testAuthenticate_OtpBranch_WithoutTempEmail() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(true);
        authRequest.setOtp(true);
        authRequest.setTempEmail(null);

        when(userService.generateEmailOtp("testuser@test.com", request, "9876543210")).thenReturn(true);

        ResponseEntity<Map<String, Object>> res1 = controller.authenticate(authRequest, request);
        assertEquals("success", res1.getBody().get("status"));
    }

    @Test
    void testAuthenticate_OtpBranch_BlankTempEmailFallsBackToUsername() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(true);
        authRequest.setOtp(true);
        authRequest.setTempEmail("");

        when(userService.generateEmailOtp("testuser@test.com", request, "9876543210")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("success", response.getBody().get("status"));
        verify(userService).generateEmailOtp("testuser@test.com", request, "9876543210");
    }

    @Test
    void testAuthenticate_PasswordFromDb_NotFound() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(true);
        authRequest.setPassword(null);

        when(userService.fetchPasswordByEmailAndPhone("testuser@test.com", "9876543210")).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("error", response.getBody().get("status"));
        assertEquals("Password not found for the given user.", response.getBody().get("message"));
    }

    @Test
    void testAuthenticate_PasswordFromDb_Found() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(true);
        authRequest.setPassword("");

        when(userService.fetchPasswordByEmailAndPhone("testuser@test.com", "9876543210")).thenReturn("dbpass");
        UserDetails userDetails = new User("testuser@test.com", "encodedpass", Collections.emptyList());
        when(customDetailService.loadUserByUsernameAndPhone("testuser@test.com", "9876543210")).thenReturn(userDetails);
        when(passwordEncoder.matches("dbpass", "encodedpass")).thenReturn(true);
        when(jwtUtil.generateToken(userDetails, "9876543210")).thenReturn("jwt.token.val");

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("success", response.getBody().get("status"));
    }

    @Test
    void testAuthenticate_PasswordMismatch() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(true);

        UserDetails userDetails = new User("testuser@test.com", "encodedpass", Collections.emptyList());
        when(customDetailService.loadUserByUsernameAndPhone("testuser@test.com", "9876543210")).thenReturn(userDetails);
        when(passwordEncoder.matches("rawpass", "encodedpass")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("error", response.getBody().get("status"));
        assertEquals("Invalid credentials", response.getBody().get("message"));
    }

    @Test
    void testAuthenticate_ExceptionHandled() {
        when(userService.validateUser(any(), any())).thenThrow(new RuntimeException("DB Error"));

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("error", response.getBody().get("status"));
        assertTrue(((String) response.getBody().get("message")).contains("Authentication failed"));
    }

    @Test
    void testAuthenticate_Success() {
        when(userService.validateUser("testuser@test.com", "9876543210")).thenReturn(true);
        when(userService.validateUserApproval("testuser@test.com", "9876543210")).thenReturn(true);

        UserDetails userDetails = new User("testuser@test.com", "encodedpass", Collections.emptyList());
        when(customDetailService.loadUserByUsernameAndPhone("testuser@test.com", "9876543210")).thenReturn(userDetails);
        when(passwordEncoder.matches("rawpass", "encodedpass")).thenReturn(true);
        when(jwtUtil.generateToken(userDetails, "9876543210")).thenReturn("jwt.token.val");

        ResponseEntity<Map<String, Object>> response = controller.authenticate(authRequest, request);
        assertEquals("success", response.getBody().get("status"));
        assertEquals("jwt.token.val", response.getBody().get("access_token"));
    }
}
