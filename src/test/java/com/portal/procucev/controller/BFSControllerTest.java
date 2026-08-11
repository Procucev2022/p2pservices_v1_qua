package com.portal.procucev.controller;

import com.portal.procucev.Dto.BFSItemDto;
import com.portal.procucev.Dto.BFSItemMainDetailsDTO;
import com.portal.procucev.Dto.BfsDTO;
import com.portal.procucev.Dto.VendorInfoBean;
import com.portal.procucev.customexception.ApiResponse;
import com.portal.procucev.model.*;
import com.portal.procucev.service.BFSService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BFSControllerTest {

    @Mock
    private BFSService bfsService;

    @InjectMocks
    private BFSController controller;

    private Organization org;
    private User user;
    private BFSItems item;
    private BFSUsers bfsUser;
    private BFSUserComments comment;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setId("ORG1");

        user = new User();
        user.setUsername("user1");

        item = new BFSItems();
        item.setId("100");

        bfsUser = new BFSUsers();
        bfsUser.setId("200");
        bfsUser.setUniqueId("UNIQ1");

        comment = new BFSUserComments();
        comment.setId("300");
    }

    @Test
    void testOrganizationSearch() {
        when(bfsService.orgSearch(any())).thenReturn(Collections.singletonList(org));
        ResponseEntity<?> resp = controller.organizationSearch(org);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetUsersByOrg() {
        when(bfsService.getUsersByOrg(any())).thenReturn(Collections.singletonList(user));
        ResponseEntity<?> resp = controller.getUsersByOrg(org);
        assertNotNull(resp.getBody());
    }

    @Test
    void testCreateBfs_SuccessAndFailure() throws Exception {
        when(bfsService.createBfs(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.createBfs(Collections.singletonList(item));
        assertNotNull(resp.getBody());

        when(bfsService.createBfs(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.createBfs(Collections.singletonList(item));
        assertNotNull(resp2.getBody());
    }

    @Test
    void testGetItemsByOrgAndGroup() {
        when(bfsService.getItemsByOrgAndUser(any())).thenReturn(Collections.singletonList(item));
        ResponseEntity<?> resp = controller.getItemsByOrgAndGroup(user);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetAllItems() {
        when(bfsService.getAllItems(any())).thenReturn(Collections.singletonList(item));
        ResponseEntity<?> resp = controller.getAllItems(user);
        assertNotNull(resp.getBody());
    }

    @Test
    void testRequestBfsItem_SuccessAndFailure() throws Exception {
        when(bfsService.requestBfsItem(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.requestBfsItem(bfsUser);
        assertNotNull(resp.getBody());

        when(bfsService.requestBfsItem(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.requestBfsItem(bfsUser);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testApproveBfsItem_ApproveAndReject_SuccessAndFailure() throws Exception {
        bfsUser.setApproval(true);
        when(bfsService.approveBfsItem(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.approveBfsItem(bfsUser);
        assertNotNull(resp.getBody());

        bfsUser.setApproval(false);
        when(bfsService.approveBfsItem(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.approveBfsItem(bfsUser);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testGetRequestedUserByBFS() {
        when(bfsService.getRequestedUserByBFS(any())).thenReturn(Collections.singletonList(bfsUser));
        ResponseEntity<?> resp = controller.getRequestedUserByBFS(item);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetAllItemMaster() throws Exception {
        when(bfsService.getRequestedItems()).thenReturn(Collections.singletonList(new BfsDTO()));
        ResponseEntity<?> resp = controller.getAllItemMaster();
        assertNotNull(resp.getBody());
    }

    @Test
    void testGeApprovedItems() throws Exception {
        when(bfsService.getApprovedItems(any())).thenReturn(Collections.singletonList(new BfsDTO()));
        ResponseEntity<?> resp = controller.geApprovedItems(user);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetBidsByBuyerAndItem() throws Exception {
        when(bfsService.getBidsByBuyer(any())).thenReturn(Collections.singletonList(new BfsDTO()));
        ResponseEntity<?> resp = controller.getBidsByBuyerAndItem(bfsUser);
        assertNotNull(resp.getBody());
    }

    @Test
    void testAcceptBfsItem_SuccessAndFailure() throws Exception {
        when(bfsService.acceptBfsItemBySeller(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.acceptBfsItem(bfsUser);
        assertNotNull(resp.getBody());

        when(bfsService.acceptBfsItemBySeller(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.acceptBfsItem(bfsUser);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testRejectBfsItemBySeller_SuccessAndFailure() throws Exception {
        when(bfsService.rejectBfsItemBySeller(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.rejectBfsItemBySeller(bfsUser);
        assertNotNull(resp.getBody());

        when(bfsService.rejectBfsItemBySeller(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.rejectBfsItemBySeller(bfsUser);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testGetDocumentsByBfs() throws Exception {
        when(bfsService.getDocumentsByBfs(any())).thenReturn(Collections.singletonList(new BFSDocuments()));
        ResponseEntity<?> resp = controller.getDocumentsByBfs(item);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetRequestedUserByBFSAndStatus() {
        when(bfsService.getRequestedUserByBFSAndStatus(any())).thenReturn(Collections.singletonList(bfsUser));
        ResponseEntity<?> resp = controller.getRequestedUserByBFSAndStatus(item);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetBfsItemsByBOQ() throws Exception {
        when(bfsService.createBfsByBoq(any())).thenReturn(Collections.singletonList(item));
        ResponseEntity<?> resp = controller.getBfsItemsByBOQ(item);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetBfsById() throws Exception {
        when(bfsService.getBfsById(any())).thenReturn(item);
        ResponseEntity<?> resp = controller.getBfsById(item);
        assertNotNull(resp.getBody());
    }

    @Test
    void testEditBfs_SuccessAndFailure() throws Exception {
        when(bfsService.editBfs(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.editBfs(item);
        assertNotNull(resp.getBody());

        when(bfsService.editBfs(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.editBfs(item);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testEditBfsUser_SuccessAndFailure() throws Exception {
        when(bfsService.editBfsUser(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.editBfsUser(bfsUser);
        assertNotNull(resp.getBody());

        when(bfsService.editBfsUser(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.editBfsUser(bfsUser);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testGetRequestedItemByBuyer() throws Exception {
        when(bfsService.getRequestedItemByBuyer(any())).thenReturn(Collections.singletonList(item));
        ResponseEntity<?> resp = controller.getRequestedItemByBuyer(user);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetRequestedItemsByCM() throws Exception {
        when(bfsService.getRequestedItemsByCM()).thenReturn(Collections.singletonList(item));
        ResponseEntity<?> resp = controller.getRequestedItemsByCM();
        assertNotNull(resp.getBody());
    }

    @Test
    void testEditBuyerItemBySeller_SuccessAndFailure() throws Exception {
        when(bfsService.editRequestedItemBySeller(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.editBuyerItemBySeller(bfsUser);
        assertNotNull(resp.getBody());

        when(bfsService.editRequestedItemBySeller(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.editBuyerItemBySeller(bfsUser);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testCreateBFSCommentByBuyer_SuccessAndFailure() throws Exception {
        when(bfsService.createBFSCommentByBuyer(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.createBFSCommentByBuyer(comment);
        assertNotNull(resp.getBody());

        when(bfsService.createBFSCommentByBuyer(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.createBFSCommentByBuyer(comment);
        assertNotNull(resp2.getBody());
    }

    @Test
    void testGetCommentsByItem() throws Exception {
        when(bfsService.getCommentsByItem(any())).thenReturn(Collections.singletonList(comment));
        ResponseEntity<?> resp = controller.getCommentsByItem(comment);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetCommentsByItemAndBuyer() throws Exception {
        when(bfsService.getCommentsByItemAndBuyer(any())).thenReturn(Collections.singletonList(comment));
        ResponseEntity<?> resp = controller.getCommentsByItemAndBuyer(comment);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetItemByUniqueId() throws Exception {
        when(bfsService.getItemByUniqueId(any())).thenReturn(new BfsDTO());
        ResponseEntity<?> resp = controller.getItemByUniqueId(bfsUser);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetImagesByBfs() throws Exception {
        when(bfsService.getImagesByBfs(any())).thenReturn(Collections.singletonList(new BFSImages()));
        ResponseEntity<?> resp = controller.getImagesByBfs(item);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetuserInfoById() throws Exception {
        when(bfsService.getuserInfoById(any())).thenReturn(new VendorInfoBean());
        ResponseEntity<?> resp = controller.getuserInfoById(user);
        assertNotNull(resp.getBody());
    }

    @Test
    void testDeactivateCommentsFlag() throws Exception {
        when(bfsService.deactivateCommentsFlag(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.deactivateCommentsFlag(item);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetBfsItemsByCategory_EmptyAndNotEmpty() {
        when(bfsService.getBfsItemsByCategory(any())).thenReturn(Collections.emptyList());
        ResponseEntity<ApiResponse> resp1 = controller.getBfsItemsByCategory(Collections.singletonList(new BFSItemDto()));
        assertEquals("200", resp1.getBody().getStatusCode());

        BFSItemMainDetailsDTO dto = new BFSItemMainDetailsDTO("1", "desc", "spec", 10.0, 5.0, "cat", "num", "loc", "1yr", "pcs", 100.0, 10.0, 90.0, "grp", false, "rem", true);
        when(bfsService.getBfsItemsByCategory(any())).thenReturn(Collections.singletonList(dto));
        ResponseEntity<ApiResponse> resp2 = controller.getBfsItemsByCategory(Collections.singletonList(new BFSItemDto()));
        assertEquals("200", resp2.getBody().getStatusCode());
    }

    @Test
    void testUploadExcel() {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
        ResponseEntity<?> resp = controller.uploadExcel(file);
        assertNotNull(resp.getBody());
        verify(bfsService).processExcel(file);
    }

    @Test
    void testGetBidsBySeller() throws Exception {
        when(bfsService.getBidsBySeller(any())).thenReturn(Collections.singletonList(new BfsDTO()));
        ResponseEntity<?> resp = controller.getBidsBySeller(bfsUser);
        assertNotNull(resp.getBody());
    }
}
