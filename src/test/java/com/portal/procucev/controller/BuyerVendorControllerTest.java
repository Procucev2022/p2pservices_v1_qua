package com.portal.procucev.controller;

import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;
import com.portal.procucev.service.BuyerVendorService;
import com.portal.procucev.service.VendorAiProcessingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuyerVendorControllerTest {

    @Mock
    private BuyerVendorService buyerVendorService;

    @Mock
    private VendorAiProcessingService vendorAiProcessingService;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private BuyerVendorController controller;

    private User sampleUser;
    private Organization sampleOrg;
    private BuyerVendor sampleVendor;
    private BuyerVendorAiProfile sampleAiProfile;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        sampleOrg = new Organization();
        sampleOrg.setId("ORG-123");

        sampleUser = new User();
        sampleUser.setUsername("testuser");
        sampleUser.setActive(true);
        sampleUser.setOrg(sampleOrg);

        sampleVendor = new BuyerVendor();
        sampleVendor.setId("vnd-uuid-1");
        sampleVendor.setVendorCode("VND-001");
        sampleVendor.setVendorName("Test Vendor");
        sampleVendor.setBuyerOrgId("ORG-123");

        sampleAiProfile = new BuyerVendorAiProfile();
        sampleAiProfile.setId("ai-uuid-1");
        sampleAiProfile.setVendorCode("VND-001");
        sampleAiProfile.setBuyerOrgId("ORG-123");

        when(userDao.findByLatestUserName("testuser")).thenReturn(sampleUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
    }

    @Test
    void testGetVendorsSuccess() {
        Page<BuyerVendor> page = new PageImpl<>(List.of(sampleVendor));
        when(buyerVendorService.getVendors(eq("ORG-123"), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        ResponseEntity<MessageResponse> response = controller.getVendors("search", "Active", "Industry", 0, 10);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Vendors retrieved successfully", response.getBody().getMessage());
    }

    @Test
    void testGetVendorsException() {
        when(buyerVendorService.getVendors(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<MessageResponse> response = controller.getVendors("search", "Active", "Industry", 0, 10);
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testCreateVendorSuccess() {
        when(buyerVendorService.createVendor(any(BuyerVendor.class))).thenReturn(sampleVendor);

        ResponseEntity<MessageResponse> response = controller.createVendor(sampleVendor);
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Vendor created successfully", response.getBody().getMessage());
    }

    @Test
    void testCreateVendorValidationFailed() {
        when(buyerVendorService.createVendor(any(BuyerVendor.class))).thenThrow(new IllegalArgumentException("Duplicate code"));

        ResponseEntity<MessageResponse> response = controller.createVendor(sampleVendor);
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testCreateVendorInternalError() {
        when(buyerVendorService.createVendor(any(BuyerVendor.class))).thenThrow(new RuntimeException("Crash"));

        ResponseEntity<MessageResponse> response = controller.createVendor(sampleVendor);
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testCreateVendorsBulkSuccess() {
        Map<String, Object> resultMap = Map.of("savedCount", 2, "skippedCount", 0);
        when(buyerVendorService.bulkCreateVendors(anyList(), eq("ORG-123"), eq("testuser"))).thenReturn(resultMap);

        ResponseEntity<MessageResponse> response = controller.createVendorsBulk(List.of(sampleVendor));
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Bulk vendor upload processed successfully", response.getBody().getMessage());
    }

    @Test
    void testCreateVendorsBulkException() {
        when(buyerVendorService.bulkCreateVendors(anyList(), anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.createVendorsBulk(List.of(sampleVendor));
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testProcessVendorsAiSuccess() {
        when(vendorAiProcessingService.processVendorsWithAi(anyList(), eq("ORG-123"), eq("testuser"))).thenReturn(List.of(sampleAiProfile));

        ResponseEntity<MessageResponse> response = controller.processVendorsAi(List.of(sampleVendor));
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("AI Vendor Processing completed successfully", response.getBody().getMessage());
    }

    @Test
    void testProcessVendorsAiException() {
        when(vendorAiProcessingService.processVendorsWithAi(anyList(), anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.processVendorsAi(List.of(sampleVendor));
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetAiAnalyzedVendorsSuccess() {
        when(vendorAiProcessingService.getAnalyzedVendors("ORG-123")).thenReturn(List.of(sampleAiProfile));

        ResponseEntity<MessageResponse> response = controller.getAiAnalyzedVendors();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("AI Vendor Analysis retrieved successfully", response.getBody().getMessage());
    }

    @Test
    void testGetAiAnalyzedVendorsException() {
        when(vendorAiProcessingService.getAnalyzedVendors(anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.getAiAnalyzedVendors();
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetVendorAiProfileFound() {
        when(vendorAiProcessingService.getVendorAiProfile("VND-001", "ORG-123")).thenReturn(Optional.of(sampleAiProfile));

        ResponseEntity<MessageResponse> response = controller.getVendorAiProfile("VND-001");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetVendorAiProfileNotFound() {
        when(vendorAiProcessingService.getVendorAiProfile("VND-404", "ORG-123")).thenReturn(Optional.empty());

        ResponseEntity<MessageResponse> response = controller.getVendorAiProfile("VND-404");
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetVendorAiProfileException() {
        when(vendorAiProcessingService.getVendorAiProfile(anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.getVendorAiProfile("VND-001");
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetVendorByIdFound() {
        when(buyerVendorService.getVendorById("vnd-uuid-1", "ORG-123")).thenReturn(Optional.of(sampleVendor));

        ResponseEntity<MessageResponse> response = controller.getVendorById("vnd-uuid-1");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetVendorByIdNotFound() {
        when(buyerVendorService.getVendorById("unknown", "ORG-123")).thenReturn(Optional.empty());

        ResponseEntity<MessageResponse> response = controller.getVendorById("unknown");
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetVendorByIdException() {
        when(buyerVendorService.getVendorById(anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.getVendorById("vnd-uuid-1");
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testUpdateVendorSuccess() {
        when(buyerVendorService.updateVendor(eq("vnd-uuid-1"), any(BuyerVendor.class), eq("ORG-123"))).thenReturn(sampleVendor);

        ResponseEntity<MessageResponse> response = controller.updateVendor("vnd-uuid-1", sampleVendor);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUpdateVendorValidationFailed() {
        when(buyerVendorService.updateVendor(eq("vnd-uuid-1"), any(BuyerVendor.class), eq("ORG-123"))).thenThrow(new IllegalArgumentException("Not found"));

        ResponseEntity<MessageResponse> response = controller.updateVendor("vnd-uuid-1", sampleVendor);
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testUpdateVendorException() {
        when(buyerVendorService.updateVendor(anyString(), any(BuyerVendor.class), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.updateVendor("vnd-uuid-1", sampleVendor);
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testUpdateVendorStatusSuccess() {
        when(buyerVendorService.updateStatus("vnd-uuid-1", "ORG-123", "Inactive")).thenReturn(true);
        when(buyerVendorService.updateStatus("vnd-uuid-1", "ORG-123", "Pending")).thenReturn(true);

        ResponseEntity<MessageResponse> response = controller.updateVendorStatus("vnd-uuid-1", Map.of("status", "Inactive"));
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ResponseEntity<MessageResponse> responsePending = controller.updateVendorStatus("vnd-uuid-1", Map.of("status", "Pending"));
        assertEquals(HttpStatus.OK, responsePending.getStatusCode());
    }

    @Test
    void testUpdateVendorStatusInvalidStatus() {
        ResponseEntity<MessageResponse> response1 = controller.updateVendorStatus("vnd-uuid-1", Map.of("status", "INVALID"));
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());

        Map<String, String> nullMap = new HashMap<>();
        nullMap.put("status", null);
        ResponseEntity<MessageResponse> response2 = controller.updateVendorStatus("vnd-uuid-1", nullMap);
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());

        ResponseEntity<MessageResponse> response3 = controller.updateVendorStatus("vnd-uuid-1", Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, response3.getStatusCode());
    }

    @Test
    void testUpdateVendorStatusNotFound() {
        when(buyerVendorService.updateStatus("unknown", "ORG-123", "Active")).thenReturn(false);

        ResponseEntity<MessageResponse> response = controller.updateVendorStatus("unknown", Map.of("status", "Active"));
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUpdateVendorStatusException() {
        when(buyerVendorService.updateStatus(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.updateVendorStatus("vnd-uuid-1", Map.of("status", "Active"));
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testDeleteVendorSuccess() {
        when(buyerVendorService.deleteVendor("vnd-uuid-1", "ORG-123")).thenReturn(true);

        ResponseEntity<MessageResponse> response = controller.deleteVendor("vnd-uuid-1");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteVendorNotFound() {
        when(buyerVendorService.deleteVendor("unknown", "ORG-123")).thenReturn(false);

        ResponseEntity<MessageResponse> response = controller.deleteVendor("unknown");
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteVendorException() {
        when(buyerVendorService.deleteVendor(anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.deleteVendor("vnd-uuid-1");
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testBulkDeleteVendorsListPayload() {
        when(buyerVendorService.bulkDeleteVendors(List.of("VND-001", "VND-002"), "ORG-123")).thenReturn(2);

        ResponseEntity<MessageResponse> response = controller.bulkDeleteVendors(Arrays.asList("VND-001", "VND-002", null));
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testBulkDeleteVendorsMapVendorCodesPayload() {
        when(buyerVendorService.bulkDeleteVendors(List.of("VND-001"), "ORG-123")).thenReturn(1);

        ResponseEntity<MessageResponse> response = controller.bulkDeleteVendors(Map.of("vendorCodes", Arrays.asList("VND-001", null)));
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testBulkDeleteVendorsMapIdsPayload() {
        when(buyerVendorService.bulkDeleteVendors(List.of("vnd-uuid-1"), "ORG-123")).thenReturn(1);

        ResponseEntity<MessageResponse> response = controller.bulkDeleteVendors(Map.of("ids", Arrays.asList("vnd-uuid-1", null)));
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testBulkDeleteVendorsMapNonListPayload() {
        when(buyerVendorService.bulkDeleteVendors(Collections.emptyList(), "ORG-123")).thenReturn(0);

        ResponseEntity<MessageResponse> response = controller.bulkDeleteVendors(Map.of("ids", "NOT_A_LIST"));
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testBulkDeleteVendorsNullPayload() {
        when(buyerVendorService.bulkDeleteVendors(Collections.emptyList(), "ORG-123")).thenReturn(0);

        ResponseEntity<MessageResponse> response1 = controller.bulkDeleteVendors(null);
        assertNotNull(response1);
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        ResponseEntity<MessageResponse> response2 = controller.bulkDeleteVendors(12345);
        assertNotNull(response2);
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }

    @Test
    void testBulkDeleteVendorsException() {
        when(buyerVendorService.bulkDeleteVendors(anyList(), anyString())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> response = controller.bulkDeleteVendors(List.of("VND-001"));
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetLoggedInBuyerOrgIdFallbackActiveUser() {
        when(userDao.findByLatestUserName("testuser")).thenReturn(null);
        User userActiveWithoutOrg = new User();
        userActiveWithoutOrg.setActive(true);
        userActiveWithoutOrg.setOrg(null);

        User userWithOrg = new User();
        userWithOrg.setActive(true);
        userWithOrg.setOrg(sampleOrg);
        when(userDao.findByUsername("testuser")).thenReturn(List.of(userActiveWithoutOrg, userWithOrg));

        Page<BuyerVendor> page = new PageImpl<>(List.of(sampleVendor));
        when(buyerVendorService.getVendors(eq("ORG-123"), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        ResponseEntity<MessageResponse> response = controller.getVendors(null, null, null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetLoggedInBuyerOrgIdFallbackInactiveUserWithOrg() {
        when(userDao.findByLatestUserName("testuser")).thenReturn(null);
        User userInactiveWithoutOrg = new User();
        userInactiveWithoutOrg.setActive(false);
        userInactiveWithoutOrg.setOrg(null);

        User userInactiveWithOrg = new User();
        userInactiveWithOrg.setActive(false);
        userInactiveWithOrg.setOrg(sampleOrg);
        when(userDao.findByUsername("testuser")).thenReturn(List.of(userInactiveWithoutOrg, userInactiveWithOrg));

        Page<BuyerVendor> page = new PageImpl<>(List.of(sampleVendor));
        when(buyerVendorService.getVendors(eq("ORG-123"), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        ResponseEntity<MessageResponse> response = controller.getVendors(null, null, null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetLoggedInBuyerOrgIdNullUsersListThrowsIllegalStateException() {
        when(userDao.findByLatestUserName("testuser")).thenReturn(null);
        when(userDao.findByUsername("testuser")).thenReturn(null);

        ResponseEntity<MessageResponse> response = controller.getVendors(null, null, null, 0, 10);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetLoggedInBuyerOrgIdNoOrgInUsersThrowsIllegalStateException() {
        when(userDao.findByLatestUserName("testuser")).thenReturn(null);
        User userNoOrg = new User();
        userNoOrg.setActive(false);
        userNoOrg.setOrg(null);
        when(userDao.findByUsername("testuser")).thenReturn(List.of(userNoOrg));

        ResponseEntity<MessageResponse> response = controller.getVendors(null, null, null, 0, 10);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
