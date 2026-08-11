package com.portal.procucev.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.ZohoPaymentLinkRequest;
import com.portal.procucev.config.ZohoApiClient;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.dao.SubscriptionPlanDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PaymentLink;
import com.portal.procucev.model.SubscriptionPlan;
import com.portal.procucev.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentLinkServiceTest {

    @Mock
    private ZohoApiClient zohoApiClient;
    @Mock
    private PaymentLinkRepository paymentLinkRepo;
    @Mock
    private SubscriptionPlanDao planRepo;
    @Mock
    private UserDao userDao;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private PaymentLinkService paymentLinkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentLinkService, "zohoPaymentsBaseUrl", "https://payments.zoho.com");
        ReflectionTestUtils.setField(paymentLinkService, "zohoAccountId", "ACC123");
    }

    private void mockSecurityContext(String username) {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(userDetails.getUsername()).thenReturn(username);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreatePaymentLink_UserNotFound() {
        mockSecurityContext("user@test.com");
        when(userDao.findByUsernameAndPhoneAndActive("user@test.com", "9876543210", true)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () ->
                paymentLinkService.createPaymentLink(1L, "9876543210", "user@test.com", "http://return.url")
        );
        assertEquals("User not found", ex.getErrorMessage());
    }

    @Test
    void testCreatePaymentLink_UserNotMatchingAuthenticatedUser() {
        mockSecurityContext("auth@test.com");
        User user = new User();
        user.setUsername("other@test.com");
        when(userDao.findByUsernameAndPhoneAndActive("user@test.com", "9876543210", true)).thenReturn(user);

        AppException ex = assertThrows(AppException.class, () ->
                paymentLinkService.createPaymentLink(1L, "9876543210", "user@test.com", "http://return.url")
        );
        assertEquals("User details do not match the authenticated user", ex.getErrorMessage());
    }

    @Test
    void testCreatePaymentLink_NoOrg() {
        mockSecurityContext("user@test.com");
        User user = new User();
        user.setUsername("user@test.com");
        when(userDao.findByUsernameAndPhoneAndActive("user@test.com", "9876543210", true)).thenReturn(user);

        AppException ex = assertThrows(AppException.class, () ->
                paymentLinkService.createPaymentLink(1L, "9876543210", "user@test.com", "http://return.url")
        );
        assertEquals("User organization not found", ex.getErrorMessage());
    }

    @Test
    void testCreatePaymentLink_PlanNotFound() {
        mockSecurityContext("user@test.com");
        User user = new User();
        user.setUsername("user@test.com");
        user.setOrg(new Organization());
        when(userDao.findByUsernameAndPhoneAndActive("user@test.com", "9876543210", true)).thenReturn(user);

        when(planRepo.findById("1")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                paymentLinkService.createPaymentLink(1L, "9876543210", "user@test.com", "http://return.url")
        );
        assertEquals("Subscription Plan not found", ex.getErrorMessage());
    }

    @Test
    void testCreatePaymentLink_PlanNotLaunched() {
        mockSecurityContext("user@test.com");
        User user = new User();
        user.setUsername("user@test.com");
        user.setOrg(new Organization());
        when(userDao.findByUsernameAndPhoneAndActive("user@test.com", "9876543210", true)).thenReturn(user);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setLaunchedStatus("NO");
        when(planRepo.findById("1")).thenReturn(Optional.of(plan));

        AppException ex = assertThrows(AppException.class, () ->
                paymentLinkService.createPaymentLink(1L, "9876543210", "user@test.com", "http://return.url")
        );
        assertTrue(ex.getErrorMessage().contains("launching soon"));
    }

    @Test
    void testCreatePaymentLink_Success_WithOfferPrice() throws Exception {
        mockSecurityContext("user@test.com");
        User user = new User();
        user.setId("10");
        user.setUsername("user@test.com");
        Organization org = new Organization();
        org.setId("100");
        user.setOrg(org);
        when(userDao.findByUsernameAndPhoneAndActive("user@test.com", "9876543210", true)).thenReturn(user);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setLaunchedStatus("YES");
        plan.setLaunchOfferPrice(1000);
        plan.setSubscriptionPrice(2000);
        plan.setPlanName("Base Plan");
        when(planRepo.findById("1")).thenReturn(Optional.of(plan));

        Map<String, Object> responseBody = new HashMap<>();
        Map<String, Object> links = new HashMap<>();
        links.put("payment_link_id", "PL123");
        links.put("url", "https://pay.zoho.com/link");
        links.put("status", "ACTIVE");
        responseBody.put("payment_links", links);

        when(zohoApiClient.post(anyString(), any(ZohoPaymentLinkRequest.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        PaymentLink link = paymentLinkService.createPaymentLink(1L, "9876543210", "user@test.com", "http://return.url");
        assertNotNull(link);
        assertEquals("PL123", link.getZohoPaymentLinkId());
        verify(paymentLinkRepo).save(any(PaymentLink.class));
    }

    @Test
    void testCreatePaymentLink_Success_WithSubscriptionPriceAndJsonException() throws Exception {
        mockSecurityContext("user@test.com");
        User user = new User();
        user.setId("10");
        user.setUsername("user@test.com");
        Organization org = new Organization();
        org.setId("100");
        user.setOrg(org);
        when(userDao.findByUsernameAndPhoneAndActive("user@test.com", "9876543210", true)).thenReturn(user);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setLaunchedStatus("YES");
        plan.setLaunchOfferPrice(0);
        plan.setSubscriptionPrice(2000);
        plan.setPlanName("Base Plan");
        when(planRepo.findById("1")).thenReturn(Optional.of(plan));

        Map<String, Object> responseBody = new HashMap<>();
        Map<String, Object> links = new HashMap<>();
        links.put("payment_link_id", "PL123");
        links.put("url", "https://pay.zoho.com/link");
        links.put("status", "ACTIVE");
        responseBody.put("payment_links", links);

        when(zohoApiClient.post(anyString(), any(ZohoPaymentLinkRequest.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error"){});

        PaymentLink link = paymentLinkService.createPaymentLink(1L, "9876543210", "user@test.com", "http://return.url");
        assertNotNull(link);
        assertEquals("PL123", link.getZohoPaymentLinkId());
        verify(paymentLinkRepo).save(any(PaymentLink.class));
    }
}
