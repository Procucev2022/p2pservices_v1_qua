package com.portal.procucev.controller;

import com.portal.procucev.Dto.*;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.customexception.RfqStatusResponse;
import com.portal.procucev.model.*;
import com.portal.procucev.service.GMTService;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GMTControllerTest {

    @Mock
    private GMTService gmtService;

    @InjectMocks
    private GMTController controller;

    private User user;
    private Organization org;
    private Rfq rfq;
    private GmtRfqVendors gmtVendor;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");

        org = new Organization();
        org.setId("ORG1");

        rfq = new Rfq();
        rfq.setRfqId("RFQ100");
        rfq.setRequestType("Forward");

        gmtVendor = new GmtRfqVendors();
        gmtVendor.setId("10");
    }

    @Test
    void testGetClientRfqIds() throws Exception {
        when(gmtService.getClientRfqIds(any())).thenReturn(Collections.singletonList("RFQ1"));
        ResponseEntity<?> resp = controller.getClientRfqIds(user);
        assertNotNull(resp.getBody());
    }

    @Test
    void testGetNoPrRfqByClient() {
        when(gmtService.getNoPrRfqByClient(any())).thenReturn(Collections.emptyList());
        ResponseEntity<?> resp = controller.getNoPrRfqByClient(user);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testGetAllDivision() {
        when(gmtService.getAllDivision()).thenReturn(Collections.singletonList("DIV1"));
        ResponseEntity<?> resp = controller.getAllDivision();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testCreateRFQForNoPrByClient_SuccessAndFailure() throws Exception {
        when(gmtService.createRFQForNoPrByClient(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.createRFQForNoPrByClient(rfq);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(gmtService.createRFQForNoPrByClient(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.createRFQForNoPrByClient(rfq);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testEditRFQForNoPrByClient_SuccessAndFailure() throws Exception {
        when(gmtService.editRFQForNoPrByClient(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.editRFQForNoPrByClient(rfq);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(gmtService.editRFQForNoPrByClient(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.editRFQForNoPrByClient(rfq);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testConvertRfqBoq() {
        when(gmtService.convertRFQBoq(any())).thenReturn(Collections.emptyList());
        ResponseEntity<?> resp = controller.convertRfqBoq(rfq);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testQueryMail_SuccessAndFailure() throws Exception {
        when(gmtService.queryMail(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.queryMail(user);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(gmtService.queryMail(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.queryMail(user);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testRequestRfqByVendors_And_Sellers() {
        when(gmtService.requestRfqByVendors(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.requestRfqByVendors(Collections.singletonList(gmtVendor));
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(gmtService.requestRfqBySellers(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.Sellers(Collections.singletonList(gmtVendor));
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testGetVendorsByGmtRFQ() {
        when(gmtService.getVendorsByGmtRfq(any())).thenReturn(Collections.emptyList());
        ResponseEntity<?> resp = controller.getVendorsByGmtRFQ(rfq);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testApproveVendor_And_IgnoreRfq_And_RejectRfq() throws Exception {
        when(gmtService.approveVendor(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.approveVendor(gmtVendor);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(gmtService.ignoreRfqByVendor(any())).thenReturn(true);
        ResponseEntity<?> resp2 = controller.ignoreRfqByVendor(Collections.singletonList(gmtVendor));
        assertEquals(HttpStatus.OK, resp2.getStatusCode());

        when(gmtService.rejectRfqForVendor(any())).thenReturn(true);
        ResponseEntity<?> resp3 = controller.rejectRfqForVendor(gmtVendor);
        assertEquals(HttpStatus.OK, resp3.getStatusCode());
    }

    @Test
    void testGetAllRfqforCM() throws Exception {
        when(gmtService.getAllRfqForCM()).thenReturn(Collections.emptyList());
        ResponseEntity<?> resp = controller.getAllRfqforCM();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testRaiseQueryByVendor() {
        when(gmtService.raiseQueryByVendor(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.raiseQueryByVendor(gmtVendor);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testFetchAllClientGMTRfqsForCM_Unpaginated_PaginatedEmpty_PaginatedSuccess_Exception() throws Exception {
        when(gmtService.fetchAllClientGMTRfqsForCM()).thenReturn(Collections.emptyList());
        ResponseEntity<?> respUnpaginated = controller.fetchAllClientGMTRfqsForCM(null, null);
        assertEquals(HttpStatus.OK, respUnpaginated.getStatusCode());

        SimplePageResponse<RfqDTO> emptyResp = new SimplePageResponse<>();
        emptyResp.setData(Collections.emptyList());
        when(gmtService.fetchAllClientGMTRfqsForCM(any())).thenReturn(emptyResp);
        ResponseEntity<?> respEmpty = controller.fetchAllClientGMTRfqsForCM(0, 10);
        assertEquals(HttpStatus.OK, respEmpty.getStatusCode());

        SimplePageResponse<RfqDTO> validResp = new SimplePageResponse<>();
        validResp.setData(Collections.singletonList(new RfqDTO()));
        validResp.setTotalRecords(1);
        when(gmtService.fetchAllClientGMTRfqsForCM(any())).thenReturn(validResp);
        ResponseEntity<?> respValid = controller.fetchAllClientGMTRfqsForCM(0, 10);
        assertEquals(HttpStatus.OK, respValid.getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCM(any())).thenThrow(new RuntimeException("err"));
        ResponseEntity<?> respErr = controller.fetchAllClientGMTRfqsForCM(0, 10);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respErr.getStatusCode());
    }

    @Test
    void testFetchAllClientGMTRfqsForCMSearch_Empty_Success_Exception() throws Exception {
        when(gmtService.fetchAllClientGMTRfqsForCMSearch(anyString(), anyString())).thenReturn(Collections.emptyList());
        ResponseEntity<?> respEmpty = controller.fetchAllClientGMTRfqsForCMSearch("type", "val");
        assertEquals(HttpStatus.OK, respEmpty.getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCMSearch(anyString(), anyString())).thenReturn(Collections.singletonList(new RfqDTO()));
        ResponseEntity<?> respValid = controller.fetchAllClientGMTRfqsForCMSearch("type", "val");
        assertEquals(HttpStatus.OK, respValid.getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCMSearch(anyString(), anyString())).thenThrow(new RuntimeException("err"));
        ResponseEntity<?> respErr = controller.fetchAllClientGMTRfqsForCMSearch("type", "val");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respErr.getStatusCode());
    }

    @Test
    void testGetCategoryByDivision_And_GetAllGmtItems_And_FetchRfqById_And_GetAllCategory_And_GetRFQsForNoPR() {
        when(gmtService.getCategoryByDivision(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getCategoryByDivision(new CategoryDivision()).getStatusCode());

        when(gmtService.getAllGmtItems()).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getAllGmtItems().getStatusCode());

        doReturn(ResponseEntity.ok("ok")).when(gmtService).fetchRfqById(any());
        assertEquals(HttpStatus.OK, controller.fetchRfqById(rfq).getStatusCode());

        when(gmtService.getAllCategory()).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getAllCategory().getStatusCode());

        when(gmtService.getRFQsForNoPR()).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getRFQsForNoPR().getStatusCode());
    }

    @Test
    void testGetAllVendors_Unpaginated_Empty_Success_Exception() {
        when(gmtService.getAllVendors()).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getAllVendors(null, null).getStatusCode());

        SimplePageResponse<VendorRFQDto> emptyResp = new SimplePageResponse<>();
        emptyResp.setData(Collections.emptyList());
        when(gmtService.getAllVendors(any())).thenReturn(emptyResp);
        assertEquals(HttpStatus.OK, controller.getAllVendors(0, 10).getStatusCode());

        SimplePageResponse<VendorRFQDto> validResp = new SimplePageResponse<>();
        validResp.setData(Collections.singletonList(new VendorRFQDto()));
        validResp.setTotalRecords(1);
        when(gmtService.getAllVendors(any())).thenReturn(validResp);
        assertEquals(HttpStatus.OK, controller.getAllVendors(0, 10).getStatusCode());

        when(gmtService.getAllVendors(any())).thenThrow(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.getAllVendors(0, 10).getStatusCode());
    }

    @Test
    void testGetAllVendorsSearch_Empty_Success_Exception() {
        when(gmtService.getAllVendorsSearch(anyString(), anyString())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getAllVendorsSearch("type", "val").getStatusCode());

        when(gmtService.getAllVendorsSearch(anyString(), anyString())).thenReturn(Collections.singletonList(new VendorRFQDto()));
        assertEquals(HttpStatus.OK, controller.getAllVendorsSearch("type", "val").getStatusCode());

        when(gmtService.getAllVendorsSearch(anyString(), anyString())).thenThrow(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.getAllVendorsSearch("type", "val").getStatusCode());
    }

    @Test
    void testForwardRfqForNoPr_Forward_Invite_Success_Failure() {
        rfq.setRequestType("Forward");
        when(gmtService.forwardRfqForNoPr(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(rfq).getStatusCode());

        when(gmtService.forwardRfqForNoPr(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(rfq).getStatusCode());

        rfq.setRequestType("Invite");
        when(gmtService.forwardRfqForNoPr(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(rfq).getStatusCode());

        when(gmtService.forwardRfqForNoPr(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(rfq).getStatusCode());
    }

    @Test
    void testEditUser_And_AcceptClient_And_IgnoreClient_And_DisableUser() throws Exception {
        when(gmtService.editUser(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.editUser(user).getStatusCode());

        when(gmtService.acceptSelfClient(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.acceptClient(user).getStatusCode());

        when(gmtService.ignoreClient(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.ignoreClient(user).getStatusCode());

        when(gmtService.disableUser(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.disableUser(user).getStatusCode());
    }

    @Test
    void testSendEmail_SwitchBranches() throws Exception {
        MessageResponse m200 = new MessageResponse("200", "msg", null, "1");
        when(gmtService.sendEmail(any())).thenReturn(m200);
        assertEquals(HttpStatus.OK, controller.sendEmail(new EmailRequest()).getStatusCode());

        MessageResponse m206 = new MessageResponse("206", "msg", null, "1");
        when(gmtService.sendEmail(any())).thenReturn(m206);
        assertEquals(HttpStatus.PARTIAL_CONTENT, controller.sendEmail(new EmailRequest()).getStatusCode());

        MessageResponse m400 = new MessageResponse("400", "msg", null, "1");
        when(gmtService.sendEmail(any())).thenReturn(m400);
        assertEquals(HttpStatus.BAD_REQUEST, controller.sendEmail(new EmailRequest()).getStatusCode());

        MessageResponse m500 = new MessageResponse("500", "msg", null, "1");
        when(gmtService.sendEmail(any())).thenReturn(m500);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.sendEmail(new EmailRequest()).getStatusCode());

        MessageResponse mOther = new MessageResponse("999", "msg", null, "1");
        when(gmtService.sendEmail(any())).thenReturn(mOther);
        assertEquals(HttpStatus.OK, controller.sendEmail(new EmailRequest()).getStatusCode());
    }

    @Test
    void testCreateRFQByClient_Success_And_Failure() throws Exception {
        Map<String, Object> mapSuccess = new HashMap<>();
        mapSuccess.put("rfqId", "RFQ123");
        when(gmtService.createRFQByClient(any())).thenReturn(mapSuccess);
        assertEquals(HttpStatus.OK, controller.createRFQByClient(rfq).getStatusCode());

        when(gmtService.createRFQByClient(any())).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.createRFQByClient(rfq).getStatusCode());
    }

    @Test
    void testGetSubscriptionPlans() throws Exception {
        when(gmtService.getSubscriptionPlans()).thenReturn(Collections.singletonList(new SubscriptionPlan()));
        ResponseEntity<?> resp = controller.getSubscriptionPlans();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testRemainingGmtEndpoints() throws Exception {
        ForwardRfqVendorRequest fReq = new ForwardRfqVendorRequest();
        when(gmtService.forwardRfqsToVendor(any())).thenReturn(Collections.emptyMap());
        assertEquals(HttpStatus.OK, controller.forwardRfqsToVendor(fReq).getStatusCode());

        when(gmtService.getLastOpenRfqsForVendor(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getOpenRfqsForSeller(org).getStatusCode());

        Organization orgNullId = new Organization();
        assertEquals(HttpStatus.OK, controller.getVendorInfo(orgNullId).getStatusCode());

        when(gmtService.getVendorInfo(any())).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.getVendorInfo(org).getStatusCode());

        when(gmtService.getVendorInfo(any())).thenReturn(new VendorInfoDto());
        assertEquals(HttpStatus.OK, controller.getVendorInfo(org).getStatusCode());

        assertEquals(HttpStatus.OK, controller.emailForwarder().getStatusCode());
        verify(gmtService).emailForwarder();

        when(gmtService.getBuyerDataByRFQ(any())).thenReturn(user);
        assertEquals(HttpStatus.OK, controller.getBuyerDataByRFQ(rfq).getStatusCode());

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(gmtService.generateOtp(any(), any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.sendOtp(org, req).getStatusCode());
        when(gmtService.generateOtp(any(), any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.sendOtp(org, req).getStatusCode());

        when(gmtService.validateOtp(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.validateOtp(org).getStatusCode());
        when(gmtService.validateOtp(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.validateOtp(org).getStatusCode());

        when(gmtService.submitUpgradeVendor(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.upgradeGmtVendor(org).getStatusCode());
        when(gmtService.submitUpgradeVendor(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.upgradeGmtVendor(org).getStatusCode());

        when(gmtService.getSellerRfqStatusData(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getSellerRfqStatusData(new RfqStatusRequest()).getStatusCode());

        when(gmtService.getBuyerByRFQ(any())).thenReturn(user);
        assertEquals(HttpStatus.OK, controller.getBuyerByRFQ(rfq).getStatusCode());

        assertEquals(HttpStatus.OK, controller.updateVendorCommentStatus(rfq).getStatusCode());
        verify(gmtService).markVendorCommentAsRead(rfq);
    }

    @Test
    void testAllRemainingGmtEndpoints() throws Exception {
        when(gmtService.getAllVendorsByCategory(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getAllVendorsByCategory(org).getStatusCode());

        when(gmtService.createRFQWithNoPr(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.createRFQForNoPrWithItems(rfq).getStatusCode());

        when(gmtService.createRFQWithNoPr(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.createRFQForNoPrWithItems(rfq).getStatusCode());

        when(gmtService.getItemsbyrfqrid(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getItemsbyrfqid(rfq).getStatusCode());

        when(gmtService.fetchSelfRegisterClients()).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getSelfRegisterClients().getStatusCode());

        when(gmtService.getclientusersByClientId(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getclientusersByclient(org).getStatusCode());

        when(gmtService.editAndResendRfq(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.editAndResendRfq(rfq).getStatusCode());

        when(gmtService.editAndResendRfq(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.editAndResendRfq(rfq).getStatusCode());

        when(gmtService.getVendorsbyRFQ(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getVendorbyRFQ(rfq).getStatusCode());

        when(gmtService.getGmtBuyers()).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getGmtBuyers().getStatusCode());

        when(gmtService.getRfqStatuses(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getRfqStatus(new RfqStatusRequest()).getStatusCode());

        when(gmtService.getRfqSellerStatuses(any())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getSellerRfqStatus(new RfqStatusRequest()).getStatusCode());

        when(gmtService.getSellerRfqCredits(any())).thenReturn(5);
        assertEquals(HttpStatus.OK, controller.getSellerRfqCredits(org).getStatusCode());

        when(gmtService.getOrgById(any())).thenReturn(org);
        assertEquals(HttpStatus.OK, controller.getOrgById(org).getStatusCode());

        when(gmtService.getOrgByUserId(any())).thenReturn(org);
        assertEquals(HttpStatus.OK, controller.getOrgByUserId(user).getStatusCode());

        when(gmtService.getRfqByItemCategory(any())).thenReturn(Collections.emptyMap());
        assertEquals(HttpStatus.OK, controller.getRfqByCategory(org).getStatusCode());

        // Extra branch cases
        SimplePageResponse<RfqDTO> validResp = new SimplePageResponse<>();
        validResp.setData(Collections.singletonList(new RfqDTO()));
        when(gmtService.fetchAllClientGMTRfqsForCM(any())).thenReturn(validResp);
        assertEquals(HttpStatus.OK, controller.fetchAllClientGMTRfqsForCM(0, null).getStatusCode());
        assertEquals(HttpStatus.OK, controller.fetchAllClientGMTRfqsForCM(null, 10).getStatusCode());

        SimplePageResponse<VendorRFQDto> vValidResp = new SimplePageResponse<>();
        vValidResp.setData(Collections.singletonList(new VendorRFQDto()));
        when(gmtService.getAllVendors(any())).thenReturn(vValidResp);
        assertEquals(HttpStatus.OK, controller.getAllVendors(0, null).getStatusCode());
        assertEquals(HttpStatus.OK, controller.getAllVendors(null, 10).getStatusCode());

        Rfq rfqOtherType = new Rfq();
        rfqOtherType.setRequestType("OtherType");
        when(gmtService.forwardRfqForNoPr(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(rfqOtherType).getStatusCode());
        when(gmtService.forwardRfqForNoPr(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(rfqOtherType).getStatusCode());

        when(gmtService.getSubscriptionPlans()).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getSubscriptionPlans().getStatusCode());

        SimplePageResponse<RfqDTO> emptyResp = new SimplePageResponse<>();
        emptyResp.setData(null);
        when(gmtService.fetchAllClientGMTRfqsForCM(any())).thenReturn(emptyResp);
        assertEquals(HttpStatus.OK, controller.fetchAllClientGMTRfqsForCM(0, 10).getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCM(any())).thenThrow(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.fetchAllClientGMTRfqsForCM(0, 10).getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCMSearch(anyString(), anyString())).thenReturn(Collections.singletonList(new RfqDTO()));
        assertEquals(HttpStatus.OK, controller.fetchAllClientGMTRfqsForCMSearch("type", "val").getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCMSearch(anyString(), anyString())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.fetchAllClientGMTRfqsForCMSearch("type", "val").getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCMSearch(anyString(), anyString())).thenThrow(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.fetchAllClientGMTRfqsForCMSearch("type", "val").getStatusCode());

        SimplePageResponse<VendorRFQDto> vEmptyResp = new SimplePageResponse<>();
        vEmptyResp.setData(null);
        when(gmtService.getAllVendors(any())).thenReturn(vEmptyResp);
        assertEquals(HttpStatus.OK, controller.getAllVendors(0, 10).getStatusCode());

        when(gmtService.getAllVendors(any())).thenThrow(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.getAllVendors(0, 10).getStatusCode());

        when(gmtService.getAllVendorsSearch(anyString(), anyString())).thenReturn(Collections.singletonList(new VendorRFQDto()));
        assertEquals(HttpStatus.OK, controller.getAllVendorsSearch("type", "val").getStatusCode());

        when(gmtService.getAllVendorsSearch(anyString(), anyString())).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getAllVendorsSearch("type", "val").getStatusCode());

        when(gmtService.getAllVendorsSearch(anyString(), anyString())).thenThrow(new RuntimeException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.getAllVendorsSearch("type", "val").getStatusCode());

        Rfq fRfq = new Rfq();
        fRfq.setRequestType("Forward");
        when(gmtService.forwardRfqForNoPr(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(fRfq).getStatusCode());

        Rfq iRfq = new Rfq();
        iRfq.setRequestType("Invite");
        when(gmtService.forwardRfqForNoPr(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(iRfq).getStatusCode());

        Rfq nullRfq = new Rfq();
        assertEquals(HttpStatus.OK, controller.forwardRfqForNoPr(nullRfq).getStatusCode());

        when(gmtService.getSubscriptionPlans()).thenThrow(new RuntimeException("plan err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.getSubscriptionPlans().getStatusCode());
    }

    @Test
    void testExactMissedBranches() throws Exception {
        when(gmtService.getAllGMTRfq(org)).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getAllGMTRfq(org).getStatusCode());

        when(gmtService.requestRfqByVendors(any())).thenReturn(false);
        assertEquals(HttpStatus.OK,
                controller.requestRfqByVendors(Collections.singletonList(gmtVendor)).getStatusCode());

        when(gmtService.requestRfqBySellers(any())).thenReturn(true);
        assertEquals(HttpStatus.OK,
                controller.Sellers(Collections.singletonList(gmtVendor)).getStatusCode());

        when(gmtService.approveVendor(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.approveVendor(gmtVendor).getStatusCode());

        when(gmtService.ignoreRfqByVendor(any())).thenReturn(false);
        assertEquals(HttpStatus.OK,
                controller.ignoreRfqByVendor(Collections.singletonList(gmtVendor)).getStatusCode());

        when(gmtService.rejectRfqForVendor(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.rejectRfqForVendor(gmtVendor).getStatusCode());

        when(gmtService.raiseQueryByVendor(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.raiseQueryByVendor(gmtVendor).getStatusCode());

        when(gmtService.editUser(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.editUser(user).getStatusCode());

        when(gmtService.acceptSelfClient(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.acceptClient(user).getStatusCode());

        when(gmtService.ignoreClient(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.ignoreClient(user).getStatusCode());

        when(gmtService.disableUser(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.disableUser(user).getStatusCode());

        when(gmtService.fetchAllClientGMTRfqsForCMSearch(anyString(), anyString())).thenReturn(null);
        assertEquals(HttpStatus.OK,
                controller.fetchAllClientGMTRfqsForCMSearch("type", "value").getStatusCode());

        when(gmtService.getAllVendorsSearch(anyString(), anyString())).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.getAllVendorsSearch("type", "value").getStatusCode());

        when(gmtService.createRFQByClient(any())).thenReturn(Collections.emptyMap());
        assertEquals(HttpStatus.OK, controller.createRFQByClient(rfq).getStatusCode());

        when(gmtService.getSubscriptionPlans()).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.getSubscriptionPlans().getStatusCode());
    }
}
