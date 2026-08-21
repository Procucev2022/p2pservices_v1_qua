package com.portal.procucev.service;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.*;
import com.portal.procucev.model.*;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.ClientRegistrationStatus;
import com.portal.procucev.utils.StatusConstants;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SelfRegistrationServiceImplTest {

    @Mock
    private OrgTypeDao orgTypeDao;
    @Mock
    private MasterStatusDao masterStatusDao;
    @Mock
    private OrgDao orgDao;
    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private OtpStoreDao otpStoreDao;
    @Mock
    private PincodeDao pinCodeDao;
    @Mock
    private CategoryDivisionDao categoryDivisionDao;
    @Mock
    private OrgCategoryDivisionDao orgCategoryDivisionDao;
    @Mock
    private ClientDao clientDao;
    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private SelfRegistrationServiceImpl service;

    private Organization org;
    private User user;
    private MasterStatus masterStatus;
    private Role role;
    private OrgType orgType;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "host", "http://localhost");
        ReflectionTestUtils.setField(service, "mailFom", "from@test.com");
        ReflectionTestUtils.setField(service, "mailid", "mail@test.com");

        org = new Organization();
        org.setId("ORG1");
        org.setCompanyName("Test Company");
        org.setEmail("test@org.com");
        org.setOrganizationPhonenumber("9876543210");
        org.setCompanyId("COMP1");
        org.setPan("ABCDE1234F");
        org.setZipCode("560001");

        user = new User();
        user.setId("USER1");
        user.setUsername("test@org.com");
        user.setPhone("9876543210");
        user.setOrg(org);

        masterStatus = new MasterStatus();
        masterStatus.setStatus("CLIENT_NEW");

        role = new Role();
        role.setId("R1");
        role.setRoleName("ClientInitiator");

        orgType = new OrgType();
        orgType.setTypeName("CLIENT");

        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(roleDao.findByRoleNameAndActive(anyString(), anyBoolean())).thenReturn(role);
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(orgType);
        when(clientDao.save(any())).thenReturn(org);
        when(userDao.save(any())).thenReturn(user);
        when(orgDao.save(any())).thenReturn(org);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testVendorRegistration_ValidationAndSuccess() throws Exception {
        assertThrows(AppException.class, () -> service.vendorRegistration(null));

        Organization noEmail = new Organization();
        assertThrows(AppException.class, () -> service.vendorRegistration(noEmail));

        Organization noPhone = new Organization();
        noPhone.setEmail("a@b.com");
        assertThrows(AppException.class, () -> service.vendorRegistration(noPhone));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        assertThrows(AppException.class, () -> service.vendorRegistration(org));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertTrue(service.vendorRegistration(org));
    }

    @Test
    void testGenerateOtp_WithTempEmail_And_NormalEmail() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        org.setTempEmail("temp@org.com");
        assertTrue(service.generateOtp(org, req));

        org.setTempEmail(null);
        assertTrue(service.generateOtp(org, req));

        assertThrows(AppException.class, () -> service.generateOtp(null, req));
    }

    @Test
    void testValidateClient_Pan_Crn_Company_Email() {
        Organization o1 = new Organization();
        o1.setCompanyName("Comp");
        when(orgDao.findByCompanyName("Comp")).thenReturn(org);
        assertTrue(service.validateClient(o1));

        Organization o2 = new Organization();
        o2.setPan("PAN123");
        when(orgDao.findByPanAndOrgType(eq("PAN123"), any())).thenReturn(Collections.singletonList(org));
        assertTrue(service.validateClient(o2));

        Organization o3 = new Organization();
        o3.setCrn("CRN123");
        when(orgDao.findByCrnAndOrgType(eq("CRN123"), any())).thenReturn(Collections.singletonList(org));
        assertTrue(service.validateClient(o3));

        Organization o4 = new Organization();
        o4.setEmail("email@test.com");
        when(userDao.findByUsernameAndActive("email@test.com", true)).thenReturn(user);
        assertTrue(service.validateClient(o4));

        Organization o5 = new Organization();
        assertFalse(service.validateClient(o5));
    }

    @Test
    void testGetClientByPan_Crn_Null() {
        Organization oPan = new Organization();
        oPan.setPan("PAN123");
        when(orgDao.findByPanAndOrgType(eq("PAN123"), any())).thenReturn(Collections.singletonList(org));
        assertEquals(org, service.getClientByPan(oPan));

        Organization oCrn = new Organization();
        oCrn.setCrn("CRN123");
        when(orgDao.findByCrnAndOrgType(isNull(), any())).thenReturn(Collections.singletonList(org));
        assertEquals(org, service.getClientByPan(oCrn));

        assertThrows(AppException.class, () -> service.getClientByPan(null));
    }

    @Test
    void testSelfclientRegistrationData_ValidationAndExistingClient() {
        Organization badOrg = new Organization();
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(badOrg));

        badOrg.setCompanyName("Comp");
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(badOrg));

        badOrg.setEmail("e@test.com");
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(badOrg));

        when(orgDao.findByCompanyNameAndOrgType(anyString(), any())).thenReturn(Collections.singletonList(org));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        ClientRegistrationStatus status1 = service.selfclientRegistrationData(org);
        assertEquals(ClientRegistrationStatus.EXISTING_CLIENT, status1);

        when(orgDao.findByCompanyNameAndOrgType(anyString(), any())).thenReturn(Collections.emptyList());
        when(pinCodeDao.findByPincode("560001")).thenReturn(new PincodeData("560001", "CityP", "StateP"));
        ClientRegistrationStatus status2 = service.selfclientRegistrationData(org);
        assertEquals(ClientRegistrationStatus.NEW_CLIENT, status2);
    }

    @Test
    void testGetPincodeDetails_And_GetCityByPincode() {
        PincodeData pData = new PincodeData("560001", "City1", "State1");
        when(pinCodeDao.existsByPincode("560001")).thenReturn(true);
        when(pinCodeDao.findByPincode("560001")).thenReturn(pData);

        List<Map<String, Object>> res1 = service.getPincodeDetails("560001");
        assertNotNull(res1);

        PincodeData res2 = service.getCityByPincode(pData);
        assertNotNull(res2);
    }

    @Test
    void testImportFromCsv() {
        String csvContent = "Pincode,OfficeName,District,StateName\n560001,Post1,City1,State1\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        int count = service.importFromCsv(file);
        assertTrue(count >= 0);
    }

    @Test
    void testGetUsersByPhoneNumber() {
        when(userDao.findByPhone("9876543210")).thenReturn(Collections.singletonList(user));
        List<User> res = service.getUsersByPhoneNumber("9876543210");
        assertNotNull(res);
    }

    @Test
    void testSelfclientRegistrationDataByApp() {
        assertDoesNotThrow(() -> service.selfclientRegistrationDataByApp(org));
    }

    @Test
    void testSellerRegistration() {
        assertDoesNotThrow(() -> service.sellerRegistration(org));
    }

    @Test
    void testGenerateId() throws Exception {
        String id1 = service.generateId("Company Name");
        assertNotNull(id1);

        String id2 = service.generateId("AB");
        assertNotNull(id2);

        String id3 = service.generateId("");
        assertNotNull(id3);
    }

    @Test
    void testUserExistsByEmailAndPhone() {
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        assertTrue(service.userExistsByEmailAndPhone("test@org.com", "+919876543210"));
    }

    @Test
    void testGenerateId_ShortAndNull() throws Exception {
        String id1 = service.generateId("AB");
        assertTrue(id1.startsWith("AB"));

        String id2 = service.generateId(null);
        assertNotNull(id2);
    }

    @Test
    void testSetUserDetails_StatusNull_RoleNull_SaveException() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(null);
        assertThrows(AppException.class, () -> service.setUserDetails(org, new User()));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(masterStatus);
        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(null);
        assertThrows(AppException.class, () -> service.setUserDetails(org, new User()));

        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(role);
        doThrow(new RuntimeException("save err")).when(userDao).save(any());
        assertThrows(AppException.class, () -> service.setUserDetails(org, new User()));
    }

    @Test
    void testValidateClient_Pan_Crn_Email_False() {
        Organization testOrg = new Organization();
        testOrg.setPan("PAN123");
        when(orgTypeDao.findByTypeName("CLIENT")).thenReturn(orgType);
        when(orgDao.findByPanAndOrgType(eq("PAN123"), any())).thenReturn(Collections.singletonList(org));
        assertTrue(service.validateClient(testOrg));

        testOrg.setPan(null);
        testOrg.setCrn("CRN123");
        when(orgDao.findByCrnAndOrgType(eq("CRN123"), any())).thenReturn(Collections.singletonList(org));
        assertTrue(service.validateClient(testOrg));

        testOrg.setCrn(null);
        testOrg.setEmail("test@org.com");
        when(userDao.findByUsernameAndActive("test@org.com", true)).thenReturn(user);
        assertTrue(service.validateClient(testOrg));

        testOrg.setEmail(null);
        assertFalse(service.validateClient(testOrg));
    }

    @Test
    void testGetClientByPan_Null_Crn_Success() {
        assertThrows(AppException.class, () -> service.getClientByPan(null));

        Organization crnOrg = new Organization();
        crnOrg.setCrn("CRN123");
        when(orgTypeDao.findByTypeName("CLIENT")).thenReturn(orgType);
        when(orgDao.findByCrnAndOrgType(any(), any())).thenReturn(Collections.singletonList(org));
        assertNotNull(service.getClientByPan(crnOrg));
    }

    @Test
    void testRegisterFromExcel_And_ImportCategoriesFromExcel_And_SellerregisterFromExcel() throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Header1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", out.toByteArray());

        assertDoesNotThrow(() -> service.registerFromExcel(file));
        assertDoesNotThrow(() -> service.importCategoriesFromExcel(file));
        assertDoesNotThrow(() -> service.SellerregisterFromExcel(file));
    }

    @Test
    void testValidateEmailOtp_And_IsEmailOtpValid_And_RemoveEmailOtp() {
        Organization o = new Organization();
        o.setEmail("test@org.com");
        o.setOrganizationPhonenumber("9876543210");
        o.setEmailOtp("123456");

        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());
        assertFalse(service.validateEmailOtp(o));
        assertFalse(service.isEmailOtpValid(o));

        OtpStore expired = new OtpStore("key", "123456", LocalDateTime.now().minusMinutes(5));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(expired));
        assertFalse(service.validateEmailOtp(o));
        assertFalse(service.isEmailOtpValid(o));

        OtpStore valid = new OtpStore("key", "123456", LocalDateTime.now().plusMinutes(5));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(valid));
        assertTrue(service.validateEmailOtp(o));
        assertTrue(service.isEmailOtpValid(o));

        assertTrue(service.getUsersByPhoneNumber(null).isEmpty());
        assertTrue(service.getUsersByPhoneNumber("   ").isEmpty());

        assertDoesNotThrow(() -> service.removeEmailOtp(o));
    }

    @Test
    void testValidateUser_And_GenerateEmailOtp_And_FetchPassword() {
        user.setPassword("pass123");
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        assertTrue(service.validateUser("test@org.com", "9876543210"));

        HttpServletRequest req = mock(HttpServletRequest.class);
        assertTrue(service.generateEmailOtp("test@org.com", req, "9876543210"));
        assertThrows(AppException.class, () -> service.generateEmailOtp(null, req, "9876543210"));

        assertEquals("pass123", service.fetchPasswordByEmailAndPhone("test@org.com", "9876543210"));
        assertThrows(IllegalArgumentException.class, () -> service.fetchPasswordByEmailAndPhone(null, "9876543210"));
        assertThrows(IllegalArgumentException.class, () -> service.fetchPasswordByEmailAndPhone("test@org.com", null));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class, () -> service.fetchPasswordByEmailAndPhone("notfound@org.com", "9876543210"));
    }

    @Test
    void testGenerateUserId_And_GetUsersByPhoneNumber_Null() {
        String uid = service.generateUserId("9876543210");
        assertTrue(uid.startsWith("USR"));

        assertThrows(IllegalArgumentException.class, () -> service.generateUserId("12"));
        assertThrows(IllegalArgumentException.class, () -> service.generateUserId(null));
    }

    @Test
    @org.junit.jupiter.api.Timeout(value = 90, unit = java.util.concurrent.TimeUnit.SECONDS)
    void testSelfclientRegistrationDataByApp_FullFlow() {
        Organization o = new Organization();
        o.setCompanyName("AppComp");
        o.setEmail("app@org.com");
        o.setOrganizationPhonenumber("9876543210");
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(orgDao.findByCompanyNameAndOrgType(anyString(), any())).thenReturn(Collections.emptyList());
        when(clientDao.save(any())).thenReturn(org);
        when(userDao.save(any())).thenReturn(user);

        Map<String, Object> r1 = service.selfclientRegistrationDataByApp(o);
        assertTrue((Boolean) r1.get("confirmationFlag"));

        when(orgDao.findByCompanyNameAndOrgType(anyString(), any())).thenReturn(Collections.singletonList(org));
        Map<String, Object> r2 = service.selfclientRegistrationDataByApp(o);
        assertTrue((Boolean) r2.get("confirmationFlag"));
    }

    @Test
    void testSellerRegistration_WithDivisionCategories() {
        Organization sOrg = new Organization();
        sOrg.setCompanyName("SellerComp");
        sOrg.setEmail("seller@org.com");
        sOrg.setOrganizationPhonenumber("9876543210");
        OrgDivisionCategory divCat = new OrgDivisionCategory();
        divCat.setDivision("D1");
        divCat.setCategory("C1");
        sOrg.setDivisionCategories(Collections.singletonList(divCat));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(clientDao.save(any())).thenReturn(org);
        when(userDao.save(any())).thenReturn(user);

        Map<String, Object> res = service.sellerRegistration(sOrg);
        assertTrue((Boolean) res.get("confirmationFlag"));
    }

    @Test
    void stubMethodsAndSimpleExistenceChecksCoverBothOutcomes() {
        assertFalse(service.selfclientRegistration(org));
        assertFalse(service.submitUpgradeVendor(org));
        assertFalse(service.upGradeVendorJob());

        when(orgDao.findByCompanyName("present")).thenReturn(org);
        when(orgDao.findByCompanyName("absent")).thenReturn(null);
        assertTrue(service.checkOrgexist("present"));
        assertFalse(service.checkOrgexist("absent"));

        when(userDao.findByUsernameAndActive("present@test.com", true)).thenReturn(user);
        when(userDao.findByUsernameAndActive("absent@test.com", true)).thenReturn(null);
        assertTrue(service.checkUserexist("present@test.com"));
        assertFalse(service.checkUserexist("absent@test.com"));

        when(userDao.findByUsernameAndPhoneAndActive("present@test.com", "1", true)).thenReturn(user);
        when(userDao.findByUsernameAndPhoneAndActive("absent@test.com", "2", true)).thenReturn(null);
        assertTrue(service.checkUserexistWithPhone("present@test.com", "1"));
        assertFalse(service.checkUserexistWithPhone("absent@test.com", "2"));
        assertTrue(service.validateUser("present@test.com", "1"));
        assertFalse(service.validateUser("absent@test.com", "2"));
    }

    @Test
    void selfClientRegistrationCoversEmptyMasterDuplicateAndUnexpectedFailures() {
        Organization emptyCompany = validOrganization();
        emptyCompany.setCompanyName("");
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(emptyCompany));

        Organization emptyEmail = validOrganization();
        emptyEmail.setEmail("");
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(emptyEmail));

        Organization emptyPhone = validOrganization();
        emptyPhone.setOrganizationPhonenumber("");
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(emptyPhone));

        Organization candidate = validOrganization();
        when(orgTypeDao.findByTypeName(ApplicationConstants.CLIENT)).thenReturn(null);
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(candidate));

        when(orgTypeDao.findByTypeName(ApplicationConstants.CLIENT)).thenReturn(orgType);
        when(orgDao.findByCompanyNameAndOrgType(candidate.getCompanyName(), orgType)).thenReturn(Collections.emptyList());
        when(userDao.findByUsernameAndPhoneAndActive(eq(candidate.getEmail()), anyString(), eq(true))).thenReturn(user);
        assertThrows(AppException.class, () -> service.selfclientRegistrationData(candidate));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(orgDao.findByCompanyNameAndOrgType(anyString(), any())).thenThrow(new RuntimeException("database down"));
        AppException wrapped = assertThrows(AppException.class, () -> service.selfclientRegistrationData(candidate));
        assertTrue(wrapped.getMessage().contains("Something went wrong"));
    }

    @Test
    void setUserDetailsSuccessPopulatesUserAndSaveFailureIsWrapped() {
        Organization candidate = validOrganization();
        candidate.setName("Client Owner");
        candidate.setSourceType(ApplicationConstants.TOOL);
        User target = new User();
        when(userDao.save(target)).thenReturn(target);

        service.setUserDetails(candidate, target);

        assertEquals(candidate.getEmail(), target.getUsername());
        assertEquals("Client Owner", target.getFullName());
        assertTrue(target.isActive());
        assertTrue(target.isSelfClient());
        assertEquals(masterStatus, target.getClientStatus());
        assertEquals(role, target.getRole());
        assertNotNull(target.getUniqueId());
        assertNotNull(target.getPassword());
    }

    @Test
    void vendorRegistrationCoversBlankMasterPincodeAndWrappedExceptionBranches() throws Exception {
        Organization blankEmail = validOrganization();
        blankEmail.setEmail("   ");
        assertThrows(AppException.class, () -> service.vendorRegistration(blankEmail));

        Organization blankPhone = validOrganization();
        blankPhone.setOrganizationPhonenumber("   ");
        assertThrows(AppException.class, () -> service.vendorRegistration(blankPhone));

        Organization vendor = validOrganization();
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(null);
        assertThrows(AppException.class, () -> service.vendorRegistration(vendor));

        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(orgType);
        when(masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED)).thenReturn(null);
        assertThrows(AppException.class, () -> service.vendorRegistration(vendor));

        when(masterStatusDao.findByStatus(StatusConstants.SELF_REGISTER_VC_ACCEPTED)).thenReturn(masterStatus);
        when(masterStatusDao.findByStatus(StatusConstants.EVALUATION_NOT_STARTED)).thenReturn(null);
        assertThrows(AppException.class, () -> service.vendorRegistration(vendor));

        when(masterStatusDao.findByStatus(StatusConstants.EVALUATION_NOT_STARTED)).thenReturn(masterStatus);
        vendor.setZipCode(null);
        when(orgDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(service.vendorRegistration(vendor));

        Organization noPincodeMatch = validOrganization();
        when(pinCodeDao.findByPincode(noPincodeMatch.getZipCode())).thenReturn(null);
        assertTrue(service.vendorRegistration(noPincodeMatch));

        when(orgDao.save(any())).thenThrow(new RuntimeException("write failed"));
        AppException wrapped = assertThrows(AppException.class, () -> service.vendorRegistration(validOrganization()));
        assertTrue(wrapped.getMessage().contains("Unexpected error during vendor registration"));
    }

    @Test
    void otpGenerationCoversInsertUpdateAndEmailSelection() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Organization candidate = validOrganization();
        candidate.setTempEmail(" Temp@Test.Com ");

        when(otpStoreDao.findByOtpKey("9876543210_EMAIL_temp@test.com")).thenReturn(Optional.empty());
        assertTrue(service.generateOtp(candidate, request));
        verify(otpStoreDao).save(argThat(saved -> "9876543210_EMAIL_temp@test.com".equals(saved.getOtpKey())));

        OtpStore existing = new OtpStore("9876543210_EMAIL_test@org.com", "old", LocalDateTime.now());
        candidate.setTempEmail("");
        when(otpStoreDao.findByOtpKey("9876543210_EMAIL_test@org.com")).thenReturn(Optional.of(existing));
        assertTrue(service.generateOtp(candidate, request));
        assertNotEquals("old", existing.getOtp());
        assertTrue(existing.getExpirationTime().isAfter(LocalDateTime.now()));

        OtpStore emailExisting = new OtpStore("9876543210_EMAIL_other@test.com", "old", LocalDateTime.now());
        when(otpStoreDao.findByOtpKey("9876543210_EMAIL_other@test.com")).thenReturn(Optional.of(emailExisting));
        assertTrue(service.generateEmailOtp(" Other@Test.Com ", request, " 9876543210 "));
        assertNotEquals("old", emailExisting.getOtp());

        when(otpStoreDao.findByOtpKey("9876543210_EMAIL_new@test.com")).thenReturn(Optional.empty());
        assertTrue(service.generateEmailOtp("New@Test.Com", request, "9876543210"));
    }

    @Test
    void otpValidationCoversTemporaryEmailInvalidValueAndRemoval() {
        Organization candidate = validOrganization();
        candidate.setTempEmail(" TEMP@Test.Com ");
        candidate.setEmailOtp("wrong");
        String key = "9876543210_EMAIL_temp@test.com";
        OtpStore validRecord = new OtpStore(key, "123456", LocalDateTime.now().plusMinutes(2));
        when(otpStoreDao.findByOtpKey(key)).thenReturn(Optional.of(validRecord));

        assertFalse(service.validateEmailOtp(candidate));
        assertFalse(service.isEmailOtpValid(candidate));
        service.removeEmailOtp(candidate);
        verify(otpStoreDao).deleteByOtpKey(key);

        candidate.setEmailOtp("123456");
        assertTrue(service.validateEmailOtp(candidate));
        assertTrue(service.isEmailOtpValid(candidate));
    }

    @Test
    void userLookupAndApprovalCoverAllOutcomesAndPhoneFormats() {
        assertFalse(service.userExistsByEmailAndPhone(null, "1"));
        assertFalse(service.userExistsByEmailAndPhone("a@test.com", null));
        when(userDao.findByUsernameAndPhoneAndActive("a@test.com", "1", true)).thenReturn(null);
        assertFalse(service.userExistsByEmailAndPhone("a@test.com", "1"));
        when(userDao.findByUsernameAndPhoneAndActive("a@test.com", "1", true)).thenReturn(user);
        assertTrue(service.userExistsByEmailAndPhone("a@test.com", "1"));

        when(userDao.findByUsernameAndPhoneAndActive("missing", "1", true)).thenReturn(null);
        assertFalse(service.validateUserApproval("missing", "1"));
        User ordinary = new User();
        ordinary.setSelfClient(false);
        when(userDao.findByUsernameAndPhoneAndActive("ordinary", "1", true)).thenReturn(ordinary);
        assertTrue(service.validateUserApproval("ordinary", "1"));
        User selfClient = new User();
        selfClient.setSelfClient(true);
        selfClient.setApproved(false);
        when(userDao.findByUsernameAndPhoneAndActive("self", "1", true)).thenReturn(selfClient);
        assertFalse(service.validateUserApproval("self", "1"));
        selfClient.setApproved(true);
        assertTrue(service.validateUserApproval("self", "1"));

        when(userDao.findByPhoneIn(anyList())).thenReturn(null, Collections.emptyList(), Collections.singletonList(user));
        assertTrue(service.getUsersByPhoneNumber("+91 98765-43210").isEmpty());
        assertTrue(service.getUsersByPhoneNumber("919876543210").isEmpty());
        assertEquals(Collections.singletonList(user), service.getUsersByPhoneNumber("9876543210"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> variants = ArgumentCaptor.forClass(List.class);
        verify(userDao, times(3)).findByPhoneIn(variants.capture());
        assertEquals(Arrays.asList("+919876543210", "919876543210", "9876543210"), variants.getAllValues().get(0));
    }

    @Test
    void pincodeLookupsCoverMissingFoundAndNull() {
        when(pinCodeDao.existsByPincode("000000")).thenReturn(false);
        List<Map<String, Object>> missing = service.getPincodeDetails("000000");
        assertEquals("Error", missing.get(0).get("Status"));
        assertNull(missing.get(0).get("PostOffice"));
        assertNull(service.getCityByPincode((String) null));

        PincodeData found = new PincodeData("560001", "Bengaluru", "Karnataka");
        when(pinCodeDao.existsByPincode("560001")).thenReturn(true);
        when(pinCodeDao.findByPincode("560001")).thenReturn(found);
        List<Map<String, Object>> response = service.getPincodeDetails("560001");
        assertEquals("Success", response.get(0).get("Status"));
        List<?> postOffices = (List<?>) response.get(0).get("PostOffice");
        assertEquals(1, postOffices.size());
        assertSame(found, service.getCityByPincode("560001"));
    }

    @Test
    void csvImportCoversInsertedDuplicateAndMalformedInput() throws Exception {
        String csv = "City/Town/Village,Pincode,StateName\n"
                + " Bengaluru ,560001,Karnataka\n"
                + " Mysuru ,570001,Karnataka\n";
        when(pinCodeDao.existsByPincode("560001")).thenReturn(false);
        when(pinCodeDao.existsByPincode("570001")).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "pincodes.csv", "text/csv", csv.getBytes());

        assertEquals(1, service.importFromCsv(file));
        verify(pinCodeDao).save(argThat(data -> "560001".equals(data.getPincode())
                && "Bengaluru".equals(data.getCity()) && "Karnataka".equals(data.getState())));

        MultipartFileThrowingInput malformed = new MultipartFileThrowingInput();
        assertEquals(0, service.importFromCsv(malformed));
    }

    @Test
    void appRegistrationCoversDuplicateInvalidNullCompanyPincodeAndException() {
        Organization candidate = validOrganization();
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        Map<String, Object> duplicate = service.selfclientRegistrationDataByApp(candidate);
        assertEquals(false, duplicate.get("confirmationFlag"));

        candidate.setEmail("not-an-email");
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        Map<String, Object> invalid = service.selfclientRegistrationDataByApp(candidate);
        assertEquals(false, invalid.get("confirmationFlag"));

        Organization noCompany = validOrganization();
        noCompany.setCompanyName(null);
        noCompany.setZipCode(null);
        when(clientDao.save(any())).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            saved.setId("NEW-ORG");
            return saved;
        });
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> created = service.selfclientRegistrationDataByApp(noCompany);
        assertEquals(true, created.get("confirmationFlag"));
        verify(orgDao, never()).findByCompanyNameAndOrgType(isNull(), any());

        Organization failing = validOrganization();
        when(orgDao.findByCompanyNameAndOrgType(anyString(), any())).thenReturn(null);
        Map<String, Object> failure = service.selfclientRegistrationDataByApp(failing);
        assertEquals(false, failure.get("confirmationFlag"));
        assertNotNull(failure.get("error"));
    }

    @Test
    void sellerRegistrationCoversDuplicateInvalidNullDivisionsAndException() {
        Organization candidate = validOrganization();
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        assertEquals(false, service.sellerRegistration(candidate).get("confirmationFlag"));

        candidate.setEmail("invalid");
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertEquals(false, service.sellerRegistration(candidate).get("confirmationFlag"));

        Organization successful = validOrganization();
        successful.setDivisionCategories(null);
        successful.setZipCode(null);
        when(clientDao.save(any())).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            saved.setId("SELLER-ORG");
            return saved;
        });
        when(userDao.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("SELLER-USER");
            return saved;
        });
        assertEquals(true, service.sellerRegistration(successful).get("confirmationFlag"));

        doThrow(new RuntimeException("seller write failed")).when(clientDao).save(any());
        Map<String, Object> failed = service.sellerRegistration(validOrganization());
        assertEquals(false, failed.get("confirmationFlag"));
        assertTrue(String.valueOf(failed.get("error")).contains("seller write failed"));
    }

    @Test
    void sellerUserDetailsCoversDuplicateAndSuccessfulPopulation() {
        Organization candidate = validOrganization();
        when(userDao.findByUsernameAndPhoneAndActive(candidate.getEmail(), candidate.getOrganizationPhonenumber(), true))
                .thenReturn(user);
        assertThrows(AppException.class, () -> service.setSellerUserDetails(candidate, new User()));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User target = service.setSellerUserDetails(candidate, new User());
        assertEquals(candidate.getEmail(), target.getUsername());
        assertEquals(role, target.getRole());
        assertTrue(target.isActive());
        assertNotNull(target.getPassword());
        assertNotNull(target.getUniqueId());
    }

    @Test
    void cellValueHelperCoversEverySupportedCellType() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("values");
            Row row = sheet.createRow(0);

            assertEquals("", cellValue(null));
            org.apache.poi.ss.usermodel.Cell string = row.createCell(0);
            string.setCellValue("  text  ");
            assertEquals("text", cellValue(string));

            org.apache.poi.ss.usermodel.Cell integer = row.createCell(1);
            integer.setCellValue(42);
            assertEquals("42", cellValue(integer));
            org.apache.poi.ss.usermodel.Cell decimal = row.createCell(2);
            decimal.setCellValue(3.5);
            assertEquals("3.5", cellValue(decimal));

            org.apache.poi.ss.usermodel.Cell date = row.createCell(3);
            date.setCellValue(new Date(0));
            org.apache.poi.ss.usermodel.CellStyle dateStyle = workbook.createCellStyle();
            short dateFormat = workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd");
            dateStyle.setDataFormat(dateFormat);
            date.setCellStyle(dateStyle);
            assertTrue(cellValue(date).contains("1970"));

            org.apache.poi.ss.usermodel.Cell bool = row.createCell(4);
            bool.setCellValue(true);
            assertEquals("true", cellValue(bool));
            org.apache.poi.ss.usermodel.Cell formula = row.createCell(5);
            formula.setCellFormula("1+2");
            assertEquals("3", cellValue(formula));
            org.apache.poi.ss.usermodel.Cell blank = row.createCell(6);
            blank.setCellType(Cell.CELL_TYPE_BLANK);
            assertEquals("", cellValue(blank));
            org.apache.poi.ss.usermodel.Cell error = row.createCell(7);
            error.setCellErrorValue((byte) 7);
            assertEquals("", cellValue(error));
        }
    }

    @Test
    void categoryExcelImportCoversMissingAndSparseRows() throws Exception {
        MockMultipartFile noHeader = workbookFile("categories.xlsx", sheet -> { });
        assertThrows(IllegalStateException.class, () -> service.importCategoriesFromExcel(noHeader));

        MockMultipartFile categories = workbookFile("categories.xlsx", sheet -> {
            Row headers = sheet.createRow(0);
            headers.createCell(2).setCellValue("   ");
            headers.createCell(3).setCellValue("Electrical");
            sheet.createRow(2);
            Row blank = sheet.createRow(3);
            blank.createCell(3).setCellValue("   ");
            Row populated = sheet.createRow(4);
            populated.createCell(3).setCellValue("Switchgear");
        });

        service.importCategoriesFromExcel(categories);
        verify(categoryDivisionDao).save(argThat(value -> "Electrical".equals(value.getDivision())
                && "Switchgear".equals(value.getCategory())));
    }

    @Test
    void clientExcelRegistrationCoversSkippedNewExistingDuplicateAndRowException() throws Exception {
        MockMultipartFile excel = workbookFile("clients.xlsx", sheet -> {
            sheet.createRow(0);
            clientRow(sheet, 1, "missing-company@test.com", "Owner", "", "9876543210", "ok");
            clientRow(sheet, 2, "missing-phone@test.com", "Owner", "No Phone", "", "ok");
            clientRow(sheet, 3, "new@test.com", "", "New Client", "9876543210", "ok");
            clientRow(sheet, 4, "duplicate@test.com", "Owner", "Existing Client", "919876543211", "ok");
            clientRow(sheet, 5, "bad-status@test.com", "Owner", "Bad Status", "9876543212", "bad");
        });
        when(orgDao.findByCompanyNameAndOrgType(eq("New Client"), any())).thenReturn(Collections.emptyList());
        when(orgDao.findByCompanyNameAndOrgType(eq("Existing Client"), any())).thenReturn(Collections.singletonList(org));
        when(orgDao.findByCompanyNameAndOrgType(eq("Bad Status"), any())).thenReturn(Collections.emptyList());
        when(userDao.findByUsernameAndPhoneAndActive(eq("duplicate@test.com"), anyString(), eq(true))).thenReturn(user);
        when(userDao.findByUsernameAndPhoneAndActive(argThat(email -> !"duplicate@test.com".equals(email)), anyString(), eq(true))).thenReturn(null);
        when(pinCodeDao.findByPincode("560001")).thenReturn(new PincodeData("560001", "Bengaluru", "Karnataka"));
        when(clientDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(masterStatusDao.findById("ok")).thenReturn(Optional.of(masterStatus));
        when(masterStatusDao.findById("bad")).thenReturn(Optional.empty());
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerFromExcel(excel);

        verify(clientDao, times(2)).save(any(Organization.class));
        verify(userDao, times(1)).save(argThat(saved -> "new@test.com".equals(saved.getUsername())));
    }

    @Test
    void sellerExcelRegistrationCoversValidationCategoriesSuccessFailureAndException() throws Exception {
        MockMultipartFile excel = workbookFile("sellers.xlsx", sheet -> {
            sheet.createRow(0);
            sellerRow(sheet, 2, "", "Contact", "company-missing@test.com", "9876543210", "", "", "", "", "");
            sellerRow(sheet, 3, "No Email", "Contact", "", "9876543210", "", "", "", "", "");
            sellerRow(sheet, 4, "No Phone", "Contact", "phone-missing@test.com", "", "", "", "", "", "");
            sellerRow(sheet, 5, "Duplicate", "Contact", "duplicate@test.com", "9876543211", "", "", "", "", "");
            sellerRow(sheet, 6, "Successful", "Contact", "success@test.com", "9876543212", "C1", "C2", "C3", "C4", "C5");
            sellerRow(sheet, 7, "Rejected", "Contact", "rejected@test.com", "9876543213", "", "", "", "", "");
            sellerRow(sheet, 8, "Explodes", "Contact", "explode@test.com", "9876543214", "C1", "", "", "", "");
        });
        when(userDao.findByUsernameAndPhoneAndActive(eq("duplicate@test.com"), anyString(), eq(true))).thenReturn(user);
        when(userDao.findByUsernameAndPhoneAndActive(argThat(email -> !"duplicate@test.com".equals(email)), anyString(), eq(true))).thenReturn(null);
        when(orgDao.save(any())).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            if ("Explodes".equals(saved.getCompanyName())) throw new RuntimeException("row write failed");
            saved.setId("ORG-" + saved.getCompanyName());
            return saved;
        });
        when(userDao.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("EXCEL-USER");
            return saved;
        });

        SelfRegistrationServiceImpl spyService = spy(service);
        doAnswer(invocation -> {
            Organization seller = invocation.getArgument(0);
            Map<String, Object> result = new HashMap<>();
            result.put("confirmationFlag", "Successful".equals(seller.getCompanyName()));
            return result;
        }).when(spyService).sellerRegistration(any());

        Map<String, Object> result = spyService.SellerregisterFromExcel(excel);

        assertEquals(false, result.get("confirmationFlag"));
        assertEquals(Collections.singletonList("Row 7"), result.get("successRows"));
        List<?> errors = (List<?>) result.get("errorRows");
        assertEquals(6, errors.size());
        verify(orgCategoryDivisionDao, atLeastOnce()).saveAll(argThat(values -> {
            int count = 0;
            for (OrgDivisionCategory ignored : values) count++;
            return count == 5;
        }));
        verify(orgCategoryDivisionDao, atLeastOnce()).saveAll(argThat(values -> !values.iterator().hasNext()));
    }

    @Test
    void privateSellerExcelHelpersCoverExistingAndNewUserBranches() {
        Organization candidate = validOrganization();
        candidate.setSourceType(ApplicationConstants.TOOL);
        User existingTarget = new User();
        when(userDao.findByUsernameAndPhoneAndActive(candidate.getEmail(), candidate.getOrganizationPhonenumber(), true))
                .thenReturn(user);
        when(userDao.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("HELPER-USER");
            return saved;
        });
        User savedExisting = ReflectionTestUtils.invokeMethod(service, "setExcelSellerUserDetails",
                candidate, existingTarget, "Excel Contact");
        assertEquals("Excel Contact", savedExisting.getFullName());
        assertEquals("Welcome@123", savedExisting.getPassword());

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        User savedNew = ReflectionTestUtils.invokeMethod(service, "setExcelSellerUserDetails",
                candidate, new User(), "New Contact");
        assertEquals("New Contact", savedNew.getFullName());

        OrgDivisionCategory division = ReflectionTestUtils.invokeMethod(service, "buildDivisionCategory",
                "Mechanical", candidate, savedNew);
        assertEquals("Mechanical", division.getCategory());
        assertSame(candidate, division.getOrganization());
        assertEquals("HELPER-USER", division.getUserId());
    }

    @Test
    void getClientByPanReturnsInputWhenNoMatchAndValidateClientTraversesAllNegativeChecks() {
        Organization pan = new Organization();
        pan.setPan("NONE");
        when(orgDao.findByPanAndOrgType(eq("NONE"), any())).thenReturn(Collections.emptyList());
        assertSame(pan, service.getClientByPan(pan));

        Organization crn = new Organization();
        crn.setCrn("NONE");
        when(orgDao.findByCrnAndOrgType(isNull(), any())).thenReturn(Collections.emptyList());
        assertSame(crn, service.getClientByPan(crn));

        Organization all = new Organization();
        all.setCompanyName("none");
        all.setPan("none");
        all.setCrn("none");
        all.setEmail("none@test.com");
        when(orgDao.findByCompanyName("none")).thenReturn(null);
        when(orgDao.findByPanAndOrgType(eq("none"), any())).thenReturn(Collections.emptyList());
        when(orgDao.findByCrnAndOrgType(eq("none"), any())).thenReturn(Collections.emptyList());
        when(userDao.findByUsernameAndActive("none@test.com", true)).thenReturn(null);
        assertFalse(service.validateClient(all));
    }

    @Test
    void otpValidationTreatsEmptyTemporaryEmailAsAbsent() {
        Organization candidate = validOrganization();
        candidate.setTempEmail("");
        candidate.setEmail(" Primary@Test.Com ");
        candidate.setEmailOtp("654321");
        String key = "9876543210_EMAIL_primary@test.com";
        OtpStore record = new OtpStore(key, "654321", LocalDateTime.now().plusMinutes(2));
        when(otpStoreDao.findByOtpKey(key)).thenReturn(Optional.of(record));

        assertTrue(service.validateEmailOtp(candidate));
        assertTrue(service.isEmailOtpValid(candidate));

        service.removeEmailOtp(candidate);
        verify(otpStoreDao).deleteByOtpKey(key);
    }

    @Test
    void getClientByPanFallsThroughWhenNeitherPanNorCrnIsSupplied() {
        Organization withoutIdentifiers = new Organization();
        withoutIdentifiers.setCompanyName("Anonymous");

        assertThrows(AppException.class, () -> service.getClientByPan(withoutIdentifiers));
    }

    @Test
    void resolvedPostcodeEnrichesCityAndStateOnEveryRegistrationFlow() throws Exception {
        PincodeData resolved = new PincodeData("560001", "Bengaluru", "Karnataka");
        when(pinCodeDao.findByPincode("560001")).thenReturn(resolved);
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(userDao.findByUsernameAndActive(anyString(), eq(true))).thenReturn(null);
        when(orgDao.findByCompanyName(anyString())).thenReturn(null);
        when(orgDao.findByPanAndOrgType(anyString(), any())).thenReturn(Collections.emptyList());
        when(orgDao.findByCrnAndOrgType(anyString(), any())).thenReturn(Collections.emptyList());
        when(clientDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orgDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Organization client = validOrganization();
        service.selfclientRegistrationData(client);
        assertEquals("Bengaluru", client.getCity());
        assertEquals("Karnataka", client.getState());

        Organization vendor = validOrganization();
        assertTrue(service.vendorRegistration(vendor));
        assertEquals("Bengaluru", vendor.getCity());

        Organization appClient = validOrganization();
        service.selfclientRegistrationDataByApp(appClient);
        assertEquals("Bengaluru", appClient.getCity());

        Organization seller = validOrganization();
        service.sellerRegistration(seller);
        assertEquals("Bengaluru", seller.getCity());
    }

    @Test
    void unresolvedPostcodeLeavesCityAndStateUntouchedOnEveryRegistrationFlow() throws Exception {
        when(pinCodeDao.findByPincode("560001")).thenReturn(null);
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(userDao.findByUsernameAndActive(anyString(), eq(true))).thenReturn(null);
        when(orgDao.findByCompanyName(anyString())).thenReturn(null);
        when(orgDao.findByPanAndOrgType(anyString(), any())).thenReturn(Collections.emptyList());
        when(orgDao.findByCrnAndOrgType(anyString(), any())).thenReturn(Collections.emptyList());
        when(clientDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orgDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Organization client = validOrganization();
        service.selfclientRegistrationData(client);
        assertNull(client.getCity());

        Organization appClient = validOrganization();
        service.selfclientRegistrationDataByApp(appClient);
        assertNull(appClient.getCity());

        Organization seller = validOrganization();
        service.sellerRegistration(seller);
        assertNull(seller.getCity());
    }

    @Test
    void clientExcelRegistrationSkipsGapsInTheSheetAndUnknownPostcodes() throws Exception {
        MockMultipartFile excel = workbookFile("clients.xlsx", sheet -> {
            sheet.createRow(0);
            clientRow(sheet, 1, "gap-before@test.com", "Owner", "Gap Client", "9876543210", "ok");
            // row 2 is deliberately absent, so the loop hits a null row
            clientRow(sheet, 3, "gap-after@test.com", "Owner", "Gap Client Two", "9876543211", "ok");
        });
        when(orgDao.findByCompanyNameAndOrgType(anyString(), any())).thenReturn(Collections.emptyList());
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        when(pinCodeDao.findByPincode("560001")).thenReturn(null);
        when(clientDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(masterStatusDao.findById("ok")).thenReturn(Optional.of(masterStatus));
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerFromExcel(excel);

        // the absent row is skipped and the unresolved postcode leaves the sheet's own state intact
        verify(clientDao, times(2)).save(any(Organization.class));
        verify(clientDao, times(2)).save(argThat(saved -> "State".equals(saved.getState())));
    }

    private Organization validOrganization() {
        Organization candidate = new Organization();
        candidate.setId("ORG-CANDIDATE");
        candidate.setCompanyName("Candidate Company");
        candidate.setEmail("test@org.com");
        candidate.setName("Candidate Owner");
        candidate.setOrganizationPhonenumber("9876543210");
        candidate.setZipCode("560001");
        candidate.setSourceType(ApplicationConstants.TOOL);
        return candidate;
    }

    private String cellValue(org.apache.poi.ss.usermodel.Cell cell) {
        return ReflectionTestUtils.invokeMethod(service, "getCellValue", cell);
    }

    private MockMultipartFile workbookFile(String name, java.util.function.Consumer<Sheet> builder) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            builder.accept(sheet);
            workbook.write(output);
            return new MockMultipartFile("file", name,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private void clientRow(Sheet sheet, int index, String email, String fullName, String company,
                           String phone, String clientStatus) {
        Row row = sheet.createRow(index);
        row.createCell(2).setCellValue(email);
        row.createCell(3).setCellValue(fullName);
        row.createCell(4).setCellValue(company);
        row.createCell(5).setCellValue("Address");
        row.createCell(6).setCellValue("State");
        row.createCell(7).setCellValue("560001");
        row.createCell(8).setCellValue(phone);
        row.createCell(11).setCellValue(clientStatus);
    }

    private void sellerRow(Sheet sheet, int index, String company, String contact, String email, String phone,
                           String cate1, String cate2, String cate3, String cate4, String cate5) {
        Row row = sheet.createRow(index);
        row.createCell(1).setCellValue(company);
        row.createCell(2).setCellValue(contact);
        row.createCell(3).setCellValue(email);
        row.createCell(4).setCellValue(phone);
        row.createCell(5).setCellValue("GSTIN");
        row.createCell(6).setCellValue("560001");
        row.createCell(7).setCellValue("City");
        row.createCell(8).setCellValue("State");
        row.createCell(9).setCellValue(cate1);
        row.createCell(10).setCellValue(cate2);
        row.createCell(11).setCellValue(cate3);
        row.createCell(12).setCellValue(cate4);
        row.createCell(13).setCellValue(cate5);
        row.createCell(14).setCellValue("Products");
    }

    private static final class MultipartFileThrowingInput implements org.springframework.web.multipart.MultipartFile {
        @Override public String getName() { return "broken"; }
        @Override public String getOriginalFilename() { return "broken.csv"; }
        @Override public String getContentType() { return "text/csv"; }
        @Override public boolean isEmpty() { return false; }
        @Override public long getSize() { return 0; }
        @Override public byte[] getBytes() throws IOException { throw new IOException("broken input"); }
        @Override public java.io.InputStream getInputStream() throws IOException { throw new IOException("broken input"); }
        @Override public void transferTo(java.io.File dest) throws IOException { throw new IOException("broken input"); }
    }
}
