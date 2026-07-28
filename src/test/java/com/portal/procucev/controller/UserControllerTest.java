package com.portal.procucev.controller;

import com.portal.procucev.Dto.SimplePageResponse;
import com.portal.procucev.Dto.UserActivityDto;
import com.portal.procucev.Dto.VendorSummaryResponse;
import com.portal.procucev.config.JwtUtil;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;
import com.portal.procucev.model.UserActivity;
import com.portal.procucev.service.UserService;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {

    @Mock
    private UserService userServices;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private EmailUserRepo emailUserRepo;

    @InjectMocks
    private UserController controller;

    private User user;
    private Organization org;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setPhone("9876543210");

        org = new Organization();
        org.setId("ORG1");
    }

    @Test
    void testListUser_And_Delete() {
        when(userServices.findAll()).thenReturn(Collections.singletonList(user));
        List<User> list = controller.listUser();
        assertEquals(1, list.size());

        String res = controller.delete(1L);
        assertEquals("success", res);
        verify(userServices).delete(1L);
    }

    @Test
    void testCreate_Success() {
        when(userServices.save(any())).thenReturn(user);
        User resp = controller.create(user);
        assertEquals(user, resp);
    }

    @Test
    void testGetUserDetails_Success() {
        when(userServices.getUserByEmail(any())).thenReturn(user);
        User resp = controller.getUserDetails(user);
        assertEquals(user, resp);
    }

    @Test
    void testChangePswd_Success_AppException() {
        when(userServices.changePassword(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.changePswd(new com.portal.procucev.model.ResetPassword());
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.changePassword(any())).thenThrow(new AppException(400, "bad req", "type", "fail"));
        ResponseEntity<?> resp2 = controller.changePswd(new com.portal.procucev.model.ResetPassword());
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());
    }

    @Test
    void testSaveEmailUser_AlreadyAuthenticated_And_NewUser_And_Exception() {
        EmailUser emailUser = new EmailUser();
        emailUser.setEmail("test@email.com");

        when(emailUserRepo.findByEmail("test@email.com")).thenReturn(emailUser);
        ResponseEntity<?> resp1 = controller.saveEmailUser(emailUser);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(emailUserRepo.findByEmail("new@email.com")).thenReturn(null);
        when(userServices.saveEmailuser(any())).thenReturn(true);
        EmailUser newUser = new EmailUser();
        newUser.setEmail("new@email.com");
        ResponseEntity<?> resp2 = controller.saveEmailUser(newUser);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());

        when(emailUserRepo.findByEmail(any())).thenThrow(new RuntimeException("db err"));
        ResponseEntity<?> resp3 = controller.saveEmailUser(newUser);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp3.getStatusCode());
    }

    @Test
    void testGetSellerByEmail_Null_Or_NotFound_Or_Success() {
        ResponseEntity<?> resp1 = controller.getSellerByEmail(null);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        User invalidUser = new User();
        ResponseEntity<?> resp2 = controller.getSellerByEmail(invalidUser);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());

        when(userServices.getSellerByEmail(any())).thenReturn(null);
        ResponseEntity<?> resp3 = controller.getSellerByEmail(user);
        assertEquals(HttpStatus.OK, resp3.getStatusCode());

        when(userServices.getSellerByEmail(any())).thenReturn(org);
        ResponseEntity<?> resp4 = controller.getSellerByEmail(user);
        assertEquals(HttpStatus.OK, resp4.getStatusCode());
    }

    @Test
    void testGetVendorSummarySearchResults_Empty_Success_Exception() {
        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenReturn(null);
        ResponseEntity<Map<String, Object>> resp1 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenReturn(Collections.singletonList(new VendorSummaryResponse()));
        ResponseEntity<Map<String, Object>> resp2 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.OK, resp2.getStatusCode());

        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenThrow(new RuntimeException("db err"));
        ResponseEntity<Map<String, Object>> resp3 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp3.getStatusCode());
    }

    @Test
    void testGetSellerByEmail_NullInputs() {
        ResponseEntity<?> r1 = controller.getSellerByEmail(null);
        assertEquals(HttpStatus.OK, r1.getStatusCode());

        User uNoName = new User();
        uNoName.setPhone("9876543210");
        ResponseEntity<?> r2 = controller.getSellerByEmail(uNoName);
        assertEquals(HttpStatus.OK, r2.getStatusCode());

        User uNoPhone = new User();
        uNoPhone.setUsername("u@test.com");
        ResponseEntity<?> r3 = controller.getSellerByEmail(uNoPhone);
        assertEquals(HttpStatus.OK, r3.getStatusCode());

        when(userServices.getSellerByEmail(any())).thenReturn(null);
        ResponseEntity<?> r4 = controller.getSellerByEmail(user);
        assertEquals(HttpStatus.OK, r4.getStatusCode());
    }

    @Test
    void testSaveUserActivities_Success_Exception() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer token123");
        when(jwtUtil.extractUsername("token123")).thenReturn("user@test.com");
        when(jwtUtil.extractPhone("token123")).thenReturn("9876543210");
        when(userServices.saveUserActivity(any(), anyString(), anyString())).thenReturn(new UserActivity());

        ResponseEntity<?> resp1 = controller.saveUserActivities(new UserActivityDto(), req);
        assertEquals(HttpStatus.CREATED, resp1.getStatusCode());

        when(req.getHeader("Authorization")).thenThrow(new RuntimeException("token err"));
        ResponseEntity<?> resp2 = controller.saveUserActivities(new UserActivityDto(), req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp2.getStatusCode());
    }

    @Test
    void testUpdateEmailUserPswd() {
        when(userServices.updateEmailUserPswd(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.updateEmailUserPswd(new EmailUser());
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.updateEmailUserPswd(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.updateEmailUserPswd(new EmailUser());
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testDisableUser() {
        when(userServices.disableUser(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.disableUser(user);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.disableUser(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.disableUser(user);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testSendOtp_SuccessAndFailure() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(userServices.generateOtp(any(), any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.sendOtp(org, req);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.generateOtp(any(), any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.sendOtp(org, req);
        assertEquals(HttpStatus.NOT_FOUND, resp2.getStatusCode());
    }

    @Test
    void testValidateOtp_SuccessAndFailure() throws Exception {
        when(userServices.validateEmailOtp(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.validateOtp(org);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.validateEmailOtp(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.validateOtp(org);
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());
    }

    @Test
    void testUpdateOrganization() {
        when(userServices.updateOrganization(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.updateOrganization(org);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.updateOrganization(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.updateOrganization(org);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testUpdateBuyer() {
        when(userServices.updateBuyer(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.updateBuyer(org);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.updateBuyer(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.updateBuyer(org);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testGetVendorSummary_Empty() {
        SimplePageResponse<VendorSummaryResponse> emptyResp = new SimplePageResponse<>();
        emptyResp.setData(Collections.emptyList());
        when(userServices.getVendorSummary(anyInt(), anyInt(), any(), any())).thenReturn(emptyResp);
        ResponseEntity<Map<String, Object>> resp1 = controller.getVendorSummary(0, 10, null, null);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
    }

    @Test
    void testGetVendorSummary_Success() {
        SimplePageResponse<VendorSummaryResponse> validResp = new SimplePageResponse<>();
        validResp.setData(Collections.singletonList(new VendorSummaryResponse()));
        validResp.setTotalRecords(1);
        when(userServices.getVendorSummary(anyInt(), anyInt(), any(), any())).thenReturn(validResp);
        ResponseEntity<Map<String, Object>> resp2 = controller.getVendorSummary(0, 10, null, null);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testGetVendorSummary_Exception() {
        when(userServices.getVendorSummary(anyInt(), anyInt(), any(), any())).thenThrow(new RuntimeException("err"));
        ResponseEntity<Map<String, Object>> resp3 = controller.getVendorSummary(0, 10, null, null);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp3.getStatusCode());
    }

    @Test
    void testGetVendorSummary_NullDataIsTreatedAsNoVendorsFound() {
        SimplePageResponse<VendorSummaryResponse> nullData = new SimplePageResponse<>();
        nullData.setData(null);
        when(userServices.getVendorSummary(anyInt(), anyInt(), any(), any())).thenReturn(nullData);

        ResponseEntity<Map<String, Object>> resp = controller.getVendorSummary(0, 10, null, null);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("No vendors found", resp.getBody().get("message"));
        assertEquals(0, resp.getBody().get("totalRecords"));
    }

    @Test
    void testChangePswdReportsFailureMessageWhenServiceReturnsFalse() {
        when(userServices.changePassword(any())).thenReturn(false);

        ResponseEntity<?> resp = controller.changePswd(new com.portal.procucev.model.ResetPassword());

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Password change failed.",
                ((com.portal.procucev.customexception.MessageResponse) resp.getBody()).getMessage());
    }

    @Test
    void testSaveEmailUserReportsFailureWhenPersistenceReturnsFalse() {
        EmailUser newUser = new EmailUser();
        newUser.setEmail("fresh@email.com");
        when(emailUserRepo.findByEmail("fresh@email.com")).thenReturn(null);
        when(userServices.saveEmailuser(any())).thenReturn(false);

        ResponseEntity<?> resp = controller.saveEmailUser(newUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        com.portal.procucev.customexception.MessageResponse body =
                (com.portal.procucev.customexception.MessageResponse) resp.getBody();
        // the controller feeds (statusCode, msg) into MessageResponse(message, status),
        // so the failure code lands in message and the text lands in status
        assertEquals(com.portal.procucev.utils.ApplicationConstants.FAILURE, body.getMessage());
        assertEquals(String.format(com.portal.procucev.utils.ApplicationConstants.AUTHENTICATE_UNSUCCESS, ""),
                body.getStatus());
    }

    @Test
    void testDeactivateOrgUser_SuccessAndFailure() {
        when(userServices.deactivateOrgUser(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.deactivateOrgUser(user);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(userServices.deactivateOrgUser(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.deactivateOrgUser(user);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testGetSellerByEmail_Null() {
        ResponseEntity<?> respNull = controller.getSellerByEmail(null);
        assertEquals(HttpStatus.OK, respNull.getStatusCode());
    }

    @Test
    void testGetSellerByEmail_MissingInfo() {
        User incompleteUser = new User();
        ResponseEntity<?> resp1 = controller.getSellerByEmail(incompleteUser);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
    }

    @Test
    void testGetSellerByEmail_NotFound() {
        when(userServices.getSellerByEmail(any())).thenReturn(null);
        ResponseEntity<?> resp2 = controller.getSellerByEmail(user);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testGetSellerByEmail_Found() {
        when(userServices.getSellerByEmail(any())).thenReturn(org);
        ResponseEntity<?> resp3 = controller.getSellerByEmail(user);
        assertEquals(HttpStatus.OK, resp3.getStatusCode());
    }

    @Test
    void testGetVendorSummarySearchResults_Empty() {
        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenReturn(Collections.emptyList());
        ResponseEntity<Map<String, Object>> resp1 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
    }

    @Test
    void testGetVendorSummarySearchResults_Success() {
        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenReturn(Collections.singletonList(new VendorSummaryResponse()));
        ResponseEntity<Map<String, Object>> resp2 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testGetVendorSummarySearchResults_Exception() {
        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenThrow(new RuntimeException("err"));
        ResponseEntity<Map<String, Object>> resp3 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp3.getStatusCode());
    }

    @Test
    void testSaveUserActivities_SuccessAndException() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer fake_token_12345");
        when(jwtUtil.extractUsername(anyString())).thenReturn("user1");
        when(jwtUtil.extractPhone(anyString())).thenReturn("999888");

        UserActivity act = new UserActivity();
        when(userServices.saveUserActivity(any(), anyString(), anyString())).thenReturn(act);

        ResponseEntity<?> resp1 = controller.saveUserActivities(new UserActivityDto(), req);
        assertEquals(HttpStatus.CREATED, resp1.getStatusCode());

        when(userServices.saveUserActivity(any(), anyString(), anyString())).thenThrow(new RuntimeException("err"));
        ResponseEntity<?> resp2 = controller.saveUserActivities(new UserActivityDto(), req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp2.getStatusCode());
    }
}
