package com.portal.procucev.service;

import com.portal.procucev.Dto.BuyerSellerReportDto;
import com.portal.procucev.Dto.DeliveryLocationUpdateRequest;
import com.portal.procucev.Dto.GMTRfqVendorDto;
import com.portal.procucev.Dto.GmtRfqSellerDto;
import com.portal.procucev.Dto.RfqDTO;
import com.portal.procucev.Dto.SellerSubscriptionReportDto;
import com.portal.procucev.Dto.ForwardRfqVendorRequest;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.customexception.RfqStatusResponse;
import com.portal.procucev.dao.CategoryDivisionDao;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.dao.GmtItemsDao;
import com.portal.procucev.dao.GmtRfqVendorDao;
import com.portal.procucev.dao.ItemCategoryDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgCategoryDivisionDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.RFQItemsDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.RfqVendorDao;
import com.portal.procucev.dao.RoleDao;
import com.portal.procucev.dao.SubscriptionPlanDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.CategoryDivision;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.EmailAttachment;
import com.portal.procucev.model.EmailRequest;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.GmtItems;
import com.portal.procucev.model.GmtRfqVendors;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.RFQDocument;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.RfqStatusRequest;
import com.portal.procucev.model.RfqVendor;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.MailUtility;
import com.portal.procucev.utils.StatusConstants;

import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Branch-level coverage for {@link GMTServiceImpl}.
 *
 * <p>{@code GMTServiceImplTest} already drives the happy paths. This class deliberately
 * targets the remaining decision outcomes: alternate branches of guard clauses, the
 * lambdas that only run when a collection is populated, and the failure handlers.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GMTServiceImplCoverageTest {

    private static final String S_NO = "S.No";
    private static final String ITEM_DESCRIPTION = "ItemDescription";
    private static final String SPECIFICATION = " Specification";
    private static final String UOM = "Uom";
    private static final String QUANTITY = "Quantity";
    private static final String REMARKS = "Remarks";

    @Mock
    private RfqDao rfqDao;
    @Mock
    private RFQItemsDao rfqItemsDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private SubscriptionPlanDao subscriptionPlanDao;
    @Mock
    private CategoryDivisionDao categoryDivisionDao;
    @Mock
    private MasterStatusDao masterStatusDao;
    @Mock
    private SelfRegistrationService selfRegistrationService;
    @Mock
    private GmtItemsDao gmtItemsDao;
    @Mock
    private RfqVendorDao rfqVendorDao;
    @Mock
    private GmtRfqVendorDao gmtRfqVendorDao;
    @Mock
    private ItemCategoryDao itemCategoryDao;
    @Mock
    private OrgDao orgDao;
    @Mock
    private UserDao userDao;
    @Mock
    private OrgTypeDao orgTypeDao;
    @Mock
    private OrgCategoryDivisionDao orgCategoryDivisionDao;
    @Mock
    private EmailUserRepo emailUserRepo;
    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private AutomaticRfqService automaticRfqService;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private com.portal.procucev.rfq.repository.RfqAiTokenUsageRepository rfqAiTokenUsageRepository;

    @InjectMocks
    private GMTServiceImpl service;

    private Organization org;
    private User user;
    private MasterStatus status;
    private Rfq rfq;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "host", "http://localhost");
        ReflectionTestUtils.setField(service, "mailFom", "from@test.com");
        ReflectionTestUtils.setField(service, "mail", "mail@test.com");
        ReflectionTestUtils.setField(service, "pswd", "mail-password");
        ReflectionTestUtils.setField(service, "toAddress", "to@test.com");
        ReflectionTestUtils.setField(service, "emailPassword", "secret");
        ReflectionTestUtils.setField(service, "subjectPrefix", "PREFIX");
        ReflectionTestUtils.setField(service, "fromEmail", "report-from@test.com");
        ReflectionTestUtils.setField(service, "toEmail", "report-to@test.com");

        org = organization("ORG1", "org@test.com", "9876543210");
        org.setCompanyName("Company1");

        user = new User();
        user.setId("USER1");
        user.setUsername("user1@test.com");
        user.setPhone("9876543210");
        user.setFullName("User One");
        user.setOrg(org);

        status = masterStatus("OPEN", "Open");

        rfq = new Rfq();
        rfq.setId("RFQ_UUID");
        rfq.setRfqId("RFQ1");
        rfq.setOrg(org);
        rfq.setStatus(status);
        rfq.setCategory("Category");
        rfq.setCreatedTS(new Date());
        rfq.setDeliveryDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(automaticRfqService.generateRfqId(anyString())).thenReturn("RFQ250101000001");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // editRFQForNoPrByClient
    // ------------------------------------------------------------------

    @Test
    void editRfqForNoPrByClientUpdatesLinkedCollectionsWhenNotRaisedByClient() throws Exception {
        RfqItem mappedItem = rfqItem("ITEM1", "Pump", "Category", "Division");
        RfqItem unmappedItem = rfqItem("ITEM2", "Valve", null, null);
        RFQDocument document = new RFQDocument();
        ClientDeliveryLocationRfq location = new ClientDeliveryLocationRfq();
        RfqVendor persistedVendor = new RfqVendor();

        GmtItems storedItem = new GmtItems();
        storedItem.setRfqItemId("ITEM1");

        Rfq updated = new Rfq();
        updated.setId("EDIT");
        updated.setRfqId("RFQ-EDIT");
        updated.setByClient(false);
        updated.setRfqItem(List.of(mappedItem, unmappedItem));
        updated.setRfqDocument(List.of(document));
        updated.setClientdeliverylocationrfq(List.of(location));

        when(rfqDao.findById("EDIT")).thenReturn(Optional.of(new Rfq()));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(gmtItemsDao.findByRfqItemIdIn(anyList())).thenReturn(List.of(storedItem));
        when(rfqVendorDao.findDataByRfqId("EDIT")).thenReturn(List.of(persistedVendor));

        assertTrue(service.editRFQForNoPrByClient(updated));

        // the "isByClient == false" branch must leave the client status untouched
        assertNull(updated.getClientStatus());
        // the existing GmtItems row is refreshed from the incoming RFQ item
        assertEquals("Pump", storedItem.getDescription());
        verify(gmtItemsDao).save(storedItem);
        verify(itemCategoryDao).saveAll(argThat(categories ->
                ((List<?>) categories).size() == 2));
        // every child collection lambda must have run
        assertSame(updated, mappedItem.getRfq());
        assertSame(updated, document.getRfq());
        assertSame(updated, location.getRfq());
        assertSame(updated, persistedVendor.getRfq());
    }

    @Test
    void editRfqForNoPrByClientHandlesEmptyChildCollectionsAndDataAccessFailure() throws Exception {
        Rfq empty = new Rfq();
        empty.setId("EMPTY");
        empty.setRfqId("RFQ-EMPTY");
        empty.setRfqItem(Collections.emptyList());
        empty.setRfqDocument(Collections.emptyList());
        empty.setClientdeliverylocationrfq(Collections.emptyList());

        when(rfqDao.findById("EMPTY")).thenReturn(Optional.of(new Rfq()));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(gmtItemsDao.findByRfqItemIdIn(anyList())).thenReturn(Collections.emptyList());
        when(rfqVendorDao.findDataByRfqId("EMPTY")).thenReturn(Collections.emptyList());

        assertTrue(service.editRFQForNoPrByClient(empty));
        verify(gmtItemsDao, never()).save(any(GmtItems.class));

        Rfq failing = new Rfq();
        failing.setId("BOOM");
        when(rfqDao.findById("BOOM")).thenThrow(new ConcurrencyFailureException("db down"));
        assertFalse(service.editRFQForNoPrByClient(failing));
    }

    // ------------------------------------------------------------------
    // convertRFQBoq / processBOQFiles / validateExcelTemplate / isRowEmpty
    // ------------------------------------------------------------------

    @Test
    void convertRfqBoqRejectsWorkbooksWithoutDataRows() throws Exception {
        Rfq noRows = new Rfq();
        noRows.setBoqfile(workbookBytes(sheet -> {
            // sheet intentionally left without any row
        }));
        assertThrows(AppException.class, () -> service.convertRFQBoq(noRows));

        Rfq singleRow = new Rfq();
        singleRow.setBoqfile(workbookBytes(sheet -> sheet.createRow(0).createCell(0).setCellValue("Title")));
        assertThrows(AppException.class, () -> service.convertRFQBoq(singleRow));
    }

    @Test
    void convertRfqBoqReadsRowsWithMissingAndBlankCells() throws Exception {
        Rfq request = new Rfq();
        request.setBoqfile(workbookBytes(sheet -> {
            sheet.createRow(0).createCell(0).setCellValue("Title");

            Row headerRow = sheet.createRow(1);
            String[] headers = {S_NO, ITEM_DESCRIPTION, SPECIFICATION, UOM, QUANTITY, REMARKS};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            Row complete = sheet.createRow(2);
            complete.createCell(0).setCellValue(1D);
            complete.createCell(1).setCellValue("Item One");
            complete.createCell(2).setCellValue("Spec");
            complete.createCell(3).setCellValue("PCS");
            complete.createCell(4).setCellValue(10D);
            complete.createCell(5).setCellValue("Remark");

            // leading blank cells then a numeric quantity: exercises the blank-string guards
            Row blankText = sheet.createRow(3);
            blankText.createCell(1).setCellValue("");
            blankText.createCell(2).setCellValue("");
            blankText.createCell(3).setCellValue("");
            blankText.createCell(4).setCellValue(5D);

            // only the serial number and the remarks column are present
            Row sparse = sheet.createRow(4);
            sparse.createCell(0).setCellValue(2D);
            sparse.createCell(5).setCellValue("Only remarks");

            // completely blank row is skipped
            sheet.createRow(5);
        }));

        List<RfqItem> items = service.convertRFQBoq(request);

        assertEquals(3, items.size());
        assertEquals("Item One", items.get(0).getDescription());
        assertNull(items.get(1).getDescription());
        assertEquals(5D, items.get(1).getQuantity());
        assertEquals("Only remarks", items.get(2).getRemarks());
        assertEquals(2D, items.get(2).getSerialNo());
    }

    @Test
    void validateExcelTemplateRejectsEveryOutOfOrderHeaderCombination() {
        // more than six columns short-circuits the strict ordering check
        assertFalse(validateHeaders(S_NO, ITEM_DESCRIPTION, SPECIFICATION, UOM, QUANTITY, REMARKS, "Extra"));
        // each permutation fails at a different link of the && chain
        assertFalse(validateHeaders(ITEM_DESCRIPTION, S_NO, SPECIFICATION, UOM, QUANTITY, REMARKS));
        assertFalse(validateHeaders(S_NO, SPECIFICATION, ITEM_DESCRIPTION, UOM, QUANTITY, REMARKS));
        assertFalse(validateHeaders(S_NO, ITEM_DESCRIPTION, UOM, SPECIFICATION, QUANTITY, REMARKS));
        assertFalse(validateHeaders(S_NO, ITEM_DESCRIPTION, SPECIFICATION, QUANTITY, UOM, REMARKS));
        assertFalse(validateHeaders(S_NO, ITEM_DESCRIPTION, SPECIFICATION, UOM, REMARKS, QUANTITY));
        assertFalse(validateHeaders(S_NO, ITEM_DESCRIPTION, SPECIFICATION, UOM, QUANTITY, REMARKS));
        // a missing mandatory column is reported to the caller
        assertThrows(AppException.class, () -> validateHeaders(S_NO, ITEM_DESCRIPTION));
    }

    @Test
    void isRowEmptyTreatsNullRowAsEmpty() throws Exception {
        Method isRowEmpty = GMTServiceImpl.class.getDeclaredMethod("isRowEmpty", Row.class);
        isRowEmpty.setAccessible(true);
        assertEquals(Boolean.TRUE, isRowEmpty.invoke(null, (Row) null));
    }

    // ------------------------------------------------------------------
    // queryMail
    // ------------------------------------------------------------------

    @Test
    void queryMailSkipsUserLookupWhenIdIsMissing() throws Exception {
        try (MockedStatic<MailUtility> mail = mockStatic(MailUtility.class)) {
            User anonymous = new User();
            anonymous.setUsername("anonymous@test.com");
            assertTrue(service.queryMail(anonymous));
            mail.verify(() -> MailUtility.mailingGMTClientRFQMailToinfoTeam(
                    anyString(), anyString(), any(), any(), anyString(), same(anonymous),
                    eq(null), eq(null), eq(null), eq(null)));
        }
    }

    // ------------------------------------------------------------------
    // getAllGMTRfq
    // ------------------------------------------------------------------

    @Test
    void getAllGmtRfqRejectsNullAndEmptyResults() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(rfqDao.findAllRfqNoPrInStatuses(any())).thenReturn(null);
        assertThrows(AppException.class, () -> service.getAllGMTRfq(org));

        when(rfqDao.findAllRfqNoPrInStatuses(any())).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllGMTRfq(org));
    }

    @Test
    void getAllGmtRfqFallsBackWhenCategoryLocationOrVendorDataIsMissing() {
        MasterStatus defaultStatus = masterStatus(StatusConstants.vendorRfqNew, "New");
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(masterStatusDao.findByStatus(StatusConstants.vendorRfqNew)).thenReturn(defaultStatus);

        Rfq withVendor = simpleRfq("A", "RFQ-A");
        withVendor.setClientdeliverylocationrfq(List.of(deliveryLocation("Bengaluru", "Karnataka")));
        Rfq withoutVendor = simpleRfq("B", "RFQ-B");
        withoutVendor.setClientdeliverylocationrfq(null);
        Rfq vendorWithoutStatus = simpleRfq("C", "RFQ-C");
        vendorWithoutStatus.setClientdeliverylocationrfq(Collections.emptyList());

        when(rfqDao.findAllRfqNoPrInStatuses(any()))
                .thenReturn(List.of(withVendor, withoutVendor, vendorWithoutStatus));

        GmtRfqVendors mapped = vendorMapping(withVendor, status);
        GmtRfqVendors duplicate = vendorMapping(withVendor, null);
        GmtRfqVendors withoutStatus = vendorMapping(vendorWithoutStatus, null);
        when(gmtRfqVendorDao.findByVendorAndRfqIn(eq(org), anyList()))
                .thenReturn(List.of(mapped, duplicate, withoutStatus));

        when(rfqItemsDao.findTopCategoriesByRfqIds(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{"A", "Pumps"},
                new Object[]{"B", null}));

        RfqItem item = new RfqItem();
        item.setRfq(withVendor);
        when(rfqItemsDao.findAllByRfqIds(anyList())).thenReturn(List.of(item));

        List<GMTRfqVendorDto> result = service.getAllGMTRfq(org);

        assertEquals(3, result.size());
        assertEquals("Pumps", result.get(0).getCategory());
        assertEquals("Bengaluru", result.get(0).getDeliveryLocation());
        assertSame(status, result.get(0).getStatus());
        assertNull(result.get(1).getCategory());
        assertNull(result.get(1).getDeliveryLocation());
        assertSame(defaultStatus, result.get(1).getStatus());
        assertSame(defaultStatus, result.get(2).getStatus());
    }

    // ------------------------------------------------------------------
    // approveVendor
    // ------------------------------------------------------------------

    @Test
    void approveVendorNotifiesSellerWhenRfqExists() throws Exception {
        GmtRfqVendors request = new GmtRfqVendors();
        request.setRfq(rfq);
        request.setVendor(org);

        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));
        when(orgDao.findEmailById("ORG1")).thenReturn("seller@test.com");
        when(orgDao.findOtherEmailById("ORG1")).thenReturn("copy@test.com");

        try (MockedStatic<MailUtility> mail = mockStatic(MailUtility.class)) {
            assertTrue(service.approveVendor(request));
            mail.verify(() -> MailUtility.emailNewGMTRfqForNoPR(
                    eq("NewRfq"), eq("PREFIX"), same(javaMailSender), same(rfq), eq("http://localhost"),
                    eq("seller@test.com"), eq("copy@test.com"), eq("from@test.com"), eq("secret"),
                    anyString(), eq("ORG1")));
        }
    }

    // ------------------------------------------------------------------
    // mapToDto / mapToClientDto
    // ------------------------------------------------------------------

    @Test
    void getAllRfqForCmMapsClientStatusAndToleratesMissingUserLookups() {
        when(masterStatusDao.findByStatusIn(anyList())).thenReturn(List.of(status));

        Rfq enriched = simpleRfq("R1", "RFQ-R1");
        enriched.setClientStatus(status);
        enriched.setUser("U1");
        Rfq bare = simpleRfq("R2", "RFQ-R2");
        bare.setUser("U2");

        when(rfqDao.findAllRfqNoPrByCM(anyList())).thenReturn(List.of(enriched, bare));
        when(gmtRfqVendorDao.findByVendorsByRfq(anyString())).thenReturn(1L);
        when(userDao.findByUser("U1")).thenReturn("Company One");
        when(userDao.findOrgIdByUser("U1")).thenReturn("ORG1");
        when(userDao.findPhoneByUser("U1")).thenReturn("9876543210");

        List<RfqDTO> result = service.getAllRfqForCM();

        assertEquals("OPEN", result.get(0).getClientStatusName());
        assertEquals("Company One", result.get(0).getCompanyName());
        assertNull(result.get(1).getClientStatusName());
        assertNull(result.get(1).getCompanyName());
        assertNull(result.get(1).getCompanyId());
        assertNull(result.get(1).getPhoneNumber());
    }

    @Test
    void fetchAllClientRfqsToleratesMissingUserLookups() {
        Rfq bare = simpleRfq("R2", "RFQ-R2");
        bare.setClientStatus(status);
        bare.setUser("U2");
        when(rfqDao.findAllClientRfqNoPr()).thenReturn(List.of(bare));

        List<RfqDTO> result = service.fetchAllClientGMTRfqsForCM();

        assertEquals(1, result.size());
        assertNull(result.get(0).getPhoneNumber());
        assertNull(result.get(0).getCompanyName());
        assertNull(result.get(0).getCompanyId());
    }

    @Test
    void pagedClientRfqsMapStatusAndHandleUnknownOrOrglessUsers() {
        Pageable pageable = PageRequest.of(0, 10);

        Rfq known = simpleRfq("P1", "RFQ-P1");
        known.setClientStatus(status);
        known.setUser("USER1");
        Rfq unknownUser = simpleRfq("P2", "RFQ-P2");
        unknownUser.setUser("MISSING");
        Rfq userWithoutOrg = simpleRfq("P3", "RFQ-P3");
        userWithoutOrg.setUser("NO_ORG");

        User orgless = new User();
        orgless.setId("NO_ORG");
        orgless.setPhone("1111111111");
        orgless.setOrg(null);

        when(rfqDao.findAllClientRfqNoPr(pageable))
                .thenReturn(new PageImpl<>(List.of(known, unknownUser, userWithoutOrg)));
        when(userDao.findUsersByIds(anyList())).thenReturn(List.of(user, orgless));
        when(gmtRfqVendorDao.countVendorsByRfqIds(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{"P1", 4L}));

        List<RfqDTO> result = service.fetchAllClientGMTRfqsForCM(pageable).getData();

        assertEquals(3, result.size());
        assertEquals("OPEN", result.get(0).getClientStatusName());
        assertEquals(status, result.get(0).getClientStatus());
        assertEquals("Company1", result.get(0).getCompanyName());
        assertEquals(4L, result.get(0).getNoOfVendors());
        assertNull(result.get(1).getPhoneNumber());
        assertEquals(0L, result.get(1).getNoOfVendors());
        assertEquals("1111111111", result.get(2).getPhoneNumber());
        assertNull(result.get(2).getCompanyName());
    }

    @Test
    void clientRfqSearchResolvesMatchesByCompanyNameAndContactNumber() {
        Rfq matched = simpleRfq("S1", "RFQ-S1");
        matched.setUser("USER1");
        when(userDao.findUsersByOrgCompanyName("Company1")).thenReturn(List.of(user));
        when(userDao.findUsersByPhone("9876543210")).thenReturn(List.of(user));
        when(rfqDao.findAllClientRfqByUserIds(anyList())).thenReturn(List.of(matched));
        when(userDao.findUsersByIds(anyList())).thenReturn(List.of(user));
        when(gmtRfqVendorDao.countVendorsByRfqIds(anyList())).thenReturn(Collections.emptyList());

        assertEquals(1, service.fetchAllClientGMTRfqsForCMSearch("companyname", "Company1").size());
        assertEquals(1, service.fetchAllClientGMTRfqsForCMSearch("contactnumber", "9876543210").size());
    }

    // ------------------------------------------------------------------
    // getVendorsbyRFQ / master data lookups
    // ------------------------------------------------------------------

    @Test
    void getVendorsByRfqCopiesOtherEmailsWhenPresent() {
        org.setOtherEmails("copy@test.com");
        RfqVendor stored = new RfqVendor();
        stored.setId("RV1");
        stored.setOrganization(org);
        rfq.setRfqVendor(List.of(stored));

        when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));
        when(userDao.findByOrg(org)).thenReturn(List.of(user));

        assertEquals("copy@test.com", service.getVendorsbyRFQ(rfq).get(0).getOtherEmails());
    }

    @Test
    void masterDataLookupsRejectEmptyNonNullCollections() {
        when(categoryDivisionDao.getCategoryByDivision(anyString())).thenReturn(Collections.emptyList());
        CategoryDivision division = new CategoryDivision();
        division.setDivision("Division");
        assertThrows(AppException.class, () -> service.getCategoryByDivision(division));

        when(categoryDivisionDao.getAllCategory()).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllCategory());

        when(categoryDivisionDao.getAllDivision()).thenReturn(Collections.emptyList());
        assertThrows(AppException.class, () -> service.getAllDivision());
    }

    @Test
    void getRfqsForNoPrRejectsAnonymousPrincipal() {
        authenticate(null);
        assertThrows(AppException.class, () -> service.getRFQsForNoPR());
    }

    // ------------------------------------------------------------------
    // createRFQWithNoPr / saveVendorsForRfq
    // ------------------------------------------------------------------

    @Test
    void createRfqWithNoPrRegistersNewAndExistingVendors() throws Exception {
        GMTServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).sendRfqToVendors(anyList(), any(Rfq.class));

        Organization newVendor = organization(null, "new@test.com", "9000000001");
        newVendor.setCompanyName("Brand New");
        newVendor.setRequestType("Forward");
        Organization existingWithEmails = organization("EX1", "ex1@test.com", "9000000002");
        existingWithEmails.setOtherEmails("copy@test.com");
        Organization existingWithoutEmails = organization("EX2", "ex2@test.com", "9000000003");

        Rfq request = new Rfq();
        request.setId("NEW_RFQ");
        request.setCategory("Category");
        request.setVendors(List.of(newVendor, existingWithEmails, existingWithoutEmails));

        Rfq saved = simpleRfq("SAVED", "RFQ-SAVED");
        saved.setCategory("Category");

        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(new OrgType());
        when(selfRegistrationService.checkOrgexist("Brand New")).thenReturn(false);
        when(rfqDao.save(request)).thenReturn(saved);
        when(orgDao.save(newVendor)).thenAnswer(invocation -> {
            newVendor.setId("NEW");
            return newVendor;
        });
        when(orgDao.findById(anyString())).thenAnswer(invocation -> Optional.of(
                switch (invocation.<String>getArgument(0)) {
                    case "EX1" -> existingWithEmails;
                    case "EX2" -> existingWithoutEmails;
                    default -> newVendor;
                }));
        // only EX2 has already been mapped to this RFQ
        when(gmtRfqVendorDao.findByVendorAndRfq(any(Organization.class), same(saved)))
                .thenAnswer(invocation -> "EX2".equals(invocation.<Organization>getArgument(0).getId())
                        ? new GmtRfqVendors()
                        : null);

        assertTrue(spyService.createRFQWithNoPr(request));

        verify(orgDao).save(newVendor);
        verify(orgDao).updateOtherEmail("copy@test.com", "EX1");
        verify(orgDao, never()).updateOtherEmail(any(), eq("EX2"));
        verify(gmtRfqVendorDao, times(2)).save(any(GmtRfqVendors.class));
        verify(rfqDao, times(3)).updateCount(saved);
        verify(orgDao, times(3)).updateRfqCreditsAndUsage(anyString());
        verify(spyService).sendRfqToVendors(argThat(vendors -> vendors.size() == 3), same(saved));
    }

    @Test
    void createRfqWithNoPrFailsWhenVendorCompanyAlreadyRegistered() {
        Organization duplicate = organization(null, "dup@test.com", "9000000004");
        duplicate.setCompanyName("Duplicate Co");

        Rfq request = new Rfq();
        request.setId("DUP_RFQ");
        request.setVendors(List.of(duplicate));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(new OrgType());
        when(selfRegistrationService.checkOrgexist("Duplicate Co")).thenReturn(true);
        when(rfqDao.save(request)).thenReturn(simpleRfq("SAVED", "RFQ-SAVED"));

        assertFalse(service.createRFQWithNoPr(request));
    }

    @Test
    void createRfqWithNoPrStillSucceedsWhenNotificationMailFails() throws Exception {
        GMTServiceImpl spyService = spy(service);
        doThrow(new MessagingException("mail server down"))
                .when(spyService).sendRfqToVendors(anyList(), any(Rfq.class));

        Rfq request = new Rfq();
        request.setId("NO_VENDORS");
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(rfqDao.save(request)).thenReturn(simpleRfq("SAVED", "RFQ-SAVED"));

        assertTrue(spyService.createRFQWithNoPr(request));
    }

    // ------------------------------------------------------------------
    // saveVendorsForForwardRfq
    // ------------------------------------------------------------------

    @Test
    void saveVendorsForForwardRfqCoversForwardStatusAndMissingOtherEmails() throws Exception {
        GMTServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).sendRfqToVendors(anyList(), any(Rfq.class));

        Organization forwardVendor = organization("EX1", "ex1@test.com", "9000000005");
        forwardVendor.setRequestType("Forward");

        Rfq request = new Rfq();
        request.setVendors(List.of(forwardVendor));

        when(orgDao.findById("EX1")).thenReturn(Optional.of(forwardVendor));
        when(gmtRfqVendorDao.findByVendorAndRfq(any(Organization.class), any(Rfq.class))).thenReturn(null);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);

        assertTrue(ReflectionTestUtils.<Boolean>invokeMethod(
                spyService, "saveVendorsForForwardRfq", request, rfq));
        verify(gmtRfqVendorDao).save(any(GmtRfqVendors.class));
        verify(orgDao, never()).updateOtherEmail(any(), anyString());

        doThrow(new MessagingException("mail server down"))
                .when(spyService).sendRfqToVendors(anyList(), any(Rfq.class));
        assertTrue(ReflectionTestUtils.<Boolean>invokeMethod(
                spyService, "saveVendorsForForwardRfq", request, rfq));
    }

    @Test
    void saveVendorsForForwardRfqRejectsAlreadyRegisteredCompany() {
        Organization duplicate = organization(null, "dup@test.com", "9000000006");
        duplicate.setCompanyName("Duplicate Co");

        Rfq request = new Rfq();
        request.setVendors(List.of(duplicate));

        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(new OrgType());
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(selfRegistrationService.checkOrgexist("Duplicate Co")).thenReturn(true);

        assertThrows(AppException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "saveVendorsForForwardRfq", request, rfq));
    }

    // ------------------------------------------------------------------
    // setUserDetails
    // ------------------------------------------------------------------

    @Test
    void setUserDetailsAcceptsAlreadyPrefixedMobileNumbers() {
        Organization vendor = organization("V1", "vendor@test.com", "+919876543210");
        vendor.setName("Vendor Name");

        when(userDao.findByUsernameAndPhoneAndActive("vendor@test.com", "+919876543210", true)).thenReturn(null);
        when(masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED)).thenReturn(status);
        when(roleDao.findByRoleNameAndActive(StatusConstants.VENDOR, true)).thenReturn(new Role());
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.setUserDetails(vendor, new User());

        assertEquals("vendor@test.com", created.getUsername());
        assertEquals("+919876543210", created.getPhone());
        assertEquals(StatusConstants.EMAIL_VERIFIED, created.getVerificationStatus());
    }

    @Test
    void setUserDetailsFailsWhenReferenceDataOrPersistenceIsUnavailable() {
        Organization vendor = organization("V1", "vendor@test.com", "9876543210");
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);

        when(masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED)).thenReturn(null);
        assertThrows(AppException.class, () -> service.setUserDetails(vendor, new User()));

        when(masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED)).thenReturn(status);
        when(roleDao.findByRoleNameAndActive(StatusConstants.VENDOR, true)).thenReturn(null);
        assertThrows(AppException.class, () -> service.setUserDetails(vendor, new User()));

        when(roleDao.findByRoleNameAndActive(StatusConstants.VENDOR, true)).thenReturn(new Role());
        when(userDao.save(any(User.class))).thenThrow(new ConcurrencyFailureException("db down"));
        assertThrows(AppException.class, () -> service.setUserDetails(vendor, new User()));
    }

    // ------------------------------------------------------------------
    // sendRfqToVendors
    // ------------------------------------------------------------------

    @Test
    void sendRfqToVendorsHandlesPrefixedPhoneMailFailureAndPersistenceFailure() throws Exception {
        authenticate("buyer@test.com");

        MasterStatus sent = masterStatus(StatusConstants.pcRfqSent, "Sent");
        MasterStatus fresh = masterStatus(StatusConstants.vendorRfqNew, "New");
        when(masterStatusDao.findByStatusIn(anyList())).thenReturn(List.of(sent, fresh));

        EmailUser emailUser = new EmailUser();
        emailUser.setEmail("configured@test.com");
        emailUser.setPassword("configured-password");
        when(emailUserRepo.findByEmail("buyer@test.com")).thenReturn(emailUser);
        when(userDao.findByUsernameAndActive("buyer@test.com", true)).thenReturn(user);

        Organization prefixedVendor = organization("V+91", "plus@test.com", "+919876543210");
        prefixedVendor.setRequestType("Forward");
        rfq.setVendors(List.of(prefixedVendor));

        User vendorUser = new User();
        vendorUser.setCreatedTS(Date.from(Instant.now().minus(2, ChronoUnit.DAYS)));
        when(userDao.findByUsernameAndPhoneAndActive("plus@test.com", "+919876543210", true))
                .thenReturn(vendorUser);

        RfqVendor persisted = new RfqVendor();
        persisted.setRfq(rfq);
        when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));

        try (MockedStatic<MailUtility> mail = mockStatic(MailUtility.class)) {
            mail.when(() -> MailUtility.emailNewRfqForNoPRForExistingUsers(
                            any(), any(), any(), any(), any(), any(), any(), any(),
                            any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new MessagingException("mail server down"));

            // the per-vendor MessagingException is swallowed, the overall call still succeeds
            assertTrue(service.sendRfqToVendors(List.of(persisted), rfq));

            doThrow(new ConcurrencyFailureException("db down")).when(rfqVendorDao).saveAll(anyList());
            assertFalse(service.sendRfqToVendors(List.of(persisted), rfq));
        }
    }

    // ------------------------------------------------------------------
    // forwardRfqForNoPr
    // ------------------------------------------------------------------

    @Test
    void forwardRfqForNoPrIgnoresMissingIdentifierAndUnknownRfq() {
        assertTrue(service.forwardRfqForNoPr(new Rfq()));

        Rfq unknown = new Rfq();
        unknown.setId("UNKNOWN");
        when(rfqDao.findById("UNKNOWN")).thenReturn(Optional.empty());
        assertTrue(service.forwardRfqForNoPr(unknown));
    }

    @Test
    void forwardRfqForNoPrForwardsToStoredRfq() throws Exception {
        GMTServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).sendRfqToVendors(anyList(), any(Rfq.class));

        Organization vendor = organization("EX1", "ex1@test.com", "9000000007");
        vendor.setRequestType(ApplicationConstants.Invite);
        Rfq incoming = new Rfq();
        incoming.setId("RFQ_UUID");
        incoming.setVendors(List.of(vendor));

        when(rfqDao.findById("RFQ_UUID")).thenReturn(Optional.of(rfq));
        when(orgDao.findById("EX1")).thenReturn(Optional.of(vendor));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(gmtRfqVendorDao.findByVendorAndRfq(any(Organization.class), any(Rfq.class))).thenReturn(null);

        assertTrue(spyService.forwardRfqForNoPr(incoming));
        assertEquals(List.of(vendor), rfq.getVendors());
    }

    @Test
    void forwardRfqForNoPrWrapsUnexpectedFailures() {
        Rfq broken = new Rfq();
        broken.setId("BROKEN");
        when(rfqDao.findById("BROKEN")).thenThrow(new ConcurrencyFailureException("db down"));
        assertThrows(AppException.class, () -> service.forwardRfqForNoPr(broken));
    }

    // ------------------------------------------------------------------
    // client administration
    // ------------------------------------------------------------------

    @Test
    void acceptSelfClientSkipsOrganizationUpdateWhenUserHasNoOrganization() throws Exception {
        User orgless = new User();
        orgless.setId("USER1");
        orgless.setUsername("orgless@test.com");

        when(userDao.findById("USER1")).thenReturn(Optional.of(orgless));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);

        try (MockedStatic<MailUtility> mail = mockStatic(MailUtility.class)) {
            assertTrue(service.acceptSelfClient(orgless));
            verify(orgDao, never()).updateClientStatus(any(), anyString());
            mail.verify(() -> MailUtility.mailingVerificationLinkWithSelfUserLogin(
                    same(javaMailSender), eq("mail@test.com"), any(), eq("mail-password"),
                    eq("http://localhost"), same(orgless)));
        }
    }

    @Test
    void ignoreClientSkipsStatusUpdateWhenUserIsUnknown() {
        when(userDao.findById("USER1")).thenReturn(Optional.empty());
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);

        assertTrue(service.ignoreClient(user));
        verify(userDao, never()).updateClientStatus(any(), any());
    }

    @Test
    void disableUserPromotesRemainingOrganizationEmail() {
        when(userDao.findById("USER1")).thenReturn(Optional.of(user));
        when(userDao.findOrgIdByUser("USER1")).thenReturn("ORG1");
        when(userDao.findByOrg("ORG1")).thenReturn(List.of("next@test.com", "other@test.com"));

        assertTrue(service.disableUser(user));
        verify(userDao).deactiveUser("USER1");
        verify(orgDao).updateEmailByOrg("ORG1", "next@test.com");
    }

    // ------------------------------------------------------------------
    // editAndResendRfq
    // ------------------------------------------------------------------

    @Test
    void editAndResendRfqHandlesEmptyChildCollectionsAndNullVendorList() throws Exception {
        GMTServiceImpl spyService = spy(service);

        Rfq updated = new Rfq();
        updated.setId("EDIT");
        updated.setRfqId("RFQ-EDIT");
        updated.setRfqItem(Collections.emptyList());
        updated.setRfqDocument(Collections.emptyList());
        updated.setClientdeliverylocationrfq(Collections.emptyList());

        when(rfqDao.findById("EDIT")).thenReturn(Optional.of(new Rfq()));
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(gmtItemsDao.findByRfqItemIdIn(anyList())).thenReturn(Collections.emptyList());
        when(rfqVendorDao.findDataByRfqId("EDIT")).thenReturn(Collections.emptyList());

        GmtRfqSellerDto seller = new GmtRfqSellerDto();
        seller.setVendorUuid("ORG1");
        doReturn(List.of(seller)).when(spyService).getVendorsByGmtRfq(updated);
        doReturn(true).when(spyService).resendRfqToVendors(anyList(), same(updated));

        assertTrue(spyService.editAndResendRfq(updated));
        verify(gmtItemsDao, never()).save(any(GmtItems.class));

        // a null vendor list is reported to the caller as "no vendors found"
        doReturn(null).when(spyService).getVendorsByGmtRfq(updated);
        assertThrows(AppException.class, () -> spyService.editAndResendRfq(updated));
    }

    // ------------------------------------------------------------------
    // getGmtBuyers
    // ------------------------------------------------------------------

    @Test
    void getGmtBuyersReturnsEmptyListWhenNoUsersMatchRole() {
        Role role = new Role();
        when(roleDao.findByRoleNameAndActive(ApplicationConstants.ClientInitiator, true)).thenReturn(role);
        when(userDao.getUsersBySelfClientAndRole(role)).thenReturn(Collections.emptyList());

        assertTrue(service.getGmtBuyers().isEmpty());
    }

    // ------------------------------------------------------------------
    // createRFQByClient
    // ------------------------------------------------------------------

    @Test
    void createRfqByClientJoinsRemarksAndBrandForEveryItem() throws Exception {
        Rfq request = new Rfq();
        request.setId("CLIENT_RFQ");
        RfqItem both = rfqItem("I1", "Pump", "Category", "Division");
        both.setRemarks("Urgent");
        both.setBrand("ACME");
        RfqItem brandOnly = rfqItem("I2", "Valve", "Category", "Division");
        brandOnly.setBrand("ACME");
        RfqItem neither = rfqItem("I3", "Pipe", "Category", "Division");
        request.setRfqItem(List.of(both, brandOnly, neither));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(rfqDao.save(request)).thenReturn(simpleRfq("SAVED", "RFQ-SAVED"));

        Map<String, Object> result = service.createRFQByClient(request);

        assertNotNull(result.get("rfqId"));
        assertEquals("SAVED", result.get("rfquuid"));
        assertEquals("Urgent ACME", both.getBrand());
        assertEquals("ACME", brandOnly.getBrand());
        assertEquals("", neither.getBrand());
        verify(gmtItemsDao).saveAll(anyList());
    }

    @Test
    void createRfqByClientReturnsNullWhenPersistenceFails() throws Exception {
        Rfq request = new Rfq();
        request.setId("CLIENT_RFQ");
        request.setRfqItem(List.of(rfqItem("I1", "Pump", "Category", "Division")));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        when(gmtItemsDao.saveAll(anyList())).thenThrow(new ConcurrencyFailureException("db down"));

        assertNull(service.createRFQByClient(request));
    }

    // ------------------------------------------------------------------
    // sendEmail
    // ------------------------------------------------------------------

    @Test
    void sendEmailSupportsMissingRecipientListsAndEmptyAttachments() {
        EmailRequest simple = new EmailRequest();
        simple.setSubject("Subject");
        simple.setBody("Body");
        simple.setAttachments(Collections.emptyList());

        MessageResponse simpleResponse = service.sendEmail(simple);
        assertEquals("200", simpleResponse.getStatusCode());

        EmailAttachment attachment = new EmailAttachment();
        attachment.setFileName("report.csv");
        attachment.setContentType("text/csv");
        attachment.setFileData(Base64.getEncoder().encodeToString("a,b".getBytes()));

        EmailRequest withAttachment = new EmailRequest();
        withAttachment.setSubject("Subject");
        withAttachment.setBody("Body");
        withAttachment.setAttachments(List.of(attachment));

        assertEquals("200", service.sendEmail(withAttachment).getStatusCode());

        attachment.setFileData(null);
        assertEquals("500", service.sendEmail(withAttachment).getStatusCode());
    }

    // ------------------------------------------------------------------
    // RFQ status APIs
    // ------------------------------------------------------------------

    @Test
    void getRfqStatusesNormalisesNullIdsAndReportsNonProgressStatuses() {
        RfqStatusRequest request = new RfqStatusRequest();
        request.setClientId("USER1");
        request.setRfqIds(Arrays.asList(null, "RFQ1"));

        MasterStatus closed = masterStatus("CLOSED", "Closed");
        rfq.setStatus(closed);
        rfq.setQuotationReceived(false);
        when(rfqDao.findByUserAndRfqIdIn(eq("USER1"), anyList())).thenReturn(List.of(rfq));

        List<RfqStatusResponse> responses = service.getRfqStatuses(request);
        assertEquals(2, responses.size());
        assertEquals("RFQnull", responses.get(0).getRfqid());
        assertEquals("Invalid RFQID", responses.get(0).getStatus());
        assertEquals("Closed", responses.get(1).getStatus());

        request.setRfqIds(null);
        when(rfqDao.findLast3ByClientId(eq("USER1"), any(PageRequest.class))).thenReturn(List.of(rfq));
        assertEquals("Closed", service.getRfqStatuses(request).get(0).getStatus());
    }

    @Test
    void getRfqSellerStatusesCoversNullIdsAndLatestRfqFallback() {
        RfqStatusRequest request = new RfqStatusRequest();
        request.setClientId("USER1");
        request.setRfqIds(Arrays.asList(null, "RFQ1"));

        when(rfqDao.findRfqsByIdsExcludingSellerRequested(anyList(), eq("USER1"))).thenReturn(List.of(rfq));
        List<RfqStatusResponse> responses = service.getRfqSellerStatuses(request);
        assertEquals("RFQnull", responses.get(0).getRfqid());
        assertEquals("Open", responses.get(1).getStatus());

        Rfq withoutStatus = simpleRfq("NS", "RFQ-NS");
        withoutStatus.setStatus(null);
        request.setRfqIds(null);
        when(rfqDao.findLatest5RfqsExcludingSellerRequested(eq("USER1"), any(Pageable.class)))
                .thenReturn(List.of(rfq, withoutStatus));

        List<RfqStatusResponse> latest = service.getRfqSellerStatuses(request);
        assertEquals("Open", latest.get(0).getStatus());
        assertEquals("Unknown", latest.get(1).getStatus());
    }

    @Test
    void getSellerRfqStatusDataCoversNullIdListAndUnquotedLatestRfqs() {
        RfqStatusRequest request = new RfqStatusRequest();
        request.setClientId("ORG1");
        request.setRfqIds(null);

        GmtRfqVendors downloaded = vendorMapping(rfq, status);
        downloaded.setQuotationReceived(false);
        when(gmtRfqVendorDao.findLatest5ByVendorUuid(eq("ORG1"), any(Pageable.class)))
                .thenReturn(List.of(downloaded));

        assertEquals("Downloaded", service.getSellerRfqStatusData(request).get(0).getStatus());
    }

    // ------------------------------------------------------------------
    // forwardRfqsToVendor / sendRfqsToVendor
    // ------------------------------------------------------------------

    @Test
    void forwardRfqsToVendorRecordsSystemErrorsPerRfq() {
        ForwardRfqVendorRequest request = new ForwardRfqVendorRequest();
        request.setSellerId("ORG1");
        request.setEmail("seller@test.com");
        request.setRfqIds(List.of("RFQ1"));

        when(orgDao.findRfqCreditsDataByOrg("ORG1")).thenReturn(5);
        when(rfqDao.findByRfqId("RFQ1")).thenReturn(rfq);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(status);
        // an absent organisation makes the mandatory Optional#get blow up
        when(orgDao.findById("ORG1")).thenReturn(Optional.empty());

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) service.forwardRfqsToVendor(request).get("summary");
        assertEquals(1, summary.get("failed"));
        assertEquals(0, summary.get("successful"));
    }

    @Test
    void sendRfqsToVendorReturnsFalseWhenPersistenceFails() {
        MasterStatus sent = masterStatus(StatusConstants.pcRfqSent, "Sent");
        when(masterStatusDao.findByStatusIn(anyList())).thenReturn(List.of(sent));
        when(rfqDao.findById(rfq.getId())).thenReturn(Optional.of(rfq));
        when(rfqVendorDao.save(any(RfqVendor.class))).thenThrow(new ConcurrencyFailureException("db down"));

        RfqVendor vendor = new RfqVendor();
        vendor.setEmail("seller@test.com");
        assertFalse(service.sendRfqsToVendor(vendor, rfq));
    }

    // ------------------------------------------------------------------
    // getOrgByUserId
    // ------------------------------------------------------------------

    @Test
    void getOrgByUserIdRejectsUnknownUserAndUserWithoutOrganization() {
        User request = new User();
        request.setId("USER1");

        when(userDao.findById("USER1")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.getOrgByUserId(request));

        User orgless = new User();
        orgless.setId("USER1");
        when(userDao.findById("USER1")).thenReturn(Optional.of(orgless));
        assertThrows(AppException.class, () -> service.getOrgByUserId(request));
    }

    // ------------------------------------------------------------------
    // getLastOpenRfqsForVendor
    // ------------------------------------------------------------------

    @Test
    void getLastOpenRfqsForVendorOmitsLocationAndDeadlineWhenUnset() {
        Rfq bare = simpleRfq("OPEN1", "RFQ-OPEN1");
        bare.setCreatedTS(new Date());
        bare.setClientdeliverylocationrfq(Collections.emptyList());
        bare.setRfqClosingDate(null);

        GmtRfqVendors mapping = vendorMapping(bare, status);
        mapping.setRequestedDate(new Date());
        when(gmtRfqVendorDao.findLastOpenRfqsByVendor(eq("ORG1"), any(Pageable.class)))
                .thenReturn(List.of(mapping));

        Map<String, Object> row = service.getLastOpenRfqsForVendor("ORG1").get(0);
        assertFalse(row.containsKey("location"));
        assertFalse(row.containsKey("days_remaining"));
        assertEquals("open_for_bidding", row.get("status"));
    }

    // ------------------------------------------------------------------
    // emailForwarder
    // ------------------------------------------------------------------

    @Test
    void emailForwarderSkipsUnparseableSubjectsAndMissingBuyers() throws Exception {
        Message noRfqId = mock(Message.class);
        Message noBuyerList = mock(Message.class);
        Message forwarded = mock(Message.class);
        when(noRfqId.getSubject()).thenReturn("PREFIX You have an Enquiry");
        when(noBuyerList.getSubject()).thenReturn("PREFIX You have an Enquiry RFQ No RFQ7 - ORG7");
        when(forwarded.getSubject()).thenReturn("PREFIX You have an Enquiry RFQ No RFQ8 - ORG8");

        jakarta.mail.Session session = mock(jakarta.mail.Session.class);
        jakarta.mail.Store store = mock(jakarta.mail.Store.class);
        IMAPFolder inbox = mock(IMAPFolder.class);
        when(session.getStore("imaps")).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(inbox);
        when(inbox.search(any(SearchTerm.class)))
                .thenReturn(new Message[]{noRfqId, noBuyerList, forwarded});

        when(rfqDao.findRFQByRfQId("RFQ7")).thenReturn(null);
        when(rfqDao.findRFQByRfQId("RFQ8")).thenReturn(List.of("BUYER8"));
        when(userDao.findEmailById("BUYER8")).thenReturn("buyer8@test.com");
        when(masterStatusDao.findByStatus(StatusConstants.VENDOR_QUOTE_SUBMITTED)).thenReturn(status);
        when(rfqDao.getIdbyRfqId("RFQ8")).thenReturn(Collections.emptyList());

        try (MockedStatic<jakarta.mail.Session> sessions = mockStatic(jakarta.mail.Session.class);
             MockedStatic<MailUtility> mail = mockStatic(MailUtility.class)) {
            sessions.when(() -> jakarta.mail.Session.getInstance(any(java.util.Properties.class)))
                    .thenReturn(session);
            mail.when(() -> MailUtility.forwardMessage(
                            eq("buyer8@test.com"), same(javaMailSender), eq("from@test.com"),
                            same(forwarded), eq("secret")))
                    .thenReturn(true);

            assertDoesNotThrow(() -> service.emailForwarder());
        }

        verify(noRfqId).setFlag(Flags.Flag.SEEN, true);
        verify(noBuyerList).setFlag(Flags.Flag.SEEN, true);
        verify(gmtRfqVendorDao, never()).updateQuotationReceived(anyString(), anyString(), any());
        verify(orgDao).updateQuoteCount("ORG8");
    }

    // ------------------------------------------------------------------
    // updateVendorClasses
    // ------------------------------------------------------------------

    @Test
    void updateVendorClassesUsesExistingUsageCountsAndSurvivesUpdateFailures() {
        OrgType vendorType = new OrgType();
        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(vendorType);

        Organization promoted = new Organization();
        promoted.setId("PROMOTED");
        promoted.setRfqUsedCount(9L);
        promoted.setQuoteSubmitted(4L);
        promoted.setVendorClass(StatusConstants.Gold);

        Organization failing = new Organization();
        failing.setId("FAILING");
        failing.setRfqUsedCount(2L);
        failing.setQuoteSubmitted(1L);

        when(orgDao.findByOrgType(vendorType)).thenReturn(List.of(promoted, failing));
        doThrow(new ConcurrencyFailureException("db down"))
                .when(orgDao).updateVendorClass("FAILING", StatusConstants.Gold);

        assertDoesNotThrow(() -> service.updateVendorClasses());
        verify(orgDao).updateVendorClass("PROMOTED", StatusConstants.Diamond);
    }

    // ------------------------------------------------------------------
    // buyer lookups
    // ------------------------------------------------------------------

    @Test
    void getBuyerDataByRfqRejectsBlankUserIdAndReportsUnknownUser() {
        Rfq blankUser = simpleRfq("B1", "RFQ-B1");
        blankUser.setUser("   ");
        when(rfqDao.findById("B1")).thenReturn(Optional.of(blankUser));
        assertNull(service.getBuyerDataByRFQ(blankUser));

        Rfq unknownUser = simpleRfq("B2", "RFQ-B2");
        unknownUser.setUser("GHOST");
        when(rfqDao.findById("B2")).thenReturn(Optional.of(unknownUser));
        when(userDao.findUserById("GHOST")).thenReturn(null);
        assertNull(service.getBuyerDataByRFQ(unknownUser));
    }

    @Test
    void getBuyerByRfqRejectsMissingIdAndReportsNoActiveUser() {
        assertNull(service.getBuyerByRFQ(new Rfq()));

        Rfq lookup = simpleRfq("B3", "RFQ-B3");
        when(rfqDao.findClientById("B3")).thenReturn("ORG1");
        when(userDao.findUserByOrgId("ORG1")).thenReturn(null);
        assertNull(service.getBuyerByRFQ(lookup));
    }

    // ------------------------------------------------------------------
    // daily report
    // ------------------------------------------------------------------

    @Test
    void dailyReportHandlesMissingLoginTimestampsAndQueryFailures() {
        BuyerSellerReportDto withoutTimestamp = new BuyerSellerReportDto(
                null, "Buyer", "Name", "9876543210", "Company", "buyer@test.com",
                "City", "GMT", "RFQ1", "Item");
        byte[] csv = ReflectionTestUtils.invokeMethod(
                service, "generateBuyerSellerCsv", List.of(withoutTimestamp));
        assertNotNull(csv);
        assertTrue(new String(csv, java.nio.charset.StandardCharsets.UTF_8).contains("1,,Buyer"));

        SellerSubscriptionReportDto seller = new SellerSubscriptionReportDto(
                new Date(0), "Seller", "888", "Seller Co", "seller@test.com", "Town",
                "Yes", 2L, "RFQ2");
        assertNotNull(ReflectionTestUtils.invokeMethod(
                service, "generateSellerSubscriptionCsv", List.of(seller)));

        when(userDao.getDailyBuyerSellerReport(any(Date.class), any(Date.class)))
                .thenThrow(new ConcurrencyFailureException("db down"));
        assertDoesNotThrow(() -> service.dailyReportEmailForwarder());
    }

    // ------------------------------------------------------------------
    // resendRfqToVendors
    // ------------------------------------------------------------------

    @Test
    void resendRfqToVendorsReturnsFalseWhenSellerOrganizationIsMissing() throws Exception {
        authenticate("buyer@test.com");
        when(emailUserRepo.findByEmail("buyer@test.com")).thenReturn(null);
        when(userDao.findByUsernameAndActive("buyer@test.com", true)).thenReturn(null);
        when(orgDao.findById("GHOST")).thenReturn(Optional.empty());

        GmtRfqSellerDto seller = new GmtRfqSellerDto();
        seller.setVendorUuid("GHOST");

        assertFalse(service.resendRfqToVendors(List.of(seller), rfq));
    }

    // ------------------------------------------------------------------
    // OTP
    // ------------------------------------------------------------------

    @Test
    void generateOtpStoresOneOtpPerOrganization() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        try (MockedStatic<MailUtility> mail = mockStatic(MailUtility.class)) {
            assertTrue(service.generateOtp(org, request));
        }

        @SuppressWarnings("unchecked")
        Map<String, String> otpMap = (Map<String, String>) ReflectionTestUtils.getField(service, "otpMap");
        assertNotNull(otpMap);
        assertEquals(6, otpMap.get("ORG1").length());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void authenticate(String username) {
        Authentication authentication = mock(Authentication.class);
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean validateHeaders(String... headers) {
        Boolean valid = ReflectionTestUtils.invokeMethod(
                service, "validateExcelTemplate", new ArrayList<>(List.of(headers)));
        return Boolean.TRUE.equals(valid);
    }

    private byte[] workbookBytes(java.util.function.Consumer<Sheet> populator) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            populator.accept(workbook.createSheet("BOQ"));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private Organization organization(String id, String email, String phone) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setEmail(email);
        organization.setOrganizationPhonenumber(phone);
        return organization;
    }

    private MasterStatus masterStatus(String value, String uiDisplay) {
        MasterStatus masterStatus = new MasterStatus();
        masterStatus.setStatus(value);
        masterStatus.setUiDisplay(uiDisplay);
        return masterStatus;
    }

    private Rfq simpleRfq(String id, String rfqId) {
        Rfq value = new Rfq();
        value.setId(id);
        value.setRfqId(rfqId);
        value.setStatus(status);
        return value;
    }

    private RfqItem rfqItem(String id, String description, String category, String division) {
        RfqItem item = new RfqItem();
        item.setId(id);
        item.setDescription(description);
        item.setCategory(category);
        item.setDivision(division);
        item.setQuantity(3D);
        item.setUnitofMeasures("PCS");
        return item;
    }

    private ClientDeliveryLocationRfq deliveryLocation(String city, String state) {
        ClientDeliveryLocationRfq location = new ClientDeliveryLocationRfq();
        location.setCity(city);
        location.setState(state);
        return location;
    }

    private GmtRfqVendors vendorMapping(Rfq target, MasterStatus mappingStatus) {
        GmtRfqVendors mapping = new GmtRfqVendors();
        mapping.setRfq(target);
        mapping.setVendor(org);
        mapping.setStatus(mappingStatus);
        mapping.setRequestedDate(new Date());
        return mapping;
    }

    @Test
    void testUpdateDeliveryLocation_NullRequest_And_MissingId() {
        MessageResponse resp1 = service.updateDeliveryLocation(null);
        assertEquals("400", resp1.getStatusCode());

        DeliveryLocationUpdateRequest emptyIdReq = new DeliveryLocationUpdateRequest();
        emptyIdReq.setId("  ");
        emptyIdReq.setRfqId("");
        MessageResponse resp2 = service.updateDeliveryLocation(emptyIdReq);
        assertEquals("400", resp2.getStatusCode());

        DeliveryLocationUpdateRequest nullBothReq = new DeliveryLocationUpdateRequest();
        nullBothReq.setId(null);
        nullBothReq.setRfqId(null);
        MessageResponse resp3 = service.updateDeliveryLocation(nullBothReq);
        assertEquals("400", resp3.getStatusCode());
    }

    @Test
    void testUpdateDeliveryLocation_AuthorizationChecks() {
        authenticate("unauthorizedUser");
        User user = new User();
        user.setUsername("unauthorizedUser");
        Role role = new Role();
        role.setRoleName("ClientInitiator");
        user.setRole(role);
        when(userDao.findByUsernameAndActive("unauthorizedUser", true)).thenReturn(user);

        DeliveryLocationUpdateRequest req = new DeliveryLocationUpdateRequest();
        req.setId("RFQ-123");
        req.setCity("Raigarh");
        MessageResponse resp = service.updateDeliveryLocation(req);
        assertEquals("403", resp.getStatusCode());

        // Test Admin role authorized
        role.setRoleName("Admin");
        Rfq dummyRfq = new Rfq();
        dummyRfq.setId("RFQ-123");
        dummyRfq.setClientdeliverylocationrfq(new ArrayList<>());
        when(rfqDao.findById("RFQ-123")).thenReturn(Optional.of(dummyRfq));
        MessageResponse respAdmin = service.updateDeliveryLocation(req);
        assertEquals("200", respAdmin.getStatusCode());

        // Test ROLE_ADMIN role authorized
        role.setRoleName("ROLE_ADMIN");
        MessageResponse respRoleAdmin = service.updateDeliveryLocation(req);
        assertEquals("200", respRoleAdmin.getStatusCode());

        // Test ADMIN uppercase role authorized
        role.setRoleName("ADMIN");
        MessageResponse respUpperAdmin = service.updateDeliveryLocation(req);
        assertEquals("200", respUpperAdmin.getStatusCode());

        // Test null role / null user
        when(userDao.findByUsernameAndActive("unauthorizedUser", true)).thenReturn(null);
        MessageResponse respNullUser = service.updateDeliveryLocation(req);
        assertEquals("200", respNullUser.getStatusCode());

        User userNoRole = new User();
        userNoRole.setUsername("unauthorizedUser");
        userNoRole.setRole(null);
        when(userDao.findByUsernameAndActive("unauthorizedUser", true)).thenReturn(userNoRole);
        MessageResponse respNoRole = service.updateDeliveryLocation(req);
        assertEquals("200", respNoRole.getStatusCode());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateDeliveryLocation_ValidationErrors() {
        authenticate("cmUser");
        User cmUser = new User();
        cmUser.setUsername("cmUser");
        Role role = new Role();
        role.setRoleName("CategoryManager");
        cmUser.setRole(role);
        when(userDao.findByUsernameAndActive("cmUser", true)).thenReturn(cmUser);

        Rfq rfqNoCity = new Rfq();
        rfqNoCity.setId("RFQ-123");
        rfqNoCity.setClientdeliverylocationrfq(new ArrayList<>());
        when(rfqDao.findById("RFQ-123")).thenReturn(Optional.of(rfqNoCity));

        DeliveryLocationUpdateRequest missingCityReq = new DeliveryLocationUpdateRequest();
        missingCityReq.setId("RFQ-123");
        missingCityReq.setCity(":null");
        MessageResponse resp1 = service.updateDeliveryLocation(missingCityReq);
        assertEquals("400", resp1.getStatusCode());
        assertEquals("City is required for delivery location", resp1.getMessage());

        DeliveryLocationUpdateRequest invalidPinReq = new DeliveryLocationUpdateRequest();
        invalidPinReq.setId("RFQ-123");
        invalidPinReq.setCity("Raigarh");
        invalidPinReq.setPincode("INV@LID*PINCODE!!!");
        MessageResponse resp2 = service.updateDeliveryLocation(invalidPinReq);
        assertEquals("400", resp2.getStatusCode());
        assertEquals("Invalid Pincode/Zipcode format", resp2.getMessage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateDeliveryLocation_RfqNotFound() {
        DeliveryLocationUpdateRequest req = new DeliveryLocationUpdateRequest();
        req.setId("NON_EXISTENT");
        req.setCity("Raigarh");
        when(rfqDao.findById("NON_EXISTENT")).thenReturn(Optional.empty());
        when(rfqDao.findByRfqId("NON_EXISTENT")).thenReturn(null);

        MessageResponse resp = service.updateDeliveryLocation(req);
        assertEquals("404", resp.getStatusCode());
    }

    @Test
    void testUpdateDeliveryLocation_Success_ExistingLocation() {
        authenticate("cmUser");
        User cmUser = new User();
        cmUser.setUsername("cmUser");
        Role role = new Role();
        role.setRoleName("CategoryManager2");
        cmUser.setRole(role);
        when(userDao.findByUsernameAndActive("cmUser", true)).thenReturn(cmUser);

        Rfq existing = new Rfq();
        existing.setId("RFQ-UUID-1");
        existing.setRfqId("RFQ-BUS-1");
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("OldCity");
        loc.setState(null);
        loc.setPincode(null);
        existing.setClientdeliverylocationrfq(new ArrayList<>(List.of(loc)));

        when(rfqDao.findById("RFQ-UUID-1")).thenReturn(Optional.of(existing));

        Date newDate = new Date();
        DeliveryLocationUpdateRequest req = new DeliveryLocationUpdateRequest();
        req.setId("RFQ-UUID-1");
        req.setCity("Raigarh");
        req.setState("Chhattisgarh");
        req.setPincode("496001");
        req.setAddress("Industrial Area");
        req.setDeliveryDate(newDate);

        MessageResponse resp = service.updateDeliveryLocation(req);
        assertEquals("200", resp.getStatusCode());
        assertEquals("Raigarh", loc.getCity());
        assertEquals("Chhattisgarh", loc.getState());
        assertEquals("496001", loc.getPincode());
        assertEquals("Industrial Area", loc.getAddress());
        assertEquals(newDate, existing.getDeliveryDate());
        verify(rfqDao).save(existing);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateDeliveryLocation_Success_NewLocation_And_FindByRfqBusinessId() {
        Rfq existing = new Rfq();
        existing.setId("RFQ-UUID-2");
        existing.setRfqId("RFQ-BUS-2");
        existing.setClientdeliverylocationrfq(null);

        when(rfqDao.findById("RFQ-BUS-2")).thenReturn(Optional.empty());
        when(rfqDao.findByRfqId("RFQ-BUS-2")).thenReturn(existing);

        DeliveryLocationUpdateRequest req = new DeliveryLocationUpdateRequest();
        req.setRfqId("RFQ-BUS-2");
        req.setCity("Mumbai");
        req.setState(":null");
        req.setPincode("null");
        req.setAddress(": null");

        MessageResponse resp = service.updateDeliveryLocation(req);
        assertEquals("200", resp.getStatusCode());
        assertNotNull(existing.getClientdeliverylocationrfq());
        assertEquals(1, existing.getClientdeliverylocationrfq().size());
        ClientDeliveryLocationRfq createdLoc = existing.getClientdeliverylocationrfq().get(0);
        assertEquals("Mumbai", createdLoc.getCity());
        assertNull(createdLoc.getState());
        assertNull(createdLoc.getPincode());
        assertNull(createdLoc.getAddress());
        verify(rfqDao).save(existing);
    }

    @Test
    void testUpdateDeliveryLocation_PartialUpdate_PreservesCity() {
        authenticate("cmUser");
        User cmUser = new User();
        cmUser.setUsername("cmUser");
        Role role = new Role();
        role.setRoleName("CategoryManagerBasic");
        cmUser.setRole(role);
        when(userDao.findByUsernameAndActive("cmUser", true)).thenReturn(cmUser);

        Rfq existing = new Rfq();
        existing.setId("RFQ-PARTIAL-1");
        existing.setRfqId("RFQ-PARTIAL-BUS");
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("Pune");
        loc.setState("Maharashtra");
        loc.setPincode("411001");
        existing.setClientdeliverylocationrfq(new ArrayList<>(List.of(loc)));

        when(rfqDao.findById("RFQ-PARTIAL-1")).thenReturn(Optional.of(existing));

        Date updatedDate = new Date();
        DeliveryLocationUpdateRequest req = new DeliveryLocationUpdateRequest();
        req.setId("RFQ-PARTIAL-1");
        req.setState("Karnataka");
        req.setPincode("560001");
        req.setDeliveryDate(updatedDate);

        MessageResponse resp = service.updateDeliveryLocation(req);
        assertEquals("200", resp.getStatusCode());
        assertEquals("Pune", loc.getCity());
        assertEquals("Karnataka", loc.getState());
        assertEquals("560001", loc.getPincode());
        assertEquals(updatedDate, existing.getDeliveryDate());
        verify(rfqDao).save(existing);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchRfqById_SanitizesColonNull() {
        Rfq existing = new Rfq();
        existing.setId("SANITIZE-TEST");
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("Raigarh");
        loc.setState(":null");
        loc.setPincode("null");
        loc.setAddress("   ");
        existing.setClientdeliverylocationrfq(new ArrayList<>(List.of(loc)));

        when(rfqDao.findById("SANITIZE-TEST")).thenReturn(Optional.of(existing));

        Rfq query = new Rfq();
        query.setId("SANITIZE-TEST");
        org.springframework.http.ResponseEntity<?> resp = service.fetchRfqById(query);
        assertNotNull(resp);
        Rfq fetched = (Rfq) resp.getBody();
        assertNotNull(fetched);
        assertEquals("Raigarh", fetched.getClientdeliverylocationrfq().get(0).getCity());
        assertNull(fetched.getClientdeliverylocationrfq().get(0).getState());
        assertNull(fetched.getClientdeliverylocationrfq().get(0).getPincode());
        assertNull(fetched.getClientdeliverylocationrfq().get(0).getAddress());
    }

    @Test
    void testFetchRfqById_EmailSourceType_AttachesAiTokenUsageForCategoryManager() {
        Rfq existing = new Rfq();
        existing.setId("RFQ-EMAIL-123");
        existing.setRfqId("RFQ260109648263");
        existing.setSourceType("EMAIL");
        existing.setCreatedTS(new Date());

        when(rfqDao.findById("RFQ-EMAIL-123")).thenReturn(Optional.of(existing));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("catmgr@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Role role = new Role();
        role.setRoleName(StatusConstants.CATEGORYMANAGER_ROLE_NAME);
        User cmUser = new User();
        cmUser.setUsername("catmgr@test.com");
        cmUser.setRole(role);
        when(userDao.findByUsernameAndActive("catmgr@test.com", true)).thenReturn(cmUser);

        com.portal.procucev.rfq.entity.RfqAiTokenUsage tokenUsage = com.portal.procucev.rfq.entity.RfqAiTokenUsage.builder()
                .rfqNumber("RFQ260109648263")
                .messageId("msg-123")
                .modelName("gemini-2.5-flash")
                .promptTokens(1420)
                .candidateTokens(450)
                .totalTokens(1870)
                .attemptsCount(1)
                .estimatedCostUsd(0.000241)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        when(rfqAiTokenUsageRepository.findByRfqNumber("RFQ260109648263")).thenReturn(Optional.of(tokenUsage));

        Rfq query = new Rfq();
        query.setId("RFQ-EMAIL-123");
        org.springframework.http.ResponseEntity<?> resp = service.fetchRfqById(query);

        assertNotNull(resp);
        Rfq result = (Rfq) resp.getBody();
        assertNotNull(result);
        assertNotNull(result.getAiTokenUsage());
        assertEquals("RFQ260109648263", result.getAiTokenUsage().getRfqNumber());
        assertEquals(1870, result.getAiTokenUsage().getTotalTokens());
        assertEquals("gemini-2.5-flash", result.getAiTokenUsage().getModelName());
        assertEquals("$0.0002", result.getAiTokenUsage().getFormattedCost());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchRfqById_PortalOrWhatsApp_DoesNotAttachAiTokenUsage() {
        Rfq existing = new Rfq();
        existing.setId("RFQ-PORTAL-123");
        existing.setRfqId("RFQ260109648999");
        existing.setSourceType("PORTAL");

        when(rfqDao.findById("RFQ-PORTAL-123")).thenReturn(Optional.of(existing));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("catmgr@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Role role = new Role();
        role.setRoleName(StatusConstants.CATEGORYMANAGER_ROLE_NAME);
        User cmUser = new User();
        cmUser.setUsername("catmgr@test.com");
        cmUser.setRole(role);
        when(userDao.findByUsernameAndActive("catmgr@test.com", true)).thenReturn(cmUser);

        Rfq query = new Rfq();
        query.setId("RFQ-PORTAL-123");
        org.springframework.http.ResponseEntity<?> resp = service.fetchRfqById(query);

        assertNotNull(resp);
        Rfq result = (Rfq) resp.getBody();
        assertNotNull(result);
        assertNull(result.getAiTokenUsage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetRfqAiTokenConsumption_SuccessForEmailRfq() {
        Rfq existing = new Rfq();
        existing.setId("UUID-1");
        existing.setRfqId("RFQ-EMAIL-1");
        existing.setSourceType("EMAIL");

        when(rfqDao.findById("UUID-1")).thenReturn(Optional.of(existing));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("catmgr@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Role role = new Role();
        role.setRoleName("Admin");
        User adminUser = new User();
        adminUser.setUsername("catmgr@test.com");
        adminUser.setRole(role);
        when(userDao.findByUsernameAndActive("catmgr@test.com", true)).thenReturn(adminUser);

        Rfq req = new Rfq();
        req.setId("UUID-1");
        ResponseEntity<?> resp = service.getRfqAiTokenConsumption(req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        com.portal.procucev.Dto.RfqAiTokenUsageDTO dto = (com.portal.procucev.Dto.RfqAiTokenUsageDTO) resp.getBody();
        assertEquals("RFQ-EMAIL-1", dto.getRfqNumber());
        assertEquals("EMAIL", dto.getSourceType());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetRfqAiTokenConsumption_FindByRfqIdWhenIdIsBlank() {
        Rfq existing = new Rfq();
        existing.setId("UUID-SEARCH-1");
        existing.setRfqId("RFQ-BUSINESS-1");
        existing.setSourceType("EMAIL");

        when(rfqDao.findByRfqId("RFQ-BUSINESS-1")).thenReturn(existing);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("cm@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Role role = new Role();
        role.setRoleName(StatusConstants.categorymanager2);
        User cmUser = new User();
        cmUser.setUsername("cm@test.com");
        cmUser.setRole(role);
        when(userDao.findByUsernameAndActive("cm@test.com", true)).thenReturn(cmUser);

        Rfq req = new Rfq();
        req.setId("   ");
        req.setRfqId("RFQ-BUSINESS-1");
        ResponseEntity<?> resp = service.getRfqAiTokenConsumption(req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetRfqAiTokenConsumption_ValidationFailures() {
        // null rfq
        ResponseEntity<?> r1 = service.getRfqAiTokenConsumption(null);
        assertEquals(HttpStatus.BAD_REQUEST, r1.getStatusCode());

        // empty id and empty rfqId
        Rfq emptyReq = new Rfq();
        emptyReq.setId("  ");
        emptyReq.setRfqId("");
        ResponseEntity<?> r2 = service.getRfqAiTokenConsumption(emptyReq);
        assertEquals(HttpStatus.BAD_REQUEST, r2.getStatusCode());

        // null id and null rfqId
        Rfq nullReq = new Rfq();
        ResponseEntity<?> r3 = service.getRfqAiTokenConsumption(nullReq);
        assertEquals(HttpStatus.BAD_REQUEST, r3.getStatusCode());
    }

    @Test
    void testGetRfqAiTokenConsumption_RfqNotFound() {
        when(rfqDao.findById("NON_EXISTENT")).thenReturn(Optional.empty());
        when(rfqDao.findByRfqId("NON_EXISTENT")).thenReturn(null);

        Rfq req = new Rfq();
        req.setId("NON_EXISTENT");
        ResponseEntity<?> resp = service.getRfqAiTokenConsumption(req);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void testGetRfqAiTokenConsumption_RejectedForNonEmailRfq() {
        Rfq existing = new Rfq();
        existing.setId("UUID-2");
        existing.setRfqId("RFQ-WHATSAPP-1");
        existing.setSourceType("WhatsApp");

        when(rfqDao.findById("UUID-2")).thenReturn(Optional.of(existing));

        Rfq req = new Rfq();
        req.setId("UUID-2");
        ResponseEntity<?> resp = service.getRfqAiTokenConsumption(req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void testGetRfqAiTokenConsumption_ForbiddenForUnauthorizedRole() {
        Rfq existing = new Rfq();
        existing.setId("UUID-3");
        existing.setRfqId("RFQ-EMAIL-3");
        existing.setSourceType("EMAIL");

        when(rfqDao.findById("UUID-3")).thenReturn(Optional.of(existing));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("vendor@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Role role = new Role();
        role.setRoleName("ROLE_VENDOR");
        User vendorUser = new User();
        vendorUser.setUsername("vendor@test.com");
        vendorUser.setRole(role);
        when(userDao.findByUsernameAndActive("vendor@test.com", true)).thenReturn(vendorUser);

        Rfq req = new Rfq();
        req.setId("UUID-3");
        ResponseEntity<?> resp = service.getRfqAiTokenConsumption(req);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsAuthorizedCategoryManager_AllRoleBranches() {
        // 1. null authentication
        SecurityContextHolder.getContext().setAuthentication(null);
        Boolean res1 = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertFalse(Boolean.TRUE.equals(res1));

        // 2. principal not UserDetails
        Authentication stringAuth = mock(Authentication.class);
        when(stringAuth.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(stringAuth);
        Boolean res2 = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertFalse(Boolean.TRUE.equals(res2));

        // 3. UserDetails with null username
        UserDetails nullUsernameUser = mock(UserDetails.class);
        when(nullUsernameUser.getUsername()).thenReturn(null);
        Authentication nullUserAuth = mock(Authentication.class);
        when(nullUserAuth.getPrincipal()).thenReturn(nullUsernameUser);
        SecurityContextHolder.getContext().setAuthentication(nullUserAuth);
        Boolean res3 = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertFalse(Boolean.TRUE.equals(res3));

        // 4. UserDao returns null
        authenticate("unknown@test.com");
        when(userDao.findByUsernameAndActive("unknown@test.com", true)).thenReturn(null);
        Boolean res4 = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertFalse(Boolean.TRUE.equals(res4));

        // 5. User has null role
        User userNoRole = new User();
        userNoRole.setUsername("norole@test.com");
        userNoRole.setRole(null);
        authenticate("norole@test.com");
        when(userDao.findByUsernameAndActive("norole@test.com", true)).thenReturn(userNoRole);
        Boolean res5 = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertFalse(Boolean.TRUE.equals(res5));

        // 6. Test each authorized role branch
        String[] authorizedRoles = {
                StatusConstants.CATEGORYMANAGER_ROLE_NAME,
                StatusConstants.categorymanager2,
                StatusConstants.CATEGORY_MANAGER_BASIC,
                "Admin",
                "ROLE_ADMIN",
                "ADMIN"
        };
        for (String roleName : authorizedRoles) {
            Role role = new Role();
            role.setRoleName(roleName);
            User user = new User();
            user.setUsername("testuser@test.com");
            user.setRole(role);
            authenticate("testuser@test.com");
            when(userDao.findByUsernameAndActive("testuser@test.com", true)).thenReturn(user);
            Boolean resRole = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
            assertTrue(Boolean.TRUE.equals(resRole), "Role should be authorized: " + roleName);
        }

        // 7. Non-matching role
        Role otherRole = new Role();
        otherRole.setRoleName("ROLE_BUYER");
        User buyerUser = new User();
        buyerUser.setUsername("buyer@test.com");
        buyerUser.setRole(otherRole);
        authenticate("buyer@test.com");
        when(userDao.findByUsernameAndActive("buyer@test.com", true)).thenReturn(buyerUser);
        Boolean resOther = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertFalse(Boolean.TRUE.equals(resOther));

        // 8. Exception branch
        authenticate("error@test.com");
        when(userDao.findByUsernameAndActive("error@test.com", true)).thenThrow(new RuntimeException("Simulated error"));
        Boolean resErr = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertFalse(Boolean.TRUE.equals(resErr));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testBuildAiTokenUsageDto_AllBranches() {
        // 1. null rfq
        com.portal.procucev.Dto.RfqAiTokenUsageDTO dtoNull = ReflectionTestUtils.invokeMethod(service, "buildAiTokenUsageDto", (Rfq) null);
        assertNull(dtoNull);

        // 2. rfq with blank rfqId fallback to id, usage found with null estimatedCostUsd
        Rfq rfqIdOnly = new Rfq();
        rfqIdOnly.setId("UUID-FALLBACK-1");
        rfqIdOnly.setRfqId("   ");
        rfqIdOnly.setSourceType("EMAIL");

        com.portal.procucev.rfq.entity.RfqAiTokenUsage usageWithNullCost = com.portal.procucev.rfq.entity.RfqAiTokenUsage.builder()
                .rfqNumber("UUID-FALLBACK-1")
                .promptTokens(100)
                .candidateTokens(50)
                .totalTokens(150)
                .estimatedCostUsd(null)
                .build();
        when(rfqAiTokenUsageRepository.findByRfqNumber("UUID-FALLBACK-1")).thenReturn(Optional.of(usageWithNullCost));

        com.portal.procucev.Dto.RfqAiTokenUsageDTO dto2 = ReflectionTestUtils.invokeMethod(service, "buildAiTokenUsageDto", rfqIdOnly);
        assertNotNull(dto2);
        assertEquals(0.0, dto2.getEstimatedCostUsd());
        assertEquals("$0.0000", dto2.getFormattedCost());

        // 3. rfq where primary rfqNumber lookup is empty, but secondary rfq.getId() lookup finds token usage
        Rfq rfqSecondary = new Rfq();
        rfqSecondary.setId("UUID-SEC-1");
        rfqSecondary.setRfqId("RFQ2601090001");
        rfqSecondary.setSourceType("EMAIL");

        when(rfqAiTokenUsageRepository.findByRfqNumber("RFQ2601090001")).thenReturn(Optional.empty());
        when(rfqAiTokenUsageRepository.findByRfqNumber("UUID-SEC-1")).thenReturn(Optional.of(usageWithNullCost));

        com.portal.procucev.Dto.RfqAiTokenUsageDTO dto3 = ReflectionTestUtils.invokeMethod(service, "buildAiTokenUsageDto", rfqSecondary);
        assertNotNull(dto3);

        // 4. Historical fallback with null rfqItem and null createdTS
        Rfq rfqHist1 = new Rfq();
        rfqHist1.setRfqId("RFQ-HIST-1");
        rfqHist1.setSourceType("EMAIL");
        rfqHist1.setRfqItem(null);
        rfqHist1.setCreatedTS(null);

        when(rfqAiTokenUsageRepository.findByRfqNumber("RFQ-HIST-1")).thenReturn(Optional.empty());

        com.portal.procucev.Dto.RfqAiTokenUsageDTO dtoHist1 = ReflectionTestUtils.invokeMethod(service, "buildAiTokenUsageDto", rfqHist1);
        assertNotNull(dtoHist1);
        assertEquals(1430, dtoHist1.getPromptTokens()); // 1250 + 1*180
        assertEquals(475, dtoHist1.getCandidateTokens()); // 380 + 1*95
        assertEquals(1905, dtoHist1.getTotalTokens());
        assertNotNull(dtoHist1.getCreatedAt());

        // 5. Historical fallback with multiple rfqItems and populated createdTS
        Rfq rfqHist2 = new Rfq();
        rfqHist2.setRfqId("RFQ-HIST-2");
        rfqHist2.setSourceType("EMAIL");
        rfqHist2.setRfqItem(List.of(new RfqItem(), new RfqItem()));
        rfqHist2.setCreatedTS(new Date());

        when(rfqAiTokenUsageRepository.findByRfqNumber("RFQ-HIST-2")).thenReturn(Optional.empty());

        com.portal.procucev.Dto.RfqAiTokenUsageDTO dtoHist2 = ReflectionTestUtils.invokeMethod(service, "buildAiTokenUsageDto", rfqHist2);
        assertNotNull(dtoHist2);
        assertEquals(1610, dtoHist2.getPromptTokens()); // 1250 + 2*180
        assertEquals(570, dtoHist2.getCandidateTokens()); // 380 + 2*95
        assertEquals(2180, dtoHist2.getTotalTokens());
    }

    @Test
    void testFetchRfqById_EmailSourceType_UnAuthorizedUserDoesNotAttachAiTokenUsage() {
        Rfq existing = new Rfq();
        existing.setId("RFQ-EMAIL-UNAUTH");
        existing.setRfqId("RFQ260109648999");
        existing.setSourceType("EMAIL");

        when(rfqDao.findById("RFQ-EMAIL-UNAUTH")).thenReturn(Optional.of(existing));

        authenticate("buyer@test.com");
        Role role = new Role();
        role.setRoleName("ROLE_BUYER");
        User buyer = new User();
        buyer.setUsername("buyer@test.com");
        buyer.setRole(role);
        when(userDao.findByUsernameAndActive("buyer@test.com", true)).thenReturn(buyer);

        Rfq query = new Rfq();
        query.setId("RFQ-EMAIL-UNAUTH");
        org.springframework.http.ResponseEntity<?> resp = service.fetchRfqById(query);

        assertNotNull(resp);
        Rfq result = (Rfq) resp.getBody();
        assertNotNull(result);
        assertNull(result.getAiTokenUsage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchRfqById_EmailSourceType_AuthorizedCategoryManagerAttachesAiTokenUsage() {
        Rfq existing = new Rfq();
        existing.setId("RFQ-EMAIL-AUTH");
        existing.setRfqId("RFQ260109648998");
        existing.setSourceType("EMAIL");

        when(rfqDao.findById("RFQ-EMAIL-AUTH")).thenReturn(Optional.of(existing));

        authenticate("catmgr@test.com");
        Role role = new Role();
        role.setRoleName(StatusConstants.CATEGORYMANAGER_ROLE_NAME);
        User catMgr = new User();
        catMgr.setUsername("catmgr@test.com");
        catMgr.setRole(role);
        when(userDao.findByUsernameAndActive("catmgr@test.com", true)).thenReturn(catMgr);

        Rfq query = new Rfq();
        query.setId("RFQ-EMAIL-AUTH");
        org.springframework.http.ResponseEntity<?> resp = service.fetchRfqById(query);

        assertNotNull(resp);
        Rfq result = (Rfq) resp.getBody();
        assertNotNull(result);
        assertNotNull(result.getAiTokenUsage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetRfqAiTokenConsumption_SuccessForShortEmailSourceTypes() {
        Rfq existingE = new Rfq();
        existingE.setId("UUID-E");
        existingE.setRfqId("RFQ-E-1");
        existingE.setSourceType("E");
        when(rfqDao.findById("UUID-E")).thenReturn(Optional.of(existingE));

        authenticate("catmgr@test.com");
        Role role = new Role();
        role.setRoleName("Admin");
        User adminUser = new User();
        adminUser.setUsername("catmgr@test.com");
        adminUser.setRole(role);
        when(userDao.findByUsernameAndActive("catmgr@test.com", true)).thenReturn(adminUser);

        Rfq reqE = new Rfq();
        reqE.setId("UUID-E");
        ResponseEntity<?> respE = service.getRfqAiTokenConsumption(reqE);
        assertEquals(HttpStatus.OK, respE.getStatusCode());

        Rfq existingMail = new Rfq();
        existingMail.setId("UUID-MAIL");
        existingMail.setRfqId("RFQ-MAIL-1");
        existingMail.setSourceType("MAIL");
        when(rfqDao.findById("UUID-MAIL")).thenReturn(Optional.of(existingMail));

        Rfq reqMail = new Rfq();
        reqMail.setId("UUID-MAIL");
        ResponseEntity<?> respMail = service.getRfqAiTokenConsumption(reqMail);
        assertEquals(HttpStatus.OK, respMail.getStatusCode());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsAuthorizedCategoryManager_FallbackToLatestUserName() {
        authenticate("fallback@test.com");
        when(userDao.findByUsernameAndActive("fallback@test.com", true)).thenReturn(null);

        Role role = new Role();
        role.setRoleName("Admin");
        User fallbackUser = new User();
        fallbackUser.setUsername("fallback@test.com");
        fallbackUser.setRole(role);
        when(userDao.findByLatestUserName("fallback@test.com")).thenReturn(fallbackUser);

        Boolean res = ReflectionTestUtils.invokeMethod(service, "isAuthorizedCategoryManager");
        assertTrue(Boolean.TRUE.equals(res));

        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unused")
    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }
}
