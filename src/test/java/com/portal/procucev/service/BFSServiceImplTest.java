package com.portal.procucev.service;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.*;
import com.portal.procucev.model.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BFSServiceImplTest {

    @Mock
    private OrgTypeDao orgTypeDao;
    @Mock
    private OrgDao orgDao;
    @Mock
    private MasterStatusDao masterStatusDao;
    @Mock
    private UserDao userDao;
    @Mock
    private BFSDao bfsDao;
    @Mock
    private BFSUserDao bfsUserDao;
    @Mock
    private BFSImagesDao bfsImagesDao;
    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private BFSDocumentsDao bFSDocumentsDao;
    @Mock
    private SearchRepository searchRepository;
    @Mock
    private BFSUserCommentsDao bfsUserCommentsDao;
    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private BFSServiceImpl service;

    private Organization org;
    private User user;
    private BFSItems item;
    private BFSUsers bfsUser;
    private MasterStatus masterStatus;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "host", "http://localhost");
        ReflectionTestUtils.setField(service, "mailFom", "from@test.com");
        ReflectionTestUtils.setField(service, "toAddress", "to@test.com");

        org = new Organization();
        org.setId("ORG1");
        org.setCompanyName("Company A");

        user = new User();
        user.setId("USER1");
        user.setUsername("user1@test.com");
        user.setPhone("9876543210");
        user.setOrg(org);

        masterStatus = new MasterStatus();
        masterStatus.setStatus("BFS_NEW");

        item = new BFSItems();
        item.setId("100");
        item.setBfsGroup("GRP1");
        item.setUserId("USER1");
        item.setDescription("Desc");

        bfsUser = new BFSUsers();
        bfsUser.setId("200");
        bfsUser.setUniqueId("UNIQ1");
        bfsUser.setAskPrice(100.0);
        bfsUser.setUser(user);
        bfsUser.setItems(item);
    }

    @Test
    void testOrgSearch_Success_And_NotFound() {
        when(orgTypeDao.findByTypeName("CLIENT")).thenReturn(new OrgType());
        when(orgTypeDao.findByTypeName("VENDOR")).thenReturn(new OrgType());

        when(orgDao.findOrganizationsByTypeAndNameIgnoreCase(any(), any(), any())).thenReturn(Collections.singletonList(org));
        List<Organization> res1 = service.orgSearch(org);
        assertEquals(1, res1.size());

        when(orgDao.findOrganizationsByTypeAndNameIgnoreCase(any(), any(), any())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.orgSearch(org));
    }

    @Test
    void testGetUsersByOrg_Success_And_NotFound() {
        when(userDao.findByOrganization("ORG1")).thenReturn(Collections.singletonList(user));
        List<User> res1 = service.getUsersByOrg(org);
        assertEquals(1, res1.size());

        when(userDao.findByOrganization("ORG1")).thenReturn(null);
        assertThrows(AppException.class, () -> service.getUsersByOrg(org));
    }

    @Test
    void testCreateBfs_Null_Empty_Success_Exception() {
        assertFalse(service.createBfs(null));
        assertFalse(service.createBfs(Collections.emptyList()));

        item.setBfsImages(Collections.singletonList(new BFSImages()));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        assertTrue(service.createBfs(Collections.singletonList(item)));
        verify(bfsDao).saveAll(any());

        doThrow(new RuntimeException("db err")).when(bfsDao).saveAll(any());
        assertFalse(service.createBfs(Collections.singletonList(item)));
    }

    @Test
    void testGetItemsByOrgAndUser_NullUser_And_EmptyList() {
        assertThrows(AppException.class, () -> service.getItemsByOrgAndUser(null));
        when(bfsDao.findByOrgAndUser(anyString(), anyString())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getItemsByOrgAndUser(user));
    }

    @Test
    void testGetAllItems_NullUser_And_EmptyList() {
        assertThrows(AppException.class, () -> service.getAllItems(null));
        when(bfsDao.findAllItems(anyString())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllItems(user));
    }

    @Test
    void testCreateBfsByBoq_NullItems_And_NullBoq() {
        assertThrows(AppException.class, () -> service.createBfsByBoq(null));
        BFSItems emptyBoqItem = new BFSItems();
        emptyBoqItem.setBoqfile(null);
        assertNull(service.createBfsByBoq(emptyBoqItem));
    }

    @Test
    void testGetItemsByOrgAndUser_Null_Empty_Success() {
        assertThrows(AppException.class, () -> service.getItemsByOrgAndUser(null));

        when(bfsDao.findByOrgAndUser("ORG1", "USER1")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getItemsByOrgAndUser(user));

        when(bfsDao.findByOrgAndUser("ORG1", "USER1")).thenReturn(Collections.singletonList(item));
        when(bfsImagesDao.findCountByBfs(item)).thenReturn(2L);
        List<BFSItems> res = service.getItemsByOrgAndUser(user);
        assertEquals(1, res.size());
        assertTrue(res.get(0).isImagesFlag());
    }

    @Test
    void testGetAllItems_Null_Empty_Success() {
        assertThrows(AppException.class, () -> service.getAllItems(null));

        when(bfsDao.findAllItems("USER1")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllItems(user));

        when(bfsDao.findAllItems("USER1")).thenReturn(Collections.singletonList(item));
        when(bfsImagesDao.findCountByBfs(item)).thenReturn(1L);
        List<BFSItems> res = service.getAllItems(user);
        assertEquals(1, res.size());
        assertTrue(res.get(0).isImagesFlag());
    }

    @Test
    void testCreateBfsByBoq_Null_And_ValidBoq() throws Exception {
        assertThrows(AppException.class, () -> service.createBfsByBoq(null));

        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Sheet1");
        XSSFRow row0 = sheet.createRow(0); // empty row 0
        XSSFRow row1 = sheet.createRow(1);
        String[] headers = new String[]{
                " S.No", " ItemDescription", " Specification", " Uom", " Category",
                " Quantity", " Location", " AgeOfAsset", " BuyPrice", " Discount",
                " Remarks", " BFSGroup"
        };
        for (int i = 0; i < headers.length; i++) {
            row1.createCell(i).setCellValue(headers[i]);
        }

        XSSFRow row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("1");
        row2.createCell(1).setCellValue("Desc");
        row2.createCell(2).setCellValue("Spec");
        row2.createCell(3).setCellValue("PCS");
        row2.createCell(4).setCellValue("Cat");
        row2.createCell(5).setCellValue(10.0);
        row2.createCell(6).setCellValue("Loc");
        row2.createCell(7).setCellValue("1");
        row2.createCell(8).setCellValue("100.0");
        row2.createCell(9).setCellValue(5.0);
        row2.createCell(10).setCellValue("Remarks");
        row2.createCell(11).setCellValue("GRP1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        item.setBoqfile(out.toByteArray());
        List<BFSItems> res = service.createBfsByBoq(item);
        assertNotNull(res);
        assertEquals(1, res.size());
    }

    @Test
    void testGetRequestedUserByBFS() {
        when(bfsUserDao.findByItems(any())).thenReturn(Collections.singletonList(bfsUser));
        assertDoesNotThrow(() -> service.getRequestedUserByBFS(item));
    }

    @Test
    void testGetBfsById() throws Exception {
        when(bfsDao.findById("100")).thenReturn(Optional.of(item));
        BFSItems res = service.getBfsById(item);
        assertEquals(item, res);
    }

    @Test
    void testEditBfs_And_EditBfsUser() throws Exception {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(bfsDao.findById("100")).thenReturn(Optional.of(item));
        when(bfsUserDao.findById("200")).thenReturn(Optional.of(bfsUser));

        assertDoesNotThrow(() -> service.editBfs(item));
        assertDoesNotThrow(() -> service.editBfsUser(bfsUser));
    }

    @Test
    void testEditRequestedItemBySeller() throws Exception {
        when(bfsUserDao.findById("200")).thenReturn(Optional.of(bfsUser));
        assertDoesNotThrow(() -> service.editRequestedItemBySeller(bfsUser));
    }

    @Test
    void testDeactivateCommentsFlag() throws Exception {
        when(bfsDao.findById("100")).thenReturn(Optional.of(item));
        assertDoesNotThrow(() -> service.deactivateCommentsFlag(item));
    }

    @Test
    void testProcessExcel() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        wb.createSheet("Sheet1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", out.toByteArray());
        assertDoesNotThrow(() -> service.processExcel(file));
    }

    @Test
    void testRequestBfsItem_Null_And_Success() {
        assertThrows(AppException.class, () -> service.requestBfsItem(null));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(bfsUserDao.save(any())).thenReturn(bfsUser);
        when(bfsDao.findSellerById(anyString())).thenReturn(item);
        item.setOrgId("ORG1");
        when(userDao.findByOrganization("ORG1")).thenReturn(Collections.singletonList(user));
        when(userDao.findOrgByID(anyString())).thenReturn(user);

        assertTrue(service.requestBfsItem(bfsUser));
    }

    @Test
    void testGetRequestedItems_Empty_And_Success() {
        when(masterStatusDao.findByStatusIn(any())).thenReturn(Collections.emptyList());
        when(bfsUserDao.findByStatusIn(any(), any())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getRequestedItems());

        bfsUser.setOrg(org);
        when(bfsUserDao.findByStatusIn(any(), any())).thenReturn(Collections.singletonList(bfsUser));
        when(userDao.findById(anyString())).thenReturn(Optional.of(user));

        List<?> res = service.getRequestedItems();
        assertNotNull(res);
        assertEquals(1, res.size());
    }

    @Test
    void testAcceptAndRejectBfsBySeller() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(bfsUserDao.findById("200")).thenReturn(Optional.of(bfsUser));
        when(bfsDao.findSellerById(anyString())).thenReturn(item);
        item.setOrgId("ORG1");
        item.setAvailableQuantity(100.0);
        bfsUser.setQuantity(10);
        bfsUser.setItems(item);
        when(userDao.findByOrganization("ORG1")).thenReturn(Collections.singletonList(user));
        when(userDao.findOrgByID(anyString())).thenReturn(user);
        when(userDao.findById(anyString())).thenReturn(Optional.of(user));

        bfsUser.setOrg(org);

        assertDoesNotThrow(() -> service.acceptBfsItemBySeller(bfsUser));

        BFSUsers unacceptedUser = new BFSUsers();
        unacceptedUser.setId("201");
        when(bfsUserDao.findById("201")).thenReturn(Optional.of(unacceptedUser));
        assertDoesNotThrow(() -> service.rejectBfsItemBySeller(unacceptedUser));
    }

    @Test
    void testGetRequestedItemByBuyer_And_Comments_And_CM() {
        when(bfsUserDao.findItemByUser("USER1")).thenReturn(Collections.singletonList("100"));
        when(bfsDao.findByIdIn(any())).thenReturn(Collections.singletonList(item));
        when(bfsImagesDao.findCountByBfs(any())).thenReturn(1L);

        List<BFSItems> reqItems = service.getRequestedItemByBuyer(user);
        assertEquals(1, reqItems.size());

        BFSUserComments comment = new BFSUserComments();
        comment.setItems(item);
        comment.setUser(user);
        when(bfsUserCommentsDao.save(any())).thenReturn(comment);
        assertTrue(service.createBFSCommentByBuyer(comment));

        when(bfsUserCommentsDao.findByItemId(any())).thenReturn(Collections.singletonList(comment));
        assertNotNull(service.getCommentsByItem(comment));

        when(bfsUserCommentsDao.findByItemId(any(), any())).thenReturn(Collections.singletonList(comment));
        assertNotNull(service.getCommentsByItemAndBuyer(comment));

        when(bfsUserDao.findItemsOrderedByLatestBidProjected()).thenReturn(Collections.singletonList(item));
        when(bfsUserDao.countRequestedByItemIds(any(), any())).thenReturn(Collections.emptyList());
        when(bfsImagesDao.countImagesByItemIds(any())).thenReturn(Collections.emptyList());
        assertNotNull(service.getRequestedItemsByCM());

        when(bfsUserDao.findByUniqueId("UNIQ1")).thenReturn(bfsUser);
        when(userDao.findById(anyString())).thenReturn(Optional.of(user));
        bfsUser.setOrg(org);
        bfsUser.setUser(user);
        bfsUser.setItems(item);
        assertNotNull(service.getItemByUniqueId("UNIQ1"));

        when(bfsImagesDao.findByBfs(anyString())).thenReturn(Collections.singletonList(new BFSImages()));
        assertNotNull(service.getImagesByBfs(item));

        assertNotNull(service.getuserInfoById(user));
    }

    @Test
    void listQueriesCoverNullResultsAndItemsWithoutImages() {
        when(bfsDao.findByOrgAndUser("ORG1", "USER1")).thenReturn(null);
        assertThrows(AppException.class, () -> service.getItemsByOrgAndUser(user));
        when(bfsDao.findByOrgAndUser("ORG1", "USER1")).thenReturn(List.of(item));
        when(bfsImagesDao.findCountByBfs(item)).thenReturn(0L);
        assertFalse(service.getItemsByOrgAndUser(user).get(0).isImagesFlag());

        when(bfsDao.findAllItems("USER1")).thenReturn(null);
        assertThrows(AppException.class, () -> service.getAllItems(user));
        when(bfsDao.findAllItems("USER1")).thenReturn(List.of(item));
        assertFalse(service.getAllItems(user).get(0).isImagesFlag());

        BFSItems itemWithoutImages = new BFSItems();
        itemWithoutImages.setBfsGroup("G2");
        itemWithoutImages.setUserId("USER1");
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        assertTrue(service.createBfs(List.of(itemWithoutImages)));
    }

    @Test
    void boqCoversEmptyInvalidSparseAndValidationPaths() throws Exception {
        BFSItems upload = new BFSItems();
        upload.setBoqfile(new byte[0]);
        assertNull(service.createBfsByBoq(upload));

        upload.setBoqfile(workbookBytes(workbook -> { }));
        assertThrows(AppException.class, () -> service.createBfsByBoq(upload));

        upload.setBoqfile(workbookBytes(workbook -> workbook.getSheetAt(0).createRow(0).createCell(0).setCellValue("only")));
        assertThrows(AppException.class, () -> service.createBfsByBoq(upload));

        upload.setBoqfile(workbookBytes(workbook -> {
            XSSFSheet sheet = workbook.getSheetAt(0);
            sheet.createRow(0);
            addBoqHeaders(sheet.createRow(1));
        }));
        assertThrows(AppException.class, () -> service.createBfsByBoq(upload));

        upload.setBoqfile(workbookBytes(workbook -> {
            XSSFSheet sheet = workbook.getSheetAt(0);
            addBoqHeaders(sheet.createRow(1));
            sheet.createRow(2).createCell(0).setCellValue("1");
            XSSFRow sparse = sheet.createRow(3);
            sparse.createCell(0).setCellValue("2");
            sparse.createCell(1).setCellValue("");
            sparse.createCell(2).setCellValue("");
            sparse.createCell(3).setCellValue("");
            sparse.createCell(4).setCellValue("");
            sparse.createCell(6).setCellValue("");
            sparse.createCell(10).setCellValue("");
            sparse.createCell(11).setCellValue("");
        }));
        List<BFSItems> sparseItems = service.createBfsByBoq(upload);
        assertEquals(2, sparseItems.size());
        assertNull(sparseItems.get(0).getDescription());

        upload.setBoqfile(workbookBytes(workbook -> {
            XSSFRow header = workbook.getSheetAt(0).createRow(1);
            header.createCell(0).setCellValue("wrong");
            workbook.getSheetAt(0).createRow(2).createCell(0).setCellValue("1");
        }));
        assertThrows(AppException.class, () -> service.createBfsByBoq(upload));

        @SuppressWarnings("unchecked")
        List<String> headers = mock(List.class);
        when(headers.contains(anyString())).thenReturn(true);
        when(headers.size()).thenReturn(12);
        String[] exact = {"S.No", "ItemDescription", " Specification", "Uom", "Category", "Quantity",
                "Location", " AgeOfAsset", " BuyPrice", " Discount", " Remarks", " BFSGroup"};
        for (int i = 0; i < exact.length; i++) {
            when(headers.get(i)).thenReturn(exact[i]);
        }
        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "validateExcelTemplate", headers));

        when(headers.size()).thenReturn(13);
        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(service, "validateExcelTemplate", headers));
        when(headers.size()).thenReturn(11);
        assertThrows(AppException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateExcelTemplate", headers));

        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "isRowEmpty", (Object) null));
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFRow blank = workbook.createSheet().createRow(0);
            blank.createCell(0).setCellValue("   ");
            assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "isRowEmpty", blank));
        }
    }

    @Test
    void requestedUsersCoverNullEmptyAndStatusQueries() {
        assertThrows(AppException.class, () -> service.getRequestedUserByBFS(null));
        when(bfsUserDao.findByItems(item)).thenReturn(null);
        assertThrows(AppException.class, () -> service.getRequestedUserByBFS(item));
        when(bfsUserDao.findByItems(item)).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getRequestedUserByBFS(item));

        assertThrows(AppException.class, () -> service.getRequestedUserByBFSAndStatus(null));
        when(masterStatusDao.findByStatusIn(any())).thenReturn(List.of(masterStatus));
        when(bfsUserDao.findByItemsAndStatus(eq(item), any())).thenReturn(null);
        assertThrows(AppException.class, () -> service.getRequestedUserByBFSAndStatus(item));
        when(bfsUserDao.findByItemsAndStatus(eq(item), any())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getRequestedUserByBFSAndStatus(item));
        when(bfsUserDao.findByItemsAndStatus(eq(item), any())).thenReturn(List.of(bfsUser));
        assertEquals(List.of(bfsUser), service.getRequestedUserByBFSAndStatus(item));
    }

    @Test
    void approveBfsItemCoversApprovalRejectionAndNull() {
        assertThrows(AppException.class, () -> service.approveBfsItem(null));
        when(bfsUserDao.findItemByBfsUser("200")).thenReturn("100");
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);

        bfsUser.setApproval(true);
        assertTrue(service.approveBfsItem(bfsUser));
        verify(bfsUserDao).updateStatus(bfsUser, masterStatus);

        bfsUser.setApproval(false);
        bfsUser.setCmRemarks("not approved");
        assertTrue(service.approveBfsItem(bfsUser));
        verify(bfsUserDao).updateCmRemarksAndStatus(bfsUser, masterStatus, "not approved");
        verify(bfsDao, times(2)).updateStatus("100", masterStatus);
    }

    @Test
    void approvedAndBuyerBidQueriesCoverSuccessAndEmpty() {
        prepareCompleteBid();
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(userDao.findById("USER1")).thenReturn(Optional.of(user));

        when(bfsUserDao.findByStatusAndUser(masterStatus, "USER1")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getApprovedItems(user));
        when(bfsUserDao.findByStatusAndUser(masterStatus, "USER1")).thenReturn(List.of(bfsUser));
        assertEquals(1, service.getApprovedItems(user).size());

        when(bfsUserDao.findByUserAndItems("USER1", "100")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getBidsByBuyer(bfsUser));
        when(bfsUserDao.findByUserAndItems("USER1", "100")).thenReturn(List.of(bfsUser));
        assertEquals(1, service.getBidsByBuyer(bfsUser).size());
    }

    @Test
    void sellerAcceptanceCoversMissingAndTerminalStates() {
        assertThrows(AppException.class, () -> service.acceptBfsItemBySeller(null));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(bfsUserDao.findById("200")).thenReturn(Optional.empty());
        assertTrue(service.acceptBfsItemBySeller(bfsUser));

        MasterStatus accepted = new MasterStatus();
        accepted.setStatus("BID_ACCEPTED");
        bfsUser.setStatus(accepted);
        when(bfsUserDao.findById("200")).thenReturn(Optional.of(bfsUser));
        assertThrows(AppException.class, () -> service.acceptBfsItemBySeller(bfsUser));

        MasterStatus rejected = new MasterStatus();
        rejected.setStatus("BID_NOT_ACCEPTED");
        bfsUser.setStatus(rejected);
        assertThrows(AppException.class, () -> service.acceptBfsItemBySeller(bfsUser));

        MasterStatus other = new MasterStatus();
        other.setStatus("BID_REQUESTED");
        bfsUser.setStatus(other);
        item.setAvailableQuantity(20);
        bfsUser.setQuantity(2);
        when(userDao.findByUserID(anyString())).thenReturn("person@test.com");
        assertTrue(service.acceptBfsItemBySeller(bfsUser));
        verify(bfsDao).updateAvailableQuantity(18.0, "100");
    }

    @Test
    void sellerRejectionCoversNullMissingAndTerminalStates() {
        assertThrows(AppException.class, () -> service.rejectBfsItemBySeller(null));
        when(bfsUserDao.findById("200")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.rejectBfsItemBySeller(bfsUser));

        MasterStatus status = new MasterStatus();
        status.setStatus("BID_ACCEPTED");
        bfsUser.setStatus(status);
        when(bfsUserDao.findById("200")).thenReturn(Optional.of(bfsUser));
        assertThrows(AppException.class, () -> service.rejectBfsItemBySeller(bfsUser));

        status.setStatus("BID_NOT_ACCEPTED");
        assertThrows(AppException.class, () -> service.rejectBfsItemBySeller(bfsUser));

        status.setStatus("BID_REQUESTED");
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        bfsUser.setSellerRemarks("declined");
        assertTrue(service.rejectBfsItemBySeller(bfsUser));
        verify(bfsUserDao).updateSellerRejectStatus(bfsUser, masterStatus, "declined");
    }

    @Test
    void documentsAndBfsDetailsCoverEveryOutcome() {
        assertThrows(AppException.class, () -> service.getDocumentsByBfs(null));
        when(bFSDocumentsDao.findByBfs(item)).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getDocumentsByBfs(item));
        BFSDocuments document = new BFSDocuments();
        when(bFSDocumentsDao.findByBfs(item)).thenReturn(List.of(document));
        assertEquals(List.of(document), service.getDocumentsByBfs(item));

        assertThrows(AppException.class, () -> service.getBfsById(null));
        when(bfsDao.findById("100")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.getBfsById(item));
        BFSImages image = new BFSImages();
        when(bfsDao.findById("100")).thenReturn(Optional.of(item));
        when(bFSDocumentsDao.findByBfs(item)).thenReturn(List.of(document));
        when(bfsImagesDao.findByBfs("100")).thenReturn(List.of(image));
        BFSItems result = service.getBfsById(item);
        assertEquals(List.of(document), result.getBfsDocuments());
        assertEquals(List.of(image), result.getBfsImages());
    }

    @Test
    void editBfsCoversNullReplacementAndFailure() {
        assertFalse(service.editBfs(null));
        BFSImages oldImage = new BFSImages();
        BFSDocuments oldDocument = new BFSDocuments();
        item.setBfsImages(List.of(new BFSImages()));
        item.setBfsDocuments(List.of(new BFSDocuments()));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(bfsImagesDao.findByBfs("100")).thenReturn(List.of(oldImage));
        when(bFSDocumentsDao.findByBfs(item)).thenReturn(List.of(oldDocument));
        assertTrue(service.editBfs(item));
        assertTrue(item.isImagesFlag());
        verify(bfsImagesDao).deleteAll(List.of(oldImage));
        verify(bFSDocumentsDao).deleteAll(List.of(oldDocument));

        doThrow(new RuntimeException("save failed")).when(bfsDao).save(item);
        assertFalse(service.editBfs(item));
    }

    @Test
    void editBfsUserCoversNullMissingAndFailure() {
        assertFalse(service.editBfsUser(null));
        when(bfsUserDao.findById("200")).thenReturn(Optional.empty());
        assertTrue(service.editBfsUser(bfsUser));
        when(bfsUserDao.findById("200")).thenThrow(new RuntimeException("db"));
        assertFalse(service.editBfsUser(bfsUser));
    }

    @Test
    void buyerRequestedItemsCoverBothEmptyStopsAndNoImage() {
        when(bfsUserDao.findItemByUser("USER1")).thenReturn(Collections.emptyList());
        assertTrue(service.getRequestedItemByBuyer(user).isEmpty());
        when(bfsUserDao.findItemByUser("USER1")).thenReturn(List.of("100"));
        when(bfsDao.findByIdIn(List.of("100"))).thenReturn(Collections.emptyList());
        assertTrue(service.getRequestedItemByBuyer(user).isEmpty());
        when(bfsDao.findByIdIn(List.of("100"))).thenReturn(List.of(item));
        when(bfsImagesDao.findCountByBfs(item)).thenReturn(0L);
        assertFalse(service.getRequestedItemByBuyer(user).get(0).isImagesFlag());
    }

    @Test
    void mutationHelpersCoverNullAndDaoFailures() {
        assertFalse(service.editRequestedItemBySeller(null));
        doThrow(new RuntimeException("db")).when(bfsUserDao).updateQuantity("200", 0);
        assertFalse(service.editRequestedItemBySeller(bfsUser));

        assertFalse(service.createBFSCommentByBuyer(null));
        BFSUserComments comment = new BFSUserComments();
        comment.setItems(item);
        doThrow(new RuntimeException("db")).when(bfsUserCommentsDao).save(comment);
        assertFalse(service.createBFSCommentByBuyer(comment));

        assertFalse(service.deactivateCommentsFlag(null));
        doThrow(new RuntimeException("db")).when(bfsDao).deactivateComment("100");
        assertFalse(service.deactivateCommentsFlag(item));
    }

    @Test
    void commentQueriesCoverNullAndEmptyResults() {
        BFSUserComments comment = new BFSUserComments();
        comment.setItems(item);
        comment.setUser(user);
        assertThrows(AppException.class, () -> service.getCommentsByItem(null));
        when(bfsUserCommentsDao.findByItemId(item)).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getCommentsByItem(comment));
        assertThrows(AppException.class, () -> service.getCommentsByItemAndBuyer(null));
        when(bfsUserCommentsDao.findByItemId(item, user)).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getCommentsByItemAndBuyer(comment));
    }

    @Test
    void cmRequestedItemsCoverEmptyAndPositiveBulkCounts() {
        when(bfsUserDao.findItemsOrderedByLatestBidProjected()).thenReturn(Collections.emptyList());
        assertTrue(service.getRequestedItemsByCM().isEmpty());

        BFSItems second = new BFSItems();
        second.setId("101");
        when(bfsUserDao.findItemsOrderedByLatestBidProjected()).thenReturn(List.of(item, second));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(bfsUserDao.countRequestedByItemIds(eq(masterStatus), any()))
                .thenReturn(Collections.singletonList(new Object[]{"100", 2L}));
        when(bfsImagesDao.countImagesByItemIds(any()))
                .thenReturn(Collections.singletonList(new Object[]{"100", 1L}));
        List<BFSItems> result = service.getRequestedItemsByCM();
        assertTrue(result.get(0).isRequestedFlag());
        assertTrue(result.get(0).isImagesFlag());
        assertFalse(result.get(1).isRequestedFlag());
        assertFalse(result.get(1).isImagesFlag());
    }

    @Test
    void uniqueIdImagesAndVendorInfoCoverFailuresAndAbsentUser() {
        assertThrows(AppException.class, () -> service.getItemByUniqueId(null));
        when(bfsUserDao.findByUniqueId("missing")).thenReturn(null);
        assertThrows(AppException.class, () -> service.getItemByUniqueId("missing"));

        assertThrows(AppException.class, () -> service.getImagesByBfs(null));
        when(bfsImagesDao.findByBfs("100")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getImagesByBfs(item));

        assertThrows(AppException.class, () -> service.getuserInfoById(null));
        when(userDao.findById("USER1")).thenReturn(Optional.empty());
        assertNotNull(service.getuserInfoById(user));
    }

    @Test
    void categorySearchNormalizesKeywordsAndHandlesNullLists() {
        com.portal.procucev.Dto.BFSItemDto populated = new com.portal.procucev.Dto.BFSItemDto();
        populated.setCategory(List.of("STEEL"));
        populated.setDescription(List.of("Pipe"));
        com.portal.procucev.Dto.BFSItemDto empty = new com.portal.procucev.Dto.BFSItemDto();
        when(searchRepository.searchItems(anySet(), anySet(), eq(5))).thenReturn(Collections.emptyList());

        assertTrue(service.getBfsItemsByCategory(List.of(populated, empty)).isEmpty());
        verify(searchRepository).searchItems(Set.of("steel"), Set.of("pipe"), 5);
        assertTrue(service.getBfsItemsByCategory(Collections.emptyList()).isEmpty());
    }

    @Test
    void processExcelCoversRowsNullCellsAndFailures() throws Exception {
        byte[] content = workbookBytes(workbook -> {
            XSSFSheet sheet = workbook.getSheetAt(0);
            sheet.createRow(0).createCell(0).setCellValue("header");
            XSSFRow row = sheet.createRow(1);
            row.createCell(1).setCellValue(" Description ");
            row.createCell(3).setCellValue("EA");
            row.createCell(4).setCellValue("Category");
            row.createCell(5).setCellValue(4.0);
            row.createCell(6).setCellValue("Location");
            row.createCell(7).setCellValue("new");
            row.createCell(8).setCellValue(50.0);
            row.createCell(10).setCellValue("Remarks");
            row.createCell(11).setCellValue("Group");
            row.createCell(12).setCellValue("ORG1");
            row.createCell(13).setCellValue("USER1");
            XSSFRow noOrg = sheet.createRow(3);
            noOrg.createCell(0).setCellValue("ignored");
        });
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(orgDao.findById("ORG1")).thenReturn(Optional.of(org));
        MockMultipartFile file = new MockMultipartFile("file", "items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
        service.processExcel(file);
        verify(bfsDao).saveAll(argThat(values -> {
            List<BFSItems> saved = new ArrayList<>();
            values.forEach(saved::add);
            return saved.size() == 1 && saved.get(0).getTotalQuantity() == 4.0
                    && saved.get(0).getDiscount() == 0.0 && saved.get(0).getSpecification() == null;
        }));

        when(orgDao.findById("ORG1")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.processExcel(file));
        MockMultipartFile invalid = new MockMultipartFile("file", "bad.xlsx", "application/octet-stream", new byte[]{1, 2, 3});
        assertThrows(RuntimeException.class, () -> service.processExcel(invalid));
    }

    @Test
    void requestBfsItemCoversSellerAbsent() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(bfsUserDao.save(bfsUser)).thenReturn(bfsUser);
        item.setOrgId("ORG1");
        when(bfsDao.findSellerById("100")).thenReturn(item);
        when(userDao.findByOrganization("ORG1")).thenReturn(Collections.emptyList());
        when(userDao.findOrgByID("USER1")).thenReturn(user);
        assertTrue(service.requestBfsItem(bfsUser));
    }

    @Test
    void sellerBidQueryCoversBothEmptyStagesAndSuccess() {
        prepareCompleteBid();
        when(userDao.findById("USER1")).thenReturn(Optional.of(user));
        when(bfsDao.findDistinctItemIdsBySellerId("200")).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getBidsBySeller(bfsUser));

        when(bfsDao.findDistinctItemIdsBySellerId("200")).thenReturn(List.of("100"));
        when(bfsUserDao.findLatestBidsByItemIds(List.of("100"))).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getBidsBySeller(bfsUser));
        when(bfsUserDao.findLatestBidsByItemIds(List.of("100"))).thenReturn(List.of(bfsUser));
        assertEquals(1, service.getBidsBySeller(bfsUser).size());
    }

    @Test
    void boqSkipsLeadingTitleRowBeforeTheHeaderRow() throws Exception {
        BFSItems upload = new BFSItems();
        upload.setBoqfile(workbookBytes(workbook -> {
            XSSFSheet sheet = workbook.getSheetAt(0);
            // a populated row 0 is not empty, so it reaches the row-number guard and is skipped there
            sheet.createRow(0).createCell(0).setCellValue("BFS Upload Template");
            addBoqHeaders(sheet.createRow(1));
            XSSFRow data = sheet.createRow(2);
            data.createCell(0).setCellValue("1");
            data.createCell(1).setCellValue("Desc");
        }));

        List<BFSItems> items = service.createBfsByBoq(upload);

        assertEquals(1, items.size());
        assertEquals("Desc", items.get(0).getDescription());
    }

    @Test
    void validateExcelTemplateRejectsEveryMisplacedHeaderColumn() {
        String[] exact = {"S.No", "ItemDescription", " Specification", "Uom", "Category", "Quantity",
                "Location", " AgeOfAsset", " BuyPrice", " Discount", " Remarks", " BFSGroup"};

        // Each iteration keeps columns 0..k-1 correct and corrupts column k, so the validation
        // chain short-circuits at a different link every time.
        for (int corrupted = 1; corrupted < exact.length; corrupted++) {
            @SuppressWarnings("unchecked")
            List<String> headers = mock(List.class);
            when(headers.contains(anyString())).thenReturn(true);
            when(headers.size()).thenReturn(exact.length);
            for (int i = 0; i < exact.length; i++) {
                when(headers.get(i)).thenReturn(i == corrupted ? "unexpected-column" : exact[i]);
            }

            assertEquals(Boolean.FALSE,
                    ReflectionTestUtils.invokeMethod(service, "validateExcelTemplate", headers),
                    "column " + corrupted + " should invalidate the template");
        }
    }

    private void prepareCompleteBid() {
        user.setFullName("Buyer");
        bfsUser.setOrg(org);
        bfsUser.setUser(user);
        bfsUser.setItems(item);
        bfsUser.setStatus(masterStatus);
        item.setOrg(org);
        item.setAvailableQuantity(10);
        item.setTotalQuantity(20);
        item.setSellPrice(100);
        item.setAskPrice(90);
    }

    private static byte[] workbookBytes(java.util.function.Consumer<XSSFWorkbook> writer) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("Sheet1");
            writer.accept(workbook);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void addBoqHeaders(XSSFRow row) {
        String[] headers = {" S.No", " ItemDescription", " Specification", " Uom", " Category", " Quantity",
                " Location", " AgeOfAsset", " BuyPrice", " Discount", " Remarks", " BFSGroup"};
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }
}
