package com.portal.procucev.service;

import com.portal.procucev.Dto.*;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.customexception.RfqStatusResponse;
import com.portal.procucev.dao.*;
import com.portal.procucev.model.*;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusConstants;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GMTServiceImplTest {

    @Mock
    private RfqDao rfqDao;
    @Mock
    private RFQItemsDao rfqItemsDao;
    @Mock
    private GmtRfqVendorDao gmtRfqVendorDao;
    @Mock
    private OrgDao orgDao;
    @Mock
    private UserDao userDao;
    @Mock
    private CategoryDivisionDao categoryDivisionDao;
    @Mock
    private MasterStatusDao masterStatusDao;
    @Mock
    private GmtItemsDao gmtItemsDao;
    @Mock
    private OrgTypeDao orgTypeDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private ItemCategoryDao itemCategoryDao;
    @Mock
    private OrgCategoryDivisionDao orgCategoryDivisionDao;
    @Mock
    private RfqVendorDao rfqVendorDao;
    @Mock
    private SubscriptionPlanDao subscriptionPlanDao;
    @Mock
    private EmailUserRepo emailUserRepo;
    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private AutomaticRfqService automaticRfqService;
    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private GMTServiceImpl service;

    private User user;
    private Organization org;
    private Rfq rfq;
    private MasterStatus masterStatus;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "host", "http://localhost");
        ReflectionTestUtils.setField(service, "mailFom", "from@test.com");

        org = new Organization();
        org.setId("ORG1");
        org.setCompanyName("Company1");
        org.setRfqCredits(15);

        user = new User();
        user.setId("USER1");
        user.setUsername("user1@test.com");
        user.setPhone("9876543210");
        user.setOrg(org);

        masterStatus = new MasterStatus();
        masterStatus.setStatus("OPEN");

        rfq = new Rfq();
        rfq.setId("RFQ_UUID");
        rfq.setRfqId("RFQ1");
        rfq.setOrg(org);
        rfq.setStatus(masterStatus);

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        when(subscriptionPlanDao.findById("2001")).thenReturn(Optional.of(new SubscriptionPlan()));
        when(userDao.findById(anyString())).thenReturn(Optional.of(user));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(userDao.findByUsernameAndActive(anyString(), eq(true))).thenReturn(user);
        when(orgDao.findRfqCreditsByOrg(anyString())).thenReturn(10);
        when(automaticRfqService.generateRfqId(anyString())).thenReturn("RFQ250101000001");
    }

    @Test
    void testCreateRFQForNoPrByClient() {
        assertDoesNotThrow(() -> service.createRFQForNoPrByClient(rfq));
    }
    void testGetClientRfqIds_NullUser_And_EmptyList() {
        assertThrows(AppException.class, () -> service.getClientRfqIds(null));
        when(rfqDao.findRFQIdsNoPrRfqByClient(anyString())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getClientRfqIds(user));
    }

    @Test
    void testGetNoPrRfqByClient_NullUser_And_EmptyList() {
        assertThrows(AppException.class, () -> service.getNoPrRfqByClient(null));
        when(rfqDao.findNoPrRfqByClient(anyString())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getNoPrRfqByClient(user));
    }

    @Test
    void testGetAllDivision_Empty() {
        when(categoryDivisionDao.getAllDivision()).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllDivision());
    }

    @Test
    void testCreateRFQForNoPrByClient_DataAccessException() throws Exception {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(gmtItemsDao.saveAll(any())).thenThrow(new org.springframework.dao.ConcurrencyFailureException("err"));
        assertFalse(service.createRFQForNoPrByClient(rfq));
    }

    @Test
    void testEditRFQForNoPrByClient_Branches() throws Exception {
        rfq.setId("RFQ_UUID");
        when(rfqDao.findById("RFQ_UUID")).thenReturn(Optional.empty());
        assertFalse(service.editRFQForNoPrByClient(rfq));

        Date existingDate = new Date();
        Rfq existingRfq = new Rfq();
        existingRfq.setId("RFQ_UUID");
        existingRfq.setDeliveryDate(existingDate);
        when(rfqDao.findById("RFQ_UUID")).thenReturn(Optional.of(existingRfq));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        Rfq update1 = new Rfq();
        update1.setId("RFQ_UUID");
        update1.setByClient(true);
        update1.setFromClient(true);
        update1.setDeliveryDate(new Date(existingDate.getTime() + 10000));
        RfqItem item1 = new RfqItem();
        item1.setId("ITEM1");
        item1.setDescription("Desc");
        update1.setRfqItem(Collections.singletonList(item1));
        assertTrue(service.editRFQForNoPrByClient(update1));

        Rfq update2 = new Rfq();
        update2.setId("RFQ_UUID");
        update2.setByClient(true);
        update2.setFromClient(false);
        update2.setDeliveryDate(null);
        update2.setRfqItem(Collections.singletonList(item1));
        assertTrue(service.editRFQForNoPrByClient(update2));
        assertEquals(existingDate, update2.getDeliveryDate());
    }

    @Test
    void testConvertRFQBoq_EmptyBoq_ThrowsAppException() {
        rfq.setBoqfile(new byte[0]);
        assertThrows(AppException.class, () -> service.convertRFQBoq(rfq));
    }

    @Test
    void testConvertRFQBoq() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");

        // Row 0: Dummy row
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("Dummy Header");

        // Row 1: Headers matching ApplicationConstants.REQUIRED_FORMAT_RFQITEMFORMAT
        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("S.No");
        r1.createCell(1).setCellValue("ItemDescription");
        r1.createCell(2).setCellValue(" Specification");
        r1.createCell(3).setCellValue("Uom");
        r1.createCell(4).setCellValue("Quantity");
        r1.createCell(5).setCellValue("Remarks");

        // Row 2: Data row
        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue(1.0);
        r2.createCell(1).setCellValue("Item 1 Description");
        r2.createCell(2).setCellValue("Brand Spec");
        r2.createCell(3).setCellValue("PCS");
        r2.createCell(4).setCellValue(10.0);
        r2.createCell(5).setCellValue("Test Remarks");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        rfq.setBoqfile(out.toByteArray());
        assertDoesNotThrow(() -> service.convertRFQBoq(rfq));
    }

    @Test
    void testQueryMail() {
        assertDoesNotThrow(() -> service.queryMail(user));
    }

    @Test
    void testRequestRfqByVendors_And_Sellers() {
        GmtRfqVendors vendor = new GmtRfqVendors();
        vendor.setRfq(rfq);
        vendor.setVendor(org);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        assertDoesNotThrow(() -> service.requestRfqByVendors(Collections.singletonList(vendor)));
        assertDoesNotThrow(() -> service.requestRfqBySellers(Collections.singletonList(vendor)));
    }

    @Test
    void testGetVendorsByGmtRfq() {
        GmtRfqVendors vendor = new GmtRfqVendors();
        vendor.setVendor(org);
        vendor.setStatus(masterStatus);
        when(gmtRfqVendorDao.findByRfq(any())).thenReturn(Collections.singletonList(vendor));

        assertDoesNotThrow(() -> service.getVendorsByGmtRfq(rfq));
    }

    @Test
    void testApproveVendor() {
        GmtRfqVendors vendor = new GmtRfqVendors();
        vendor.setId("10");
        vendor.setRfq(rfq);
        vendor.setVendor(org);
        when(gmtRfqVendorDao.findById(anyString())).thenReturn(Optional.of(vendor));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(userDao.findByOrganization(anyString())).thenReturn(Collections.singletonList(user));

        assertDoesNotThrow(() -> service.approveVendor(vendor));
    }

    @Test
    void testIgnoreRfqByVendor_And_RejectRfqForVendor() {
        GmtRfqVendors vendor = new GmtRfqVendors();
        vendor.setId("10");
        vendor.setRfq(rfq);
        when(gmtRfqVendorDao.findById(anyString())).thenReturn(Optional.of(vendor));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        assertDoesNotThrow(() -> service.ignoreRfqByVendor(Collections.singletonList(vendor)));
        assertDoesNotThrow(() -> service.rejectRfqForVendor(vendor));
    }

    @Test
    void testRaiseQueryByVendor() {
        GmtRfqVendors vendor = new GmtRfqVendors();
        vendor.setId("10");
        when(gmtRfqVendorDao.findById(anyString())).thenReturn(Optional.of(vendor));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        assertDoesNotThrow(() -> service.raiseQueryByVendor(vendor));
    }

    @Test
    void testGetCategoryByDivision() {
        when(categoryDivisionDao.getCategoryByDivision(any())).thenReturn(Collections.singletonList("Cat1"));
        assertDoesNotThrow(() -> service.getCategoryByDivision(new CategoryDivision()));
    }

    @Test
    void testGetAllGmtItems() {
        when(gmtItemsDao.findAll()).thenReturn(Collections.singletonList(new GmtItems()));
        List<GmtItems> res = service.getAllGmtItems();
        assertEquals(1, res.size());
    }

    @Test
    void testFetchRfqById() {
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);
        when(rfqDao.findById(anyString())).thenReturn(Optional.of(rfq));
        assertDoesNotThrow(() -> service.fetchRfqById(rfq));
    }

    @Test
    void testGetAllCategory() {
        when(categoryDivisionDao.getAllCategory()).thenReturn(Collections.singletonList("Cat1"));
        assertDoesNotThrow(() -> service.getAllCategory());
    }

    @Test
    void testCreateRFQWithNoPr() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(rfqDao.save(any())).thenReturn(rfq);

        assertDoesNotThrow(() -> service.createRFQWithNoPr(rfq));
    }

    @Test
    void testForwardRfqForNoPr() {
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(rfqDao.save(any())).thenReturn(rfq);

        assertDoesNotThrow(() -> service.forwardRfqForNoPr(rfq));
    }

    @Test
    void testGetItemsbyrfqrid() {
        when(rfqItemsDao.findByRfq(any())).thenReturn(Collections.singletonList(new RfqItem()));
        List<RfqItem> res = service.getItemsbyrfqrid(rfq);
        assertEquals(1, res.size());
    }

    @Test
    void testEditUser_And_AcceptSelfClient_And_IgnoreClient_And_DisableUser() {
        Role r = new Role();
        r.setRoleName("ROLE_USER");
        user.setRole(r);
        when(userDao.findById(anyString())).thenReturn(Optional.of(user));
        when(userDao.findByUsernameAndActive(anyString(), eq(true))).thenReturn(user);
        when(orgDao.findById(anyString())).thenReturn(Optional.of(org));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(r);

        assertDoesNotThrow(() -> service.editUser(user));
        assertDoesNotThrow(() -> service.acceptSelfClient(user));
        assertDoesNotThrow(() -> service.ignoreClient(user));
        assertDoesNotThrow(() -> service.disableUser(user));
    }

    @Test
    void testGetclientusersByClientId() {
        when(userDao.findByOrganization(anyString())).thenReturn(Collections.singletonList(user));
        assertDoesNotThrow(() -> service.getclientusersByClientId(org));
    }

    @Test
    void testEditAndResendRfq() {
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(rfqDao.save(any())).thenReturn(rfq);

        assertDoesNotThrow(() -> service.editAndResendRfq(rfq));
    }

    @Test
    void testCreateRFQByClient() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(rfqDao.save(any())).thenReturn(rfq);

        assertDoesNotThrow(() -> service.createRFQByClient(rfq));
    }

    @Test
    void testSendEmail() {
        EmailRequest req = new EmailRequest();
        req.setSubject("Subj");
        req.setBody("Msg");
        req.setTo(Collections.singletonList("to@test.com"));

        assertDoesNotThrow(() -> service.sendEmail(req));
    }

    @Test
    void testGetSubscriptionPlans() {
        when(subscriptionPlanDao.findAll()).thenReturn(Collections.singletonList(new SubscriptionPlan()));
        List<SubscriptionPlan> res = service.getSubscriptionPlans();
        assertEquals(1, res.size());
    }

    @Test
    void testGetRfqStatuses_And_SellerStatuses() {
        RfqStatusRequest req = new RfqStatusRequest();
        req.setRfqIds(Collections.singletonList("RFQ1"));
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);

        assertDoesNotThrow(() -> service.getRfqStatuses(req));
        assertDoesNotThrow(() -> service.getRfqSellerStatuses(req));
    }

    @Test
    void testGetSellerRfqCredits() {
        when(orgDao.findRfqCreditsByOrg(anyString())).thenReturn(15);
        when(orgDao.findById(anyString())).thenReturn(Optional.of(org));
        int credits = service.getSellerRfqCredits(org);
        assertEquals(15, credits);
    }

    @Test
    void testGetClientRfqIds_NullUser_EmptyList_Success() {
        assertThrows(AppException.class, () -> service.getClientRfqIds(null));

        when(rfqDao.findRFQIdsNoPrRfqByClient("USER1")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getClientRfqIds(user));

        when(rfqDao.findRFQIdsNoPrRfqByClient("USER1")).thenReturn(Collections.singletonList("RFQ1"));
        List<String> res = service.getClientRfqIds(user);
        assertEquals(1, res.size());
    }

    @Test
    void testGetNoPrRfqByClient_NullUser_EmptyList_Success() {
        assertThrows(AppException.class, () -> service.getNoPrRfqByClient(null));

        when(rfqDao.findNoPrRfqByClient("USER1")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getNoPrRfqByClient(user));

        rfq = new Rfq();
        rfq.setId("100");
        rfq.setRfqId("RFQ100");
        when(rfqDao.findNoPrRfqByClient("USER1")).thenReturn(Collections.singletonList(rfq));
        List<ClientRFQDto> res = service.getNoPrRfqByClient(user);
        assertEquals(1, res.size());
    }

    @Test
    void testGetAllDivision_Null_Success() {
        when(categoryDivisionDao.getAllDivision()).thenReturn(null);
        assertThrows(AppException.class, () -> service.getAllDivision());

        when(categoryDivisionDao.getAllDivision()).thenReturn(Collections.singletonList("DIV1"));
        List<String> res = service.getAllDivision();
        assertEquals(1, res.size());
    }

    @Test
    void testCreateRFQForNoPrByClient_Success_Exception() throws Exception {
        Rfq createRfq = new Rfq();
        createRfq.setRfqId("RFQ100");
        RfqItem item = new RfqItem();
        item.setId("ITM1");
        item.setDescription("Desc");
        item.setBrand("Brand");
        item.setRemarks("Remark");
        createRfq.setRfqItem(Collections.singletonList(item));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(rfqDao.save(any())).thenReturn(createRfq);

        assertTrue(service.createRFQForNoPrByClient(createRfq));
    }

    @Test
    void testConvertRFQBoq_Null_And_Excel() throws Exception {
        rfq = new Rfq();
        rfq.setBoqfile(new byte[0]);
        assertThrows(AppException.class, () -> service.convertRFQBoq(rfq));

        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Sheet1");
        sheet.createRow(0); // row 0
        Row row1 = sheet.createRow(1);
        String[] headers = new String[]{"S.No", "ItemDescription", "Specification", "Uom", "Quantity", "Remarks"};
        for (int i = 0; i < headers.length; i++) {
            row1.createCell(i).setCellValue(headers[i]);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        rfq.setBoqfile(out.toByteArray());
        assertThrows(AppException.class, () -> service.convertRFQBoq(rfq));
    }

    @Test
    void testGetOrgById_And_GetOrgByUserId() throws Exception {
        when(orgDao.findById(anyString())).thenReturn(Optional.of(org));
        assertEquals(org, service.getOrgById(org));

        when(userDao.findById(anyString())).thenReturn(Optional.of(user));
        assertEquals(org, service.getOrgByUserId(user));
    }

    @Test
    void testForwardRfqsToVendor() {
        ForwardRfqVendorRequest req = new ForwardRfqVendorRequest();
        req.setSellerId("ORG1");
        req.setRfqIds(Collections.singletonList("RFQ1"));

        when(orgDao.findById(anyString())).thenReturn(Optional.of(org));
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        assertDoesNotThrow(() -> service.forwardRfqsToVendor(req));
    }

    @Test
    void testGetLastOpenRfqsForVendor() {
        assertDoesNotThrow(() -> service.getLastOpenRfqsForVendor("ORG1"));
    }

    @Test
    void testGetVendorInfo() {
        when(orgDao.findById(anyString())).thenReturn(Optional.of(org));
        assertDoesNotThrow(() -> service.getVendorInfo(org));
    }

    @Test
    void testEmailForwarder() {
        assertDoesNotThrow(() -> service.emailForwarder());
    }

    @Test
    void testGetBuyerDataByRFQ() {
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);
        assertDoesNotThrow(() -> service.getBuyerDataByRFQ(rfq));
    }

    @Test
    void testGenerateOtp_And_ValidateOtp() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(orgDao.findById(anyString())).thenReturn(Optional.of(org));

        assertDoesNotThrow(() -> service.generateOtp(org, req));

        org.setEmail("test@org.com");
        assertDoesNotThrow(() -> service.validateOtp(org));
    }

    @Test
    void testSubmitUpgradeVendor() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId("2001");
        org.setSubscriptionPlan(plan);
        when(orgDao.findById(anyString())).thenReturn(Optional.of(org));
        when(subscriptionPlanDao.findById("2001")).thenReturn(Optional.of(plan));
        when(userDao.findByOrganization(anyString())).thenReturn(Collections.singletonList(user));

        assertDoesNotThrow(() -> service.submitUpgradeVendor(org));
    }

    @Test
    void testGetSellerRfqStatusData() {
        RfqStatusRequest req = new RfqStatusRequest();
        req.setRfqIds(Collections.singletonList("RFQ1"));
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);

        assertDoesNotThrow(() -> service.getSellerRfqStatusData(req));
    }

    @Test
    void testGetBuyerByRFQ() {
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);
        assertDoesNotThrow(() -> service.getBuyerByRFQ(rfq));
    }

    @Test
    void testMarkVendorCommentAsRead() {
        when(rfqDao.findByRfqId(anyString())).thenReturn(rfq);
        assertDoesNotThrow(() -> service.markVendorCommentAsRead(rfq));
    }

    @Test
    void testGetAllGMTRfq_Populated() {
        rfq.setId("RFQ_ID_1");
        rfq.setProjectDesc("Proj Desc");

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("Bengaluru");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(rfqDao.findAllRfqNoPrInStatuses(any())).thenReturn(Collections.singletonList(rfq));

        GmtRfqVendors gmtVendor = new GmtRfqVendors();
        gmtVendor.setRfq(rfq);
        gmtVendor.setStatus(masterStatus);
        gmtVendor.setRequestedDate(new Date());
        when(gmtRfqVendorDao.findByVendorAndRfqIn(any(), any())).thenReturn(Collections.singletonList(gmtVendor));

        List<Object[]> catData = new ArrayList<>();
        catData.add(new Object[]{"RFQ_ID_1", "Category A"});
        when(rfqItemsDao.findTopCategoriesByRfqIds(any())).thenReturn(catData);

        RfqItem rfqItem = new RfqItem();
        rfqItem.setRfq(rfq);
        rfqItem.setDescription("Item A");
        when(rfqItemsDao.findAllByRfqIds(any())).thenReturn(Collections.singletonList(rfqItem));

        List<GMTRfqVendorDto> res = service.getAllGMTRfq(org);
        assertNotNull(res);
        assertEquals(1, res.size());
    }

    @Test
    void testSetUserDetails_And_GenerateUserId() {
        org.setOrganizationPhonenumber("9876543210");
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        Role vRole = new Role();
        vRole.setRoleName("VENDOR");
        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(vRole);
        when(userDao.save(any())).thenReturn(user);

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        User u1 = service.setUserDetails(org, new User());
        assertNotNull(u1);

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        User u2 = service.setUserDetails(org, new User());
        assertEquals(user, u2);

        String uid = service.generateUserId("9876543210");
        assertTrue(uid.startsWith("USR"));
        assertThrows(IllegalArgumentException.class, () -> service.generateUserId("12"));
    }

    @Test
    void testPreviouslyUncoveredSimpleFailurePaths() throws Exception {
        assertThrows(AppException.class, () -> service.getCategoryByDivision(new CategoryDivision()));
        assertThrows(AppException.class, () -> service.getAllGmtItems());
        assertThrows(AppException.class, () -> service.fetchRfqById(rfq));
        assertThrows(AppException.class, () -> service.getAllCategory());
        assertThrows(AppException.class, () -> service.getItemsbyrfqrid(rfq));
        assertThrows(AppException.class, () -> service.editUser(null));
        assertThrows(AppException.class, () -> service.acceptSelfClient(null));
        assertThrows(AppException.class, () -> service.ignoreClient(null));
        when(userDao.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.disableUser(user));
        assertThrows(AppException.class, () -> service.getOrgById(null));
        assertThrows(AppException.class, () -> service.getOrgById(new Organization()));
        assertThrows(AppException.class, () -> service.getOrgByUserId(null));
        assertThrows(AppException.class, () -> service.getOrgByUserId(new User()));
        assertThrows(AppException.class, () -> service.queryMail(null));
        assertFalse(service.approveVendor(null));
        assertNull(service.getVendorInfo(new Organization()));
        assertNull(service.getBuyerDataByRFQ(null));
        assertNull(service.getBuyerByRFQ(null));
        assertThrows(AppException.class, () -> service.generateOtp(null, mock(HttpServletRequest.class)));
        assertThrows(NullPointerException.class, () -> service.validateOtp(new Organization()));
        assertFalse(service.submitUpgradeVendor(null));
        assertEquals(0, service.getSellerRfqCredits(null));
    }

    @Test
    void testRequestAndVendorMappingBranches() {
        GmtRfqVendors input = new GmtRfqVendors();
        input.setVendor(org);
        input.setRfq(rfq);
        GmtRfqVendors existing = new GmtRfqVendors();
        when(gmtRfqVendorDao.findByVendorAndRfq(org, rfq)).thenReturn(existing);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        assertTrue(service.requestRfqByVendors(List.of(input)));
        assertTrue(service.ignoreRfqByVendor(List.of(input)));
        assertTrue(service.rejectRfqForVendor(input));
        assertTrue(service.raiseQueryByVendor(input));

        when(gmtRfqVendorDao.findByVendorAndRfq(org, rfq)).thenThrow(new RuntimeException("db"));
        assertFalse(service.requestRfqByVendors(List.of(input)));
        assertFalse(service.ignoreRfqByVendor(List.of(input)));
        assertFalse(service.rejectRfqForVendor(input));
        assertFalse(service.raiseQueryByVendor(input));
        assertFalse(service.raiseQueryByVendor(null));
    }

    @Test
    void testGetVendorsByGmtRfqValidationAndNullFields() {
        assertThrows(NullPointerException.class, () -> service.getVendorsByGmtRfq(null));
        when(gmtRfqVendorDao.findByRfq(rfq)).thenReturn(null);
        assertThrows(AppException.class, () -> service.getVendorsByGmtRfq(rfq));
        when(gmtRfqVendorDao.findByRfq(rfq)).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getVendorsByGmtRfq(rfq));

        GmtRfqVendors mapping = new GmtRfqVendors();
        mapping.setRfq(rfq);
        when(gmtRfqVendorDao.findByRfq(rfq)).thenReturn(List.of(mapping));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        List<GmtRfqSellerDto> result = service.getVendorsByGmtRfq(rfq);
        assertEquals(1, result.size());
        assertNull(result.get(0).getVendorUuid());
        assertSame(masterStatus, result.get(0).getStatus());
    }

    @Test
    void testGetAllRfqForCmAndClientLists() {
        when(masterStatusDao.findByStatusIn(any())).thenReturn(List.of(masterStatus));
        when(rfqDao.findAllRfqNoPrByCM(anyList())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllRfqForCM());

        rfq.setClientStatus(null);
        rfq.setUser("USER1");
        when(rfqDao.findAllRfqNoPrByCM(anyList())).thenReturn(List.of(rfq));
        when(gmtRfqVendorDao.findByVendorsByRfq(anyString())).thenReturn(2L);
        when(userDao.findByUser("USER1")).thenReturn("Company");
        when(userDao.findOrgIdByUser("USER1")).thenReturn("ORG1");
        when(userDao.findPhoneByUser("USER1")).thenReturn("9999999999");
        assertEquals(1, service.getAllRfqForCM().size());

        when(rfqDao.findAllClientRfqNoPr()).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.fetchAllClientGMTRfqsForCM());
        rfq.setClientStatus(masterStatus);
        when(rfqDao.findAllClientRfqNoPr()).thenReturn(List.of(rfq));
        assertEquals(1, service.fetchAllClientGMTRfqsForCM().size());
    }

    @Test
    void testPagedClientRfqAndSearchBranches() {
        Pageable pageable = PageRequest.of(0, 5);
        when(rfqDao.findAllClientRfqNoPr(pageable)).thenReturn(new PageImpl<>(Collections.emptyList()));
        assertThrows(AppException.class, () -> service.fetchAllClientGMTRfqsForCM(pageable));

        rfq.setUser("USER1");
        user.setId("USER1");
        when(rfqDao.findAllClientRfqNoPr(pageable)).thenReturn(new PageImpl<>(List.of(rfq)));
        when(userDao.findUsersByIds(any())).thenReturn(List.of(user));
        when(gmtRfqVendorDao.countVendorsByRfqIds(any())).thenReturn(List.<Object[]>of(new Object[]{rfq.getId(), 3L}));
        assertEquals(1, service.fetchAllClientGMTRfqsForCM(pageable).getData().size());

        assertTrue(service.fetchAllClientGMTRfqsForCMSearch("unknown", "x").isEmpty());
        when(rfqDao.findAllClientRfqByRfqIdOrDescription(anyString(), anyString())).thenReturn(Collections.emptyList());
        assertTrue(service.fetchAllClientGMTRfqsForCMSearch("rfqid", "RFQ1").isEmpty());
        when(userDao.findUsersByOrgCompanyName("Company")).thenReturn(Collections.emptyList());
        assertTrue(service.fetchAllClientGMTRfqsForCMSearch("companyname", "Company").isEmpty());
        when(userDao.findUsersByPhone("999")).thenReturn(Collections.emptyList());
        assertTrue(service.fetchAllClientGMTRfqsForCMSearch("contactnumber", "999").isEmpty());

        when(rfqDao.findAllClientRfqByRfqIdOrDescription(anyString(), anyString())).thenReturn(List.of(rfq));
        when(userDao.findUsersByIds(any())).thenReturn(List.of(user));
        assertEquals(1, service.fetchAllClientGMTRfqsForCMSearch("description", "x").size());
    }

    @Test
    void testVendorDirectoryBranches() {
        OrgType type = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(type);
        when(orgDao.getAllVendor(type)).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllVendors());
        when(orgDao.getAllVendor(type)).thenReturn(List.<Object[]>of(new Object[]{"1", "Co", "V1", "999", "City", "v@x.com"}));
        assertEquals(1, service.getAllVendors().size());

        Pageable pageable = PageRequest.of(0, 5);
        when(orgDao.getAllVendor(type, pageable)).thenReturn(new PageImpl<>(Collections.emptyList()));
        assertThrows(AppException.class, () -> service.getAllVendors(pageable));
        VendorRFQDto dto = new VendorRFQDto();
        when(orgDao.getAllVendor(type, pageable)).thenReturn(new PageImpl<>(List.of(dto)));
        assertEquals(1, service.getAllVendors(pageable).getData().size());

        when(orgDao.searchVendorByType(type, "company", "Co")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllVendorsSearch("company", "Co"));
        when(orgDao.searchVendorByType(type, "company", "Co")).thenReturn(List.of(dto));
        assertEquals(1, service.getAllVendorsSearch("company", "Co").size());
        when(orgDao.getAllVendorByCategory(any())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllVendorsByCategory(org));
        when(orgDao.getAllVendorByCategory(any())).thenReturn(List.of(dto));
        assertEquals(1, service.getAllVendorsByCategory(org).size());
    }

    @Test
    void testRfqVendorDetailsUserAndOrganizationFallback() {
        RfqVendor vendor = new RfqVendor();
        vendor.setId("RV1");
        vendor.setOrganization(org);
        rfq.setRfqVendor(List.of(vendor));
        when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));
        when(userDao.findByOrg(org)).thenReturn(List.of(user));
        assertEquals("user1@test.com", service.getVendorsbyRFQ(rfq).get(0).getEmail());

        org.setEmail("fallback@test.com");
        org.setOrganizationPhonenumber("9999999999");
        when(userDao.findByOrg(org)).thenReturn(Collections.emptyList());
        assertEquals("fallback@test.com", service.getVendorsbyRFQ(rfq).get(0).getEmail());
        rfq.setRfqVendor(Collections.emptyList());
        assertTrue(service.getVendorsbyRFQ(rfq).isEmpty());
    }

    @Test
    void testSelfRegisteredClientsAndBuyerLookupBranches() {
        OrgType type = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(type);
        when(orgDao.findByOrgTypeAndSelfClient(eq(type), eq(true), any())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.fetchSelfRegisterClients());
        when(orgDao.findByOrgTypeAndSelfClient(eq(type), eq(true), any())).thenReturn(List.of(org));
        assertEquals(1, service.fetchSelfRegisterClients().size());

        Rfq lookup = new Rfq();
        lookup.setId("LOOKUP");
        when(rfqDao.findById("LOOKUP")).thenReturn(Optional.empty());
        assertNull(service.getBuyerDataByRFQ(lookup));
        when(rfqDao.findById("LOOKUP")).thenReturn(Optional.of(lookup));
        assertNull(service.getBuyerDataByRFQ(lookup));
        lookup.setUser("USER1");
        when(userDao.findUserById("USER1")).thenReturn(user);
        assertSame(user, service.getBuyerDataByRFQ(lookup));

        when(rfqDao.findClientById("LOOKUP")).thenReturn(null);
        assertNull(service.getBuyerByRFQ(lookup));
        when(rfqDao.findClientById("LOOKUP")).thenReturn("ORG1");
        when(userDao.findUserByOrgId("ORG1")).thenReturn(user);
        assertSame(user, service.getBuyerByRFQ(lookup));
    }

    @Test
    void testEditUserDisableAndClientLookupBranches() {
        user.setEmailMatched(false);
        when(userDao.findOrgIdByUser(user.getId())).thenReturn("ORG1");
        assertTrue(service.editUser(user));
        user.setEmailMatched(true);
        assertTrue(service.editUser(user));

        when(userDao.findById(user.getId())).thenReturn(Optional.of(user));
        when(userDao.findByOrg("ORG1")).thenReturn(Collections.emptyList());
        assertTrue(service.disableUser(user));
        when(userDao.findByOrgAndActive("ORG1")).thenReturn(null);
        assertThrows(AppException.class, () -> service.getclientusersByClientId(org));
        when(userDao.findByOrgAndActive("ORG1")).thenReturn(List.of(user));
        assertEquals(1, service.getclientusersByClientId(org).size());
    }

    @Test
    void testGmtBuyersBranches() {
        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(null);
        assertTrue(service.getGmtBuyers().isEmpty());
        Role role = new Role();
        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(role);
        when(userDao.getUsersBySelfClientAndRole(role)).thenReturn(null);
        assertNull(service.getGmtBuyers());
        when(userDao.getUsersBySelfClientAndRole(role)).thenReturn(List.of(user));
        assertEquals(1, service.getGmtBuyers().size());
        when(userDao.getUsersBySelfClientAndRole(role)).thenThrow(new RuntimeException("db"));
        assertTrue(service.getGmtBuyers().isEmpty());
    }

    @Test
    void testSendEmailNormalInvalidAttachmentAndFailure() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailRequest request = new EmailRequest();
        request.setTo(List.of("valid@example.com"));
        request.setCc(List.of("copy@example.com"));
        request.setBcc(List.of("blind@example.com"));
        request.setSubject("subject");
        request.setBody("body");

        try (org.mockito.MockedStatic<com.portal.procucev.utils.EmailValidatorUtil> validator =
                     mockStatic(com.portal.procucev.utils.EmailValidatorUtil.class)) {
            assertNotNull(service.sendEmail(request));

            validator.when(() -> com.portal.procucev.utils.EmailValidatorUtil.validateEmails(any(), any()))
                    .thenAnswer(invocation -> {
                        List<String> invalid = invocation.getArgument(1);
                        invalid.add("invalid-address");
                        return null;
                    });
            request.setTo(List.of("invalid-address"));
            assertNotNull(service.sendEmail(request));

            validator.when(() -> com.portal.procucev.utils.EmailValidatorUtil.validateEmails(any(), any()))
                    .thenAnswer(invocation -> null);

            request.setTo(List.of("valid@example.com"));
            EmailAttachment attachment = new EmailAttachment();
            attachment.setFileName("a.txt");
            attachment.setContentType("text/plain");
            attachment.setFileData(Base64.getEncoder().encodeToString("data".getBytes()));
            request.setAttachments(List.of(attachment));
            assertNotNull(service.sendEmail(request));

            attachment.setFileData(" ");
            assertNotNull(service.sendEmail(request));
            attachment.setFileData("%%%not-base64%%%");
            assertNotNull(service.sendEmail(request));
        }
    }

    @Test
    void testStatusApisAllMajorBranches() {
        RfqStatusRequest request = new RfqStatusRequest();
        request.setClientId("USER1");
        request.setRfqIds(List.of("1", "RFQ2"));
        masterStatus.setUiDisplay("InProgress");
        rfq.setStatus(masterStatus);
        rfq.setRfqId("RFQ1");
        when(rfqDao.findByUserAndRfqIdIn(anyString(), any())).thenReturn(List.of(rfq));
        List<RfqStatusResponse> statuses = service.getRfqStatuses(request);
        assertEquals(2, statuses.size());
        assertEquals("Published To Multiple Sellers", statuses.get(0).getStatus());

        rfq.setQuotationReceived(true);
        assertEquals("Quotation_Received", service.getRfqStatuses(request).get(0).getStatus());
        request.setRfqIds(Collections.emptyList());
        when(rfqDao.findLast3ByClientId(anyString(), any())).thenReturn(Collections.emptyList());
        assertEquals("Not Found", service.getRfqStatuses(request).get(0).getStatus());
        rfq.setQuotationReceived(false);
        rfq.setStatus(null);
        when(rfqDao.findLast3ByClientId(anyString(), any())).thenReturn(List.of(rfq));
        assertEquals("Unknown", service.getRfqStatuses(request).get(0).getStatus());

        request.setRfqIds(List.of("1"));
        when(rfqDao.findRfqsByIdsExcludingSellerRequested(any(), anyString())).thenReturn(List.of(rfq));
        assertEquals("Unknown", service.getRfqSellerStatuses(request).get(0).getStatus());
        when(rfqDao.findRfqsByIdsExcludingSellerRequested(any(), anyString())).thenReturn(Collections.emptyList());
        assertEquals("Invalid RFQID", service.getRfqSellerStatuses(request).get(0).getStatus());
        request.setRfqIds(Collections.emptyList());
        when(rfqDao.findLatest5RfqsExcludingSellerRequested(anyString(), any())).thenReturn(Collections.emptyList());
        assertEquals("Not Found", service.getRfqSellerStatuses(request).get(0).getStatus());
    }

    @Test
    void testSellerStatusDataBranches() {
        RfqStatusRequest request = new RfqStatusRequest();
        request.setClientId("ORG1");
        request.setRfqIds(Arrays.asList(null, "1", "RFQ2"));
        GmtRfqVendors vendor = new GmtRfqVendors();
        vendor.setRfq(rfq);
        rfq.setRfqId("RFQ1");
        when(gmtRfqVendorDao.findByVendorUuidAndRfqIds(anyString(), any())).thenReturn(List.of(vendor));
        List<RfqStatusResponse> result = service.getSellerRfqStatusData(request);
        assertEquals("Downloaded", result.get(0).getStatus());
        vendor.setQuotationReceived(true);
        assertEquals("Quote_Submitted", service.getSellerRfqStatusData(request).get(0).getStatus());

        request.setRfqIds(Collections.emptyList());
        when(gmtRfqVendorDao.findLatest5ByVendorUuid(anyString(), any())).thenReturn(Collections.emptyList());
        assertEquals("Not Found", service.getSellerRfqStatusData(request).get(0).getStatus());
        when(gmtRfqVendorDao.findLatest5ByVendorUuid(anyString(), any())).thenReturn(List.of(vendor));
        assertEquals("Quote_Submitted", service.getSellerRfqStatusData(request).get(0).getStatus());
    }

    @Test
    void testOrgLookupAndCategoryRfqBranches() {
        Organization missing = new Organization();
        missing.setId("MISSING");
        when(orgDao.findById("MISSING")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.getOrgById(missing));

        Map<String, Object> invalid = service.getRfqByItemCategory(null);
        assertEquals(0L, invalid.get("count"));
        invalid = service.getRfqByItemCategory(new Organization());
        assertEquals(0L, invalid.get("count"));
        when(orgCategoryDivisionDao.findCategoryByOrg("ORG1")).thenReturn(List.of("Cat"));
        when(rfqDao.findTopRfqsByCategory(any(), anyString(), any(), any())).thenReturn(List.of(rfq));
        when(rfqDao.countByRfqItemCategory(any(), anyString())).thenReturn(1L);
        assertEquals(1L, service.getRfqByItemCategory(org).get("count"));
    }

    @Test
    void testForwardRfqsValidationFailures() {
        ForwardRfqVendorRequest request = new ForwardRfqVendorRequest();
        request.setSellerId("ORG1");
        request.setEmail("seller@example.com");
        request.setRfqIds(List.of("RFQ1"));

        when(orgDao.findRfqCreditsDataByOrg("ORG1")).thenReturn(null);
        assertNotNull(service.forwardRfqsToVendor(request).get("summary"));
        when(orgDao.findRfqCreditsDataByOrg("ORG1")).thenReturn(1);
        when(rfqDao.findByRfqId("RFQ1")).thenReturn(null);
        assertNotNull(service.forwardRfqsToVendor(request).get("summary"));
        when(rfqDao.findByRfqId("RFQ1")).thenReturn(rfq);
        request.setEmail("bad");
        assertNotNull(service.forwardRfqsToVendor(request).get("summary"));
        request.setEmail("seller@example.com");
        when(orgDao.findRfqCreditsDataByOrg("ORG1")).thenReturn(0);
        assertNotNull(service.forwardRfqsToVendor(request).get("summary"));
    }

    @Test
    void testLastOpenRfqsAndVendorInfo() {
        rfq.setCreatedTS(new Date());
        rfq.setRfqClosingDate(Date.from(java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.DAYS)));
        ClientDeliveryLocationRfq location = new ClientDeliveryLocationRfq();
        location.setCity("City");
        location.setState("State");
        rfq.setClientdeliverylocationrfq(List.of(location));
        GmtRfqVendors vendor = new GmtRfqVendors();
        vendor.setRfq(rfq);
        vendor.setRequestedDate(new Date());
        when(gmtRfqVendorDao.findLastOpenRfqsByVendor(eq("ORG1"), any())).thenReturn(List.of(vendor));
        assertEquals(1, service.getLastOpenRfqsForVendor("ORG1").size());

        OrgDivisionCategory category = new OrgDivisionCategory();
        category.setCategory("Cat");
        org.setDivisionCategories(List.of(category));
        when(orgDao.findById("ORG1")).thenReturn(Optional.of(org));
        assertEquals(List.of("Cat"), service.getVendorInfo(org).getCategories());
        org.setDivisionCategories(null);
        assertTrue(service.getVendorInfo(org).getCategories().isEmpty());
    }

    @Test
    void testUpdateVendorClassesAllClassifications() {
        OrgType type = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(null);
        assertDoesNotThrow(() -> service.updateVendorClasses());
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(type);

        Organization diamond = new Organization();
        diamond.setId("D");
        diamond.setQuoteSubmitted(3L);
        Organization gold = new Organization();
        gold.setId("G");
        gold.setQuoteSubmitted(1L);
        Organization marketing = new Organization();
        marketing.setId("M");
        marketing.setQuoteSubmitted(null);
        Organization unchanged = new Organization();
        unchanged.setId("U");
        unchanged.setQuoteSubmitted(0L);
        unchanged.setVendorClass(StatusConstants.Marketing);
        Organization optOut = new Organization();
        optOut.setId("O");
        optOut.setVendorClass(StatusConstants.Opt_Out);
        when(orgDao.findByOrgType(type)).thenReturn(List.of(diamond, gold, marketing, unchanged, optOut));
        assertDoesNotThrow(() -> service.updateVendorClasses());
        verify(orgDao).updateVendorClass("D", StatusConstants.Diamond);
        verify(orgDao).updateVendorClass("G", StatusConstants.Gold);
        verify(orgDao).updateVendorClass("M", StatusConstants.Marketing);
    }

    @Test
    void testOtpHappyWrongAndConsumed() {
        assertTrue(service.generateOtp(org, mock(HttpServletRequest.class)));
        @SuppressWarnings("unchecked")
        Map<String, String> otpMap = (Map<String, String>) ReflectionTestUtils.getField(service, "otpMap");
        assertNotNull(otpMap);
        org.setUserOtp("wrong");
        assertFalse(service.validateOtp(org));
        org.setUserOtp(otpMap.get(org.getId()));
        assertTrue(service.validateOtp(org));
        assertFalse(service.validateOtp(org));
    }

    @Test
    void testPrivateParsingAndCsvEscapingBranches() {
        assertEquals("RFQ123", ReflectionTestUtils.invokeMethod(service, "extractRfqId", "RFQ No RFQ123 - V-1"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractRfqId", "none"));
        assertEquals("V-1", ReflectionTestUtils.invokeMethod(service, "extractVendorId", "RFQ No RFQ123 - V-1"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "extractVendorId", "none"));
        assertEquals("", ReflectionTestUtils.invokeMethod(service, "escapeCsv", (String) null));
        assertEquals("plain", ReflectionTestUtils.invokeMethod(service, "escapeCsv", "plain"));
        assertEquals("\"a,b\"", ReflectionTestUtils.invokeMethod(service, "escapeCsv", "a,b"));
        assertEquals("\"a\"\"b\"", ReflectionTestUtils.invokeMethod(service, "escapeCsv", "a\"b"));
        assertEquals("\"a\nb\"", ReflectionTestUtils.invokeMethod(service, "escapeCsv", "a\nb"));
    }

    @Test
    void testDailyReportWorkflowWithEmptyData() {
        ReflectionTestUtils.setField(service, "fromEmail", "from@example.com");
        ReflectionTestUtils.setField(service, "toEmail", "to@example.com");
        when(userDao.getDailyBuyerSellerReport(any(), any())).thenReturn(Collections.emptyList());
        when(userDao.getDailySellerSubscriptionReport(any(), any())).thenReturn(Collections.emptyList());
        assertDoesNotThrow(() -> service.dailyReportEmailForwarder());
        verify(javaMailSender, atLeastOnce()).createMimeMessage();
    }

    @Test
    void testGenerateUserIdRejectsNullName() {
        assertThrows(IllegalArgumentException.class, () -> service.generateUserId(null));
    }

    @Test
    void testMailWorkflowEntryPointsWithoutExternalRecipients() throws Exception {
        org.springframework.security.core.Authentication authentication =
                mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.userdetails.UserDetails details =
                mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(authentication.getPrincipal()).thenReturn(details);
        when(details.getUsername()).thenReturn("buyer@example.com");
        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            rfq.setDeliveryDate(Date.from(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS)));
            rfq.setCreatedTS(new Date());
            rfq.setVendors(Collections.emptyList());
            when(masterStatusDao.findByStatusIn(any())).thenReturn(Collections.emptyList());
            when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(null);

            assertTrue(service.sendRfqToVendors(Collections.emptyList(), rfq));
            assertTrue(service.resendRfqToVendors(Collections.emptyList(), rfq));

            Rfq forwardingRequest = new Rfq();
            forwardingRequest.setVendors(Collections.emptyList());
            assertTrue(ReflectionTestUtils.<Boolean>invokeMethod(
                    service, "saveVendorsForForwardRfq", forwardingRequest, rfq));

            RfqVendor singleVendor = new RfqVendor();
            Rfq missing = new Rfq();
            missing.setId("MISSING");
            when(rfqDao.findById("MISSING")).thenReturn(Optional.empty());
            assertFalse(service.sendRfqsToVendor(singleVendor, missing));

            EmailRequest noRecipients = new EmailRequest();
            assertTrue(ReflectionTestUtils.<List<String>>invokeMethod(
                    service, "verifyRecipientsSMTP", noRecipients).isEmpty());

            String nearDate = ReflectionTestUtils.invokeMethod(
                    service, "buildingRfqDueDate", rfq.getDeliveryDate());
            String cappedDate = ReflectionTestUtils.invokeMethod(
                    service, "buildingRfqDueDate",
                    Date.from(java.time.Instant.now().plus(5, java.time.temporal.ChronoUnit.DAYS)));
            assertNotNull(nearDate);
            assertNotNull(cappedDate);

            when(userDao.getFullName("buyer@example.com")).thenReturn("Buyer");
            when(rfqDao.getRfqsByNoPrFlagIsTrue("Buyer")).thenReturn(List.of(rfq));
            assertEquals(1, service.getRFQsForNoPR().size());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void testSendRfqToVendorsPopulatedCoversAllMailVariants() throws Exception {
        ReflectionTestUtils.setField(service, "subjectPrefix", "PREFIX");
        ReflectionTestUtils.setField(service, "emailPassword", "secret");
        org.springframework.security.core.Authentication authentication =
                mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.userdetails.UserDetails details =
                mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(authentication.getPrincipal()).thenReturn(details);
        when(details.getUsername()).thenReturn("buyer@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try (org.mockito.MockedStatic<com.portal.procucev.utils.MailUtility> mail =
                     mockStatic(com.portal.procucev.utils.MailUtility.class)) {
            MasterStatus sent = new MasterStatus();
            sent.setStatus(StatusConstants.pcRfqSent);
            MasterStatus vendorNew = new MasterStatus();
            vendorNew.setStatus(StatusConstants.vendorRfqNew);
            when(masterStatusDao.findByStatusIn(any())).thenReturn(List.of(sent, vendorNew));

            EmailUser emailUser = new EmailUser();
            emailUser.setEmail("configured@example.com");
            emailUser.setPassword("configured-password");
            when(emailUserRepo.findByEmail("buyer@example.com")).thenReturn(emailUser);
            user.setFullName("Buyer Name");
            when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);

            Date today = Date.from(java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            Date yesterday = Date.from(java.time.LocalDate.now().minusDays(1)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true)))
                    .thenAnswer(invocation -> {
                        User vendorUser = new User();
                        vendorUser.setCreatedTS(invocation.<String>getArgument(0).contains("existing")
                                ? yesterday : today);
                        return vendorUser;
                    });

            List<Organization> vendors = new ArrayList<>();
            vendors.add(mailVendor("FNEW0", "forward-new0@example.com", "Forward"));
            vendors.add(mailVendor("FNEW1", "forward-new1@example.com", "Forward"));
            vendors.add(mailVendor("FEX", "forward-existing@example.com", "Forward"));
            vendors.add(mailVendor("INEW0", "invite-new0@example.com", "Invite"));
            vendors.add(mailVendor("INEW1", "invite-new1@example.com", "Invite"));
            vendors.add(mailVendor("IEX", "invite-existing@example.com", "Invite"));
            when(rfqVendorDao.countCredentialEmailsSent("FNEW0")).thenReturn(0L);
            when(rfqVendorDao.countCredentialEmailsSent("FNEW1")).thenReturn(1L);
            when(rfqVendorDao.countCredentialEmailsSent("INEW0")).thenReturn(0L);
            when(rfqVendorDao.countCredentialEmailsSent("INEW1")).thenReturn(1L);
            when(rfqVendorDao.findLatestByOrganizationUuid(anyString())).thenAnswer(invocation -> {
                RfqVendor value = new RfqVendor();
                value.setId("LATEST-" + invocation.getArgument(0));
                return value;
            });

            rfq.setDeliveryDate(Date.from(java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.DAYS)));
            rfq.setVendors(vendors);
            RfqVendor persisted = new RfqVendor();
            persisted.setRfq(rfq);
            when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));

            assertTrue(service.sendRfqToVendors(List.of(persisted), rfq));
            assertEquals("RFQ1", persisted.getRfqId());
            assertSame(sent, persisted.getProcucevStatus());
            assertSame(vendorNew, persisted.getVendorStatus());
            verify(rfqVendorDao).saveAll(anyList());
            verify(rfqVendorDao, times(2)).save(any(RfqVendor.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Organization mailVendor(String id, String email, String requestType) {
        Organization vendor = new Organization();
        vendor.setId(id);
        vendor.setEmail(email);
        vendor.setOrganizationPhonenumber("9876543210");
        vendor.setOtherEmails("copy@example.com");
        vendor.setRequestType(requestType);
        return vendor;
    }

    @Test
    void testSaveVendorsForForwardRfqPopulatedNewAndExisting() throws Exception {
        SelfRegistrationService registration = mock(SelfRegistrationService.class);
        ReflectionTestUtils.setField(service, "selfRegistrationService", registration);
        GMTServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).sendRfqToVendors(anyList(), same(rfq));

        Organization newVendor = mailVendor(null, "new@example.com", ApplicationConstants.Invite);
        newVendor.setCompanyName("New Company");
        Organization existingInput = mailVendor("EXIST", "existing@example.com", "Forward");
        existingInput.setOtherEmails("updated@example.com");
        Organization existingStored = mailVendor("EXIST", "stored@example.com", "Forward");
        existingStored.setOrganizationPhonenumber("9123456789");
        Rfq request = new Rfq();
        request.setVendors(List.of(newVendor, existingInput));

        when(registration.checkOrgexist("New Company")).thenReturn(false);
        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(new OrgType());
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(orgDao.save(newVendor)).thenAnswer(invocation -> {
            newVendor.setId("NEW");
            return newVendor;
        });
        when(orgDao.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.of("EXIST".equals(id) ? existingStored : newVendor);
        });
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        GmtRfqVendors alreadyMapped = new GmtRfqVendors();
        when(gmtRfqVendorDao.findByVendorAndRfq(any(), same(rfq)))
                .thenAnswer(invocation -> invocation.<Organization>getArgument(0).getId().equals("EXIST")
                        ? alreadyMapped : null);

        assertTrue(ReflectionTestUtils.<Boolean>invokeMethod(
                spyService, "saveVendorsForForwardRfq", request, rfq));
        verify(orgDao).save(newVendor);
        verify(orgDao).updateOtherEmail("updated@example.com", "EXIST");
        verify(gmtRfqVendorDao).save(any(GmtRfqVendors.class));
        verify(spyService).sendRfqToVendors(argThat(list -> list.size() == 2), same(rfq));
    }

    @Test
    void testEditAndResendRfqFullSuccessNoVendorsAndDataFailure() throws Exception {
        GMTServiceImpl spyService = spy(service);
        Rfq updated = new Rfq();
        updated.setId("EDIT");
        updated.setRfqId("RFQ-EDIT");
        RfqItem existingItem = new RfqItem();
        existingItem.setId("ITEM1");
        existingItem.setBrand("Brand");
        existingItem.setDescription("Description");
        existingItem.setQuantity(4D);
        existingItem.setRemarks("Remark");
        existingItem.setUnitofMeasures("PCS");
        RfqItem newItem = new RfqItem();
        newItem.setId("ITEM2");
        updated.setRfqItem(List.of(existingItem, newItem));
        RFQDocument document = new RFQDocument();
        updated.setRfqDocument(List.of(document));
        ClientDeliveryLocationRfq location = new ClientDeliveryLocationRfq();
        updated.setClientdeliverylocationrfq(List.of(location));
        RfqVendor persistedVendor = new RfqVendor();

        GmtItems gmtItem = new GmtItems();
        gmtItem.setRfqItemId("ITEM1");
        when(rfqDao.findById("EDIT")).thenReturn(Optional.of(new Rfq()));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(gmtItemsDao.findByRfqItemIdIn(anyList())).thenReturn(List.of(gmtItem));
        when(rfqVendorDao.findDataByRfqId("EDIT")).thenReturn(List.of(persistedVendor));
        GmtRfqSellerDto seller = new GmtRfqSellerDto();
        seller.setVendorUuid("ORG1");
        doReturn(List.of(seller)).when(spyService).getVendorsByGmtRfq(updated);
        doReturn(true).when(spyService).resendRfqToVendors(anyList(), same(updated));

        assertTrue(spyService.editAndResendRfq(updated));
        assertSame(updated, existingItem.getRfq());
        assertSame(updated, document.getRfq());
        assertSame(updated, location.getRfq());
        assertSame(updated, persistedVendor.getRfq());
        verify(gmtItemsDao).save(gmtItem);
        verify(spyService).resendRfqToVendors(anyList(), same(updated));

        doReturn(Collections.emptyList()).when(spyService).getVendorsByGmtRfq(updated);
        assertThrows(AppException.class, () -> spyService.editAndResendRfq(updated));

        Rfq failure = new Rfq();
        failure.setId("ERR");
        when(rfqDao.findById("ERR"))
                .thenThrow(new org.springframework.dao.ConcurrencyFailureException("db"));
        assertFalse(spyService.editAndResendRfq(failure));
    }

    @Test
    void testResendRfqToVendorsPopulatedAndSendSingleVendorOutcomes() throws Exception {
        ReflectionTestUtils.setField(service, "subjectPrefix", "PREFIX");
        ReflectionTestUtils.setField(service, "emailPassword", "secret");
        org.springframework.security.core.Authentication authentication =
                mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.userdetails.UserDetails details =
                mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(authentication.getPrincipal()).thenReturn(details);
        when(details.getUsername()).thenReturn("buyer@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try (org.mockito.MockedStatic<com.portal.procucev.utils.MailUtility> mail =
                     mockStatic(com.portal.procucev.utils.MailUtility.class)) {
            EmailUser emailUser = new EmailUser();
            emailUser.setEmail("configured@example.com");
            emailUser.setPassword("configured-password");
            when(emailUserRepo.findByEmail("buyer@example.com")).thenReturn(emailUser);
            user.setFullName("Buyer");
            when(userDao.findByUsernameAndActive("buyer@example.com", true)).thenReturn(user);
            rfq.setDeliveryDate(Date.from(java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS)));

            Organization first = mailVendor("V1", "one@example.com", "Forward");
            Organization second = mailVendor("V2", "two@example.com", "Forward");
            when(orgDao.findById("V1")).thenReturn(Optional.of(first));
            when(orgDao.findById("V2")).thenReturn(Optional.of(second));
            when(userDao.findByOrg("V1")).thenReturn(List.of("one@example.com"));
            when(userDao.findByOrg("V2")).thenReturn(Collections.emptyList());
            GmtRfqSellerDto dto1 = new GmtRfqSellerDto();
            dto1.setVendorUuid("V1");
            dto1.setOtherEmails("copy@example.com");
            GmtRfqSellerDto dto2 = new GmtRfqSellerDto();
            dto2.setVendorUuid("V2");
            assertTrue(service.resendRfqToVendors(List.of(dto1, dto2), rfq));

            MasterStatus sent = new MasterStatus();
            sent.setStatus(StatusConstants.pcRfqSent);
            MasterStatus fresh = new MasterStatus();
            fresh.setStatus(StatusConstants.vendorRfqNew);
            when(masterStatusDao.findByStatusIn(any())).thenReturn(List.of(sent, fresh));
            when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));
            RfqVendor direct = new RfqVendor();
            direct.setEmail("single@example.com");
            direct.setVendorId("V1");
            direct.setPhone("9999999999");
            String dueDate = ReflectionTestUtils.invokeMethod(service, "buildingRfqDueDate", rfq.getDeliveryDate());
            mail.when(() -> com.portal.procucev.utils.MailUtility.emailNewRfqForNoPR(
                    "PREFIX", "NewRfq", javaMailSender, rfq, "http://localhost",
                    "single@example.com", "from@test.com", null, null, dueDate, null,
                    "from@test.com", "secret", "V1", "9999999999"))
                    .thenReturn(true, false);
            assertTrue(service.sendRfqsToVendor(direct, rfq));
            assertFalse(service.sendRfqsToVendor(direct, rfq));
            assertSame(sent, direct.getProcucevStatus());
            assertSame(fresh, direct.getVendorStatus());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void testRequestRfqBySellersExistingNewNoCreditsAndFailure() {
        ReflectionTestUtils.setField(service, "subjectPrefix", "PREFIX");
        ReflectionTestUtils.setField(service, "emailPassword", "secret");
        Organization secondOrg = mailVendor("ORG2", "second@example.com", "Forward");
        GmtRfqVendors existingRequest = new GmtRfqVendors();
        existingRequest.setVendor(org);
        existingRequest.setRfq(rfq);
        Rfq secondRfq = new Rfq();
        secondRfq.setId("RFQ2-ID");
        secondRfq.setRfqId("RFQ2");
        secondRfq.setDeliveryDate(new Date());
        GmtRfqVendors newRequest = new GmtRfqVendors();
        newRequest.setVendor(secondOrg);
        newRequest.setRfq(secondRfq);
        when(orgDao.findRfqCreditsByOrg(anyString())).thenReturn(2);
        when(masterStatusDao.findByStatus(StatusConstants.vendorApproved)).thenReturn(masterStatus);
        when(gmtRfqVendorDao.findByVendorAndRfq(org, rfq)).thenReturn(new GmtRfqVendors());
        when(gmtRfqVendorDao.findByVendorAndRfq(secondOrg, secondRfq)).thenReturn(null);
        rfq.setDeliveryDate(new Date());
        when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));
        when(rfqDao.findById(secondRfq.getId())).thenReturn(Optional.empty());
        when(orgDao.findEmailById("ORG1")).thenReturn("seller@example.com");
        when(orgDao.findOtherEmailById("ORG1")).thenReturn("copy@example.com");

        try (org.mockito.MockedStatic<com.portal.procucev.utils.MailUtility> ignored =
                     mockStatic(com.portal.procucev.utils.MailUtility.class)) {
            assertTrue(service.requestRfqBySellers(List.of(existingRequest, newRequest)));
        }
        verify(gmtRfqVendorDao).updateStatus(rfq, org, masterStatus);
        verify(gmtRfqVendorDao).save(newRequest);
        verify(orgDao, times(2)).updateRfqCreditsAndUsage(anyString());

        when(orgDao.findRfqCreditsByOrg("ORG1")).thenReturn(0);
        assertThrows(AppException.class, () -> service.requestRfqBySellers(List.of(existingRequest)));
        when(orgDao.findRfqCreditsByOrg("ORG1")).thenThrow(new RuntimeException("db"));
        assertThrows(AppException.class, () -> service.requestRfqBySellers(List.of(existingRequest)));
    }

    @Test
    void testForwardRfqsToVendorSuccessfulAndSystemFailurePaths() {
        GMTServiceImpl spyService = spy(service);
        ForwardRfqVendorRequest request = new ForwardRfqVendorRequest();
        request.setSellerId("ORG1");
        request.setEmail("seller@example.com");
        request.setRfqIds(List.of("RFQ1", "RFQ2"));
        Rfq second = new Rfq();
        second.setId("RFQ2-ID");
        second.setRfqId("RFQ2");
        when(orgDao.findRfqCreditsDataByOrg("ORG1")).thenReturn(2);
        when(rfqDao.findByRfqId("RFQ1")).thenReturn(rfq);
        when(rfqDao.findByRfqId("RFQ2")).thenReturn(second);
        when(orgDao.findById("ORG1")).thenReturn(Optional.of(org));
        when(masterStatusDao.findByStatus(StatusConstants.vendorApproved)).thenReturn(masterStatus);
        when(gmtRfqVendorDao.findByVendorAndRfq(org, rfq)).thenReturn(new GmtRfqVendors());
        when(gmtRfqVendorDao.findByVendorAndRfq(org, second)).thenReturn(null);
        doReturn(true, false).when(spyService).sendRfqsToVendor(any(RfqVendor.class), any(Rfq.class));

        Map<String, Object> response = spyService.forwardRfqsToVendor(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) response.get("summary");
        assertEquals(1, summary.get("successful"));
        assertEquals(1, summary.get("failed"));
        verify(rfqDao).updateCount(rfq);
        verify(orgDao).updateRfqCreditsAndUsage("ORG1");
        verify(gmtRfqVendorDao).save(any(GmtRfqVendors.class));
    }

    @Test
    void testVerifyRecipientsSmtpSuccessRejectedAndConnectionFailureWithoutNetwork() throws Exception {
        jakarta.mail.Session session = mock(jakarta.mail.Session.class);
        when(session.getProperties()).thenReturn(new Properties());
        jakarta.mail.Transport transport = mock(jakarta.mail.Transport.class);
        when(session.getTransport("smtp")).thenReturn(transport);
        doNothing().doThrow(new jakarta.mail.SendFailedException("rejected"))
                .doThrow(new jakarta.mail.MessagingException("offline"))
                .when(transport).connect();

        try (org.mockito.MockedStatic<jakarta.mail.Session> sessions = mockStatic(jakarta.mail.Session.class)) {
            sessions.when(() -> jakarta.mail.Session.getInstance(
                    any(Properties.class), any()))
                    .thenReturn(session);
            EmailRequest request = new EmailRequest();
            request.setTo(List.of("valid@example.com"));
            request.setCc(List.of("rejected@example.com"));
            request.setBcc(List.of("offline@example.com"));
            List<String> invalid = ReflectionTestUtils.invokeMethod(service, "verifyRecipientsSMTP", request);
            assertEquals(List.of("rejected@example.com"), invalid);
            verify(transport).sendMessage(any(jakarta.mail.Message.class), any(jakarta.mail.Address[].class));
        }
    }

    @Test
    void testEmailForwarderProcessesSkipRetryAndSuccessWithoutNetwork() throws Exception {
        ReflectionTestUtils.setField(service, "subjectPrefix", "PREFIX");
        ReflectionTestUtils.setField(service, "emailPassword", "secret");
        jakarta.mail.Session session = mock(jakarta.mail.Session.class);
        jakarta.mail.Store store = mock(jakarta.mail.Store.class);
        org.eclipse.angus.mail.imap.IMAPFolder inbox = mock(org.eclipse.angus.mail.imap.IMAPFolder.class);
        jakarta.mail.Message nullSubject = mock(jakarta.mail.Message.class);
        jakarta.mail.Message malformed = mock(jakarta.mail.Message.class);
        jakarta.mail.Message noBuyer = mock(jakarta.mail.Message.class);
        jakarta.mail.Message retry = mock(jakarta.mail.Message.class);
        jakarta.mail.Message success = mock(jakarta.mail.Message.class);
        when(malformed.getSubject()).thenReturn("PREFIX You have an Enquiry RFQ No malformed");
        when(noBuyer.getSubject()).thenReturn("PREFIX You have an Enquiry RFQ No RFQ2 - ORG2");
        when(retry.getSubject()).thenReturn("PREFIX You have an Enquiry RFQ No RFQ3 - ORG3");
        when(success.getSubject()).thenReturn("PREFIX You have an Enquiry RFQ No RFQ4 - ORG4");
        when(session.getStore("imaps")).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(inbox);
        when(inbox.search(any(jakarta.mail.search.SearchTerm.class)))
                .thenReturn(new jakarta.mail.Message[]{nullSubject, malformed, noBuyer, retry, success});
        when(rfqDao.findRFQByRfQId("RFQ2")).thenReturn(Collections.emptyList());
        when(rfqDao.findRFQByRfQId("RFQ3")).thenReturn(List.of("BUYER3"));
        when(rfqDao.findRFQByRfQId("RFQ4")).thenReturn(List.of("BUYER4"));
        when(userDao.findEmailById("BUYER3")).thenReturn("buyer3@example.com");
        when(userDao.findEmailById("BUYER4")).thenReturn("buyer4@example.com");
        when(masterStatusDao.findByStatus(StatusConstants.VENDOR_QUOTE_SUBMITTED)).thenReturn(masterStatus);
        when(rfqDao.getIdbyRfqId("RFQ4")).thenReturn(List.of("RFQ4-ID"));

        try (org.mockito.MockedStatic<jakarta.mail.Session> sessions = mockStatic(jakarta.mail.Session.class);
             org.mockito.MockedStatic<com.portal.procucev.utils.MailUtility> mail =
                     mockStatic(com.portal.procucev.utils.MailUtility.class)) {
            sessions.when(() -> jakarta.mail.Session.getInstance(any(Properties.class))).thenReturn(session);
            mail.when(() -> com.portal.procucev.utils.MailUtility.forwardMessage(
                    "buyer3@example.com", javaMailSender, "from@test.com", retry, "secret"))
                    .thenReturn(false);
            mail.when(() -> com.portal.procucev.utils.MailUtility.forwardMessage(
                    "buyer4@example.com", javaMailSender, "from@test.com", success, "secret"))
                    .thenReturn(true);
            assertDoesNotThrow(() -> service.emailForwarder());
        }
        verify(rfqDao).updateRfqByRfqId("RFQ4", masterStatus);
        verify(rfqVendorDao).updateQuotationReceived("RFQ4", "ORG4", masterStatus);
        verify(gmtRfqVendorDao).updateQuotationReceived("RFQ4-ID", "ORG4", masterStatus);
        verify(orgDao).updateQuoteCount("ORG4");
        verify(malformed).setFlag(jakarta.mail.Flags.Flag.SEEN, true);
        verify(noBuyer).setFlag(jakarta.mail.Flags.Flag.SEEN, true);
        verify(retry, never()).setFlag(jakarta.mail.Flags.Flag.SEEN, true);
        verify(success).setFlag(jakarta.mail.Flags.Flag.SEEN, true);
        verify(inbox).close(false);
    }

    @Test
    void testPopulatedCsvGenerationAndDailyReportRows() {
        Date date = new Date(0);
        BuyerSellerReportDto buyer = new BuyerSellerReportDto(
                date, "Buyer", "Name", "999", "Company, Ltd", "buyer@example.com",
                "City", "GMT", "RFQ1", "Item\nSearch");
        SellerSubscriptionReportDto seller = new SellerSubscriptionReportDto(
                null, "Seller", "888", "Seller Co", "seller@example.com", "Town",
                "Yes", null, "RFQ2");
        byte[] buyerCsv = ReflectionTestUtils.invokeMethod(
                service, "generateBuyerSellerCsv", List.of(buyer));
        byte[] sellerCsv = ReflectionTestUtils.invokeMethod(
                service, "generateSellerSubscriptionCsv", List.of(seller));
        String buyerText = new String(buyerCsv, java.nio.charset.StandardCharsets.UTF_8);
        String sellerText = new String(sellerCsv, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(buyerText.contains("\"Company, Ltd\""));
        assertTrue(buyerText.contains("\"Item\nSearch\""));
        assertTrue(sellerText.contains("Yes,0,RFQ2"));

        ReflectionTestUtils.setField(service, "fromEmail", "from@example.com");
        ReflectionTestUtils.setField(service, "toEmail", "to@example.com");
        Object[] buyerRow = {date, "Buyer", "Name", "999", "Company", "buyer@example.com",
                "City", "GMT", "RFQ1", "Item"};
        Object[] sellerRowWithCount = {date, "Seller", "888", "Seller Co", "seller@example.com",
                "Town", "Yes", 3, "RFQ2"};
        Object[] sellerRowWithoutCount = {null, "Seller 2", "777", "Other", "other@example.com",
                "Village", "No", null, "RFQ3"};
        when(userDao.getDailyBuyerSellerReport(any(), any())).thenReturn(List.<Object[]>of(buyerRow));
        when(userDao.getDailySellerSubscriptionReport(any(), any()))
                .thenReturn(List.<Object[]>of(sellerRowWithCount, sellerRowWithoutCount));
        assertDoesNotThrow(() -> service.dailyReportEmailForwarder());
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }
}
