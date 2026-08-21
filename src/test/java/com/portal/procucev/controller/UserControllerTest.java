package com.portal.procucev.controller;

import com.portal.procucev.Dto.SimplePageResponse;
import com.portal.procucev.Dto.UserActivityDto;
import com.portal.procucev.Dto.VendorSummaryResponse;
import com.portal.procucev.config.JwtUtil;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.ResetPassword;
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
        user.setId("USER1");
        user.setUsername("testuser");
        user.setPhone("9876543210");
        user.setFirstName("First");
        user.setFullName("Full Name");

        org = new Organization();
        org.setId("ORG1");
        user.setOrg(org);
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
        ResponseEntity<?> resp1 = controller.changePswd(new ResetPassword());
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        reset(userServices);
        when(userServices.changePassword(any())).thenThrow(new AppException(400, "bad req", "type", "fail"));
        ResponseEntity<?> resp2 = controller.changePswd(new ResetPassword());
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());

        reset(userServices);
        when(userServices.changePassword(any())).thenThrow(new RuntimeException("err"));
        assertThrows(RuntimeException.class, () -> controller.changePswd(new ResetPassword()));
    }

    @Test
    void testVendorSummary() {
        SimplePageResponse<VendorSummaryResponse> pageResp = new SimplePageResponse<>();
        pageResp.setData(List.of(new VendorSummaryResponse()));
        pageResp.setTotalRecords(1);
        when(userServices.getVendorSummary(anyInt(), anyInt(), any(), any())).thenReturn(pageResp);

        ResponseEntity<Map<String, Object>> resp = controller.getVendorSummary(0, 10, null, null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        pageResp.setData(Collections.emptyList());
        ResponseEntity<Map<String, Object>> emptyResp = controller.getVendorSummary(0, 10, null, null);
        assertEquals(HttpStatus.OK, emptyResp.getStatusCode());

        when(userServices.getVendorSummary(anyInt(), anyInt(), any(), any())).thenThrow(new RuntimeException("err"));
        ResponseEntity<Map<String, Object>> errResp = controller.getVendorSummary(0, 10, null, null);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errResp.getStatusCode());
    }

    @Test
    void testDeactivateOrgUser() {
        when(userServices.deactivateOrgUser(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.deactivateOrgUser(user);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testGetSellerByEmail() {
        ResponseEntity<?> r1 = controller.getSellerByEmail(null);
        assertEquals(HttpStatus.OK, r1.getStatusCode());

        User u1 = new User();
        u1.setUsername("e@test.com");
        ResponseEntity<?> r2 = controller.getSellerByEmail(u1);
        assertEquals(HttpStatus.OK, r2.getStatusCode());

        u1.setPhone("9876543210");
        when(userServices.getSellerByEmail(any())).thenReturn(null);
        ResponseEntity<?> r3 = controller.getSellerByEmail(u1);
        assertEquals(HttpStatus.OK, r3.getStatusCode());

        when(userServices.getSellerByEmail(any())).thenReturn(org);
        ResponseEntity<?> r4 = controller.getSellerByEmail(u1);
        assertEquals(HttpStatus.OK, r4.getStatusCode());
    }

    @Test
    void testGetBuyerByEmail() {
        ResponseEntity<?> r1 = controller.getBuyerByEmail(null);
        assertEquals(HttpStatus.OK, r1.getStatusCode());

        when(userServices.getBuyerUserByEmail(any())).thenReturn(null);
        ResponseEntity<?> r2 = controller.getBuyerByEmail(user);
        assertEquals(HttpStatus.OK, r2.getStatusCode());

        when(userServices.getBuyerUserByEmail(any())).thenReturn(user);
        ResponseEntity<?> r3 = controller.getBuyerByEmail(user);
        assertEquals(HttpStatus.OK, r3.getStatusCode());
    }

    @Test
    void testVendorSummarySearch() {
        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenReturn(List.of(new VendorSummaryResponse()));
        ResponseEntity<Map<String, Object>> r1 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.OK, r1.getStatusCode());

        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenReturn(Collections.emptyList());
        ResponseEntity<Map<String, Object>> r2 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.OK, r2.getStatusCode());

        when(userServices.getVendorSummarySearchResults(anyString(), anyString())).thenThrow(new RuntimeException("err"));
        ResponseEntity<Map<String, Object>> r3 = controller.getVendorSummarySearchResults("type", "val");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r3.getStatusCode());
    }

    @Test
    void testSaveUserActivities() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer header.token.val");
        when(jwtUtil.extractUsername(anyString())).thenReturn("user1");
        when(jwtUtil.extractPhone(anyString())).thenReturn("9876543210");
        when(userServices.saveUserActivity(any(), anyString(), anyString())).thenReturn(new UserActivity());

        ResponseEntity<?> r1 = controller.saveUserActivities(new UserActivityDto(), req);
        assertEquals(HttpStatus.CREATED, r1.getStatusCode());

        when(userServices.saveUserActivity(any(), anyString(), anyString())).thenThrow(new RuntimeException("err"));
        ResponseEntity<?> r2 = controller.saveUserActivities(new UserActivityDto(), req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r2.getStatusCode());
    }

    @Test
    void testGetBuyerMessageByEmail() {
        when(userServices.getBuyerByEmail(anyString())).thenReturn(ResponseEntity.ok(new MessageResponse("200", "Success")));
        ResponseEntity<MessageResponse> resp = controller.getBuyerMessageByEmail(user);
        assertNotNull(resp);
    }
}
