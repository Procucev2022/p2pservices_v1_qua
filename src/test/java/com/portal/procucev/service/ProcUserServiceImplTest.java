package com.portal.procucev.service;

import com.portal.procucev.Dto.SimplePageResponse;
import com.portal.procucev.Dto.UserActivityDto;
import com.portal.procucev.Dto.VendorSummaryResponse;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.*;
import com.portal.procucev.model.*;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusConstants;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcUserServiceImplTest {

    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private OrgDao orgDao;
    @Mock
    private RfqDao rfqDao;
    @Mock
    private OrgTypeDao orgTypeDao;
    @Mock
    private OtpStoreDao otpStoreDao;
    @Mock
    private EmailUserRepo emailUserRepo;
    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private UserActivityDao userActivityDao;
    @Mock
    private MimeMessage mimeMessage;

    private ProcUserServiceImpl service;

    private User user;
    private Organization org;
    private Role role;

    @BeforeEach
    void setUp() {
        service = new ProcUserServiceImpl(null);
        ReflectionTestUtils.setField(service, "userDao", userDao);
        ReflectionTestUtils.setField(service, "roleDao", roleDao);
        ReflectionTestUtils.setField(service, "orgDao", orgDao);
        ReflectionTestUtils.setField(service, "rfqDao", rfqDao);
        ReflectionTestUtils.setField(service, "orgTypeDao", orgTypeDao);
        ReflectionTestUtils.setField(service, "otpStoreDao", otpStoreDao);
        ReflectionTestUtils.setField(service, "emailUserRepo", emailUserRepo);
        ReflectionTestUtils.setField(service, "javaMailSender", javaMailSender);
        ReflectionTestUtils.setField(service, "userActivityDao", userActivityDao);

        ReflectionTestUtils.setField(service, "host", "http://localhost");
        ReflectionTestUtils.setField(service, "mailFom", "from@test.com");
        ReflectionTestUtils.setField(service, "mailid", "mail@test.com");

        org = new Organization();
        org.setId("ORG1");
        org.setEmail("test@org.com");
        org.setOrganizationPhonenumber("9876543210");
        org.setCompanyName("Company A");
        org.setEmailOtp("123456");
        org.setDetails("Details");
        org.setGstin("GSTIN123");
        org.setAddress1("Address 1");
        org.setState("State");
        org.setCity("City");
        org.setZipCode("123456");
        org.setContactPerson("Person");

        role = new Role();
        role.setId("ROLE1");
        role.setRoleName("ROLE_USER");

        user = new User();
        user.setId("USER1");
        user.setUsername("test@org.com");
        user.setPhone("9876543210");
        user.setOrg(org);
        user.setActive(true);
        user.setRole(role);
        user.setVerificationStatus("NOT_VERIFIED");
        user.setSelfClient(false);

        lenient().when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testSave_And_CreateUser_And_FindAll_And_Delete() {
        when(userDao.save(any())).thenReturn(user);
        assertEquals(user, service.save(user));
        assertEquals(user, service.createUser(user));

        assertNull(service.findAll());
        assertDoesNotThrow(() -> service.delete(1L));
    }

    @Test
    void testGetUserByEmail_Null_And_Found() {
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(rfqDao.findRfqCountByUser(anyString())).thenReturn(3);
        when(emailUserRepo.findByEmail(anyString())).thenReturn(new EmailUser());

        User res = service.getUserByEmail(user);
        assertNotNull(res);
        assertTrue(res.isAuth());
    }

    @Test
    void testGetUserByEmail_NotFound() {
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertThrows(AppException.class, () -> service.getUserByEmail(user));
    }

    @Test
    void testChangePassword_UserNotFound_OldPasswordMismatch_Success() {
        ResetPassword reset = new ResetPassword();
        reset.setUserName("user1@test.com");
        reset.setPhone("9876543210");
        reset.setPassword("old");
        reset.setNewpassword("new");

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertThrows(AppException.class, () -> service.changePassword(reset));

        user.setPassword("correct_old");
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        assertThrows(AppException.class, () -> service.changePassword(reset));

        reset.setPassword("correct_old");
        assertTrue(service.changePassword(reset));
    }

    @Test
    void testCheckUserexist() {
        when(userDao.findByUsernameAndActive("test@org.com", true)).thenReturn(user);
        assertTrue(service.checkUserexist("test@org.com"));

        when(userDao.findByUsernameAndActive("notfound@org.com", true)).thenReturn(null);
        assertFalse(service.checkUserexist("notfound@org.com"));
    }

    @Test
    void testUpdateUserByRoleByOrg() {
        when(userDao.findByOrgIn(any())).thenReturn(Collections.singletonList(user));
        List<User> res = service.updateUserByRoleByOrg(Collections.singletonList(org), role);
        assertEquals(1, res.size());
    }

    @Test
    void testForgotPassword() {
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertThrows(AppException.class, () -> service.forgotPassword(user));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(userDao.save(any())).thenReturn(user);
        assertTrue(service.forgotPassword(user));
    }

    @Test
    void testSaveEmailuser_And_UpdateEmailUserPswd() {
        EmailUser eUser = new EmailUser();
        eUser.setEmail("e@test.com");
        eUser.setPassword("pass");

        when(emailUserRepo.save(any())).thenReturn(eUser);
        assertTrue(service.saveEmailuser(eUser));

        when(emailUserRepo.findByEmail("e@test.com")).thenReturn(null);
        assertFalse(service.updateEmailUserPswd(eUser));

        when(emailUserRepo.findByEmail("e@test.com")).thenReturn(eUser);
        assertTrue(service.updateEmailUserPswd(eUser));
    }

    @Test
    void testDisableUser_EmptyEmailsAndNonEmptyEmails() {
        when(userDao.findById(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.disableUser(user));

        when(userDao.findById(anyString())).thenReturn(Optional.of(user));
        when(userDao.findOrgIdByUser(anyString())).thenReturn("ORG1");
        
        when(userDao.findByOrg("ORG1")).thenReturn(Collections.emptyList());
        assertTrue(service.disableUser(user));

        when(userDao.findByOrg("ORG1")).thenReturn(Collections.singletonList("admin@org.com"));
        assertTrue(service.disableUser(user));
    }

    @Test
    void testGenerateOtp_UserNotFound_Success() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertFalse(service.generateOtp(org, req));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());

        assertTrue(service.generateOtp(org, req));
    }

    @Test
    void testValidateOtp_MapCache() throws Exception {
        assertFalse(service.validateOtp(null));

        Organization orgMissing = new Organization();
        assertFalse(service.validateOtp(orgMissing));

        Field mapField = ProcUserServiceImpl.class.getDeclaredField("otpsMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, OtpDetails> map = (Map<String, OtpDetails>) mapField.get(service);

        String key = "test@org.com|9876543210";
        map.put(key, new OtpDetails("123456", LocalDateTime.now().plusMinutes(5)));

        assertTrue(service.validateOtp(org));
    }

    @Test
    void testValidateEmailOtp_FullFlow() throws Exception {
        assertFalse(service.validateEmailOtp(null));

        OtpStore store = new OtpStore("key", "123456", LocalDateTime.now().plusMinutes(10));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(store));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(userDao.updateActivityTs(anyString(), anyString(), eq(StatusConstants.EMAIL_VERIFIED))).thenReturn(1);

        assertTrue(service.validateEmailOtp(org));
    }

    @Test
    void testGetVendorSummary_WithLastLoginData() {
        OrgType vType = new OrgType();
        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(vType);

        Page<Organization> page = new PageImpl<>(Collections.singletonList(org));
        when(orgDao.findVendors(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        List<Object[]> lastLoginData = new ArrayList<>();
        lastLoginData.add(new Object[]{"ORG1", new Date()});
        when(userDao.findLastLoginByOrgIds(any())).thenReturn(lastLoginData);

        SimplePageResponse<VendorSummaryResponse> res = service.getVendorSummary(0, 10, null, null);
        assertNotNull(res);
        assertEquals(1, res.getTotalRecords());
    }

    @Test
    void testDeactivateOrgUser() {
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        assertTrue(service.deactivateOrgUser(user));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertThrows(AppException.class, () -> service.deactivateOrgUser(user));
    }

    @Test
    void testCheckUserexist_TrueAndFalse() {
        when(userDao.findByUsernameAndActive("test@org.com", true)).thenReturn(user);
        assertTrue(service.checkUserexist("test@org.com"));

        when(userDao.findByUsernameAndActive("notfound@org.com", true)).thenReturn(null);
        assertFalse(service.checkUserexist("notfound@org.com"));
    }

    @Test
    void testUpdateUserByRoleByOrg_SuccessAndException() {
        when(userDao.findByOrgIn(any())).thenReturn(Collections.singletonList(user));
        List<User> res = service.updateUserByRoleByOrg(Collections.singletonList(org), role);
        assertEquals(1, res.size());

        when(userDao.findByOrgIn(any())).thenThrow(new RuntimeException("db err"));
        assertNull(service.updateUserByRoleByOrg(Collections.singletonList(org), role));
    }

    @Test
    void testUpdateEmailUserPswd_Null_NotFound_Success() {
        assertFalse(service.updateEmailUserPswd(null));

        EmailUser eu = new EmailUser();
        eu.setEmail("test@org.com");
        eu.setPassword("newpass");

        when(emailUserRepo.findByEmail("test@org.com")).thenReturn(null);
        assertFalse(service.updateEmailUserPswd(eu));

        when(emailUserRepo.findByEmail("test@org.com")).thenReturn(eu);
        assertTrue(service.updateEmailUserPswd(eu));
        verify(emailUserRepo).updatePassword("test@org.com", "newpass");
    }

    @Test
    void testDisableUser_SuccessAndNotFound() {
        when(userDao.findById("USER1")).thenReturn(Optional.of(user));
        when(userDao.findOrgIdByUser("USER1")).thenReturn("ORG1");
        when(userDao.findByOrg("ORG1")).thenReturn(Collections.singletonList("test@org.com"));

        assertTrue(service.disableUser(user));
        verify(orgDao).updateEmailByOrg("ORG1", "test@org.com");

        when(userDao.findById("USER1")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.disableUser(user));
    }

    @Test
    void testUpdateOrganization_And_UpdateBuyer() {
        assertFalse(service.updateOrganization(null));
        assertFalse(service.updateBuyer(null));

        Organization orgInput = new Organization();
        orgInput.setId("ORG1");
        orgInput.setCompanyName("New Company");
        orgInput.setBranches(Collections.singletonList(new OrgBranches()));
        orgInput.setDivisionCategories(Collections.singletonList(new OrgDivisionCategory()));

        Organization existingOrg = new Organization();
        existingOrg.setId("ORG1");
        existingOrg.setBranches(new ArrayList<>());
        existingOrg.setDivisionCategories(new ArrayList<>());

        when(orgDao.findById("ORG1")).thenReturn(Optional.of(existingOrg));
        assertTrue(service.updateOrganization(orgInput));

        Organization orgInputAll = new Organization();
        orgInputAll.setId("ORG1");
        orgInputAll.setCompanyName("New Company");
        orgInputAll.setDetails("Details");
        orgInputAll.setGstin("GSTIN");
        orgInputAll.setAddress1("Address");
        orgInputAll.setState("State");
        orgInputAll.setCity("City");
        orgInputAll.setZipCode("Zip");
        orgInputAll.setContactPerson("Contact");
        orgInputAll.setEmail("Email");
        orgInputAll.setOrganizationPhonenumber("Phone");
        orgInputAll.setBranches(Collections.emptyList());
        orgInputAll.setDivisionCategories(Collections.emptyList());
        assertTrue(service.updateOrganization(orgInputAll));

        OrgDivisionCategory cat = new OrgDivisionCategory();
        cat.setDivision("D1");
        cat.setCategory("C1");
        orgInput.setDivisionCategories(Collections.singletonList(cat));
        assertTrue(service.updateBuyer(orgInput));
    }

    @Test
    void testGetVendorSummary_Branches() {
        when(orgTypeDao.findByTypeName("VENDOR")).thenReturn(null);
        SimplePageResponse<VendorSummaryResponse> r1 = service.getVendorSummary(0, 10, null, null);
        assertEquals(0, r1.getTotalRecords());

        OrgType orgType = new OrgType();
        when(orgTypeDao.findByTypeName("VENDOR")).thenReturn(orgType);
        Page<Organization> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orgDao.findVendors(any(), any(), any(), any())).thenReturn(emptyPage);
        SimplePageResponse<VendorSummaryResponse> r2 = service.getVendorSummary(0, 10, null, null);
        assertEquals(0, r2.getTotalRecords());

        Organization vOrg = new Organization();
        vOrg.setId("V1");
        vOrg.setCompanyName("V Company");
        vOrg.setSubscriptionPlan(new SubscriptionPlan());
        vOrg.setRfqUsedCount(2L);
        vOrg.setQuoteSubmitted(1L);
        vOrg.setSubscriptionExpiry(new Date());
        Page<Organization> vPage = new PageImpl<>(Collections.singletonList(vOrg));
        when(orgDao.findVendors(any(), any(), any(), any())).thenReturn(vPage);
        List<Object[]> lastLoginList = new ArrayList<>();
        lastLoginList.add(new Object[]{"V1", new Date()});
        when(userDao.findLastLoginByOrgIds(any())).thenReturn(lastLoginList);

        SimplePageResponse<VendorSummaryResponse> r3 = service.getVendorSummary(0, 10, null, null);
        assertEquals(1, r3.getData().size());
    }

    @Test
    void testGenerateOtp_Branches() {
        assertFalse(service.generateOtp(null, null));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertFalse(service.generateOtp(org, null));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());
        assertTrue(service.generateOtp(org, null));
    }

    @Test
    void testValidateEmailOtp_Branches() throws Exception {
        assertFalse(service.validateEmailOtp(null));

        Organization invalidOrg = new Organization();
        invalidOrg.setEmail("test@org.com");
        invalidOrg.setOrganizationPhonenumber("9876543210");
        invalidOrg.setEmailOtp(null);
        assertFalse(service.validateEmailOtp(invalidOrg));

        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());
        assertFalse(service.validateEmailOtp(org));

        OtpStore expiredRecord = new OtpStore("key", "123456", LocalDateTime.now().minusMinutes(5));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(expiredRecord));
        assertFalse(service.validateEmailOtp(org));

        OtpStore validRecord = new OtpStore("key", "123456", LocalDateTime.now().plusMinutes(5));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(validRecord));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(userDao.updateActivityTs(anyString(), anyString(), anyString())).thenReturn(1);

        assertTrue(service.validateEmailOtp(org));

        // Mismatch OTP branch
        Organization mismatchOrg = new Organization();
        mismatchOrg.setEmail("test@org.com");
        mismatchOrg.setOrganizationPhonenumber("9876543210");
        mismatchOrg.setEmailOtp("999999");
        assertFalse(service.validateEmailOtp(mismatchOrg));

        // User is null branch
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertFalse(service.validateEmailOtp(org));
    }

    @Test
    void testGetSellerByEmail() {
        Role vRole = new Role();
        vRole.setRoleName("ROLE_VENDOR");
        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(vRole);
        when(userDao.findByUsernameAndPhoneAndActiveAndRole(anyString(), anyString(), any())).thenReturn(user);

        Organization res = service.getSellerByEmail(user);
        assertEquals(org, res);
    }

    @Test
    void testGetVendorSummarySearchResults() {
        OrgType vType = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(vType);

        org.setSubscriptionPlan(new SubscriptionPlan());
        org.setRfqUsedCount(5L);
        org.setQuoteSubmitted(3L);
        org.setSubscriptionExpiry(new Date());

        when(orgDao.findVendorsBySearchType(anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(org));
        List<VendorSummaryResponse> res = service.getVendorSummarySearchResults("type", "val");
        assertNotNull(res);
        assertEquals(1, res.size());
    }

    @Test
    void testUpdateEmailUserPswd_And_DisableUser_And_GenerateOtpExistingStore() throws Exception {
        EmailUser eu = new EmailUser();
        eu.setEmail("eu@test.com");
        eu.setPassword("newpass");
        when(emailUserRepo.findByEmail(anyString())).thenReturn(eu);
        assertTrue(service.updateEmailUserPswd(eu));

        when(userDao.findById(anyString())).thenReturn(Optional.of(user));
        when(userDao.findOrgIdByUser(anyString())).thenReturn("ORG1");
        when(userDao.findByOrg(anyString())).thenReturn(Collections.singletonList("admin@test.com"));
        assertTrue(service.disableUser(user));

        OtpStore existingStore = new OtpStore("key", "111111", LocalDateTime.now().plusMinutes(10));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(existingStore));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        HttpServletRequest req = mock(HttpServletRequest.class);
        assertTrue(service.generateOtp(org, req));

        assertFalse(service.validateOtp(null));
        assertFalse(service.validateOtp(new Organization()));
    }

    @Test
    void testSaveUserActivity() {
        UserActivity act = new UserActivity();
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(roleDao.findById("ROLE1")).thenReturn(Optional.of(role));
        when(userActivityDao.save(any())).thenReturn(act);

        UserActivityDto dto = new UserActivityDto();
        dto.setGmtOrBfs("BFS");
        dto.setLoginTime(LocalDateTime.now());

        UserActivity res = service.saveUserActivity(dto, "user1@test.com", "9876543210");
        assertNotNull(res);
    }

    @Test
    void testUpdateOrganization_Full() {
        assertFalse(service.updateOrganization(null));

        OrgBranches branch = new OrgBranches();
        OrgDivisionCategory divCat = new OrgDivisionCategory();
        org.setBranches(new ArrayList<>(Collections.singletonList(branch)));
        org.setDivisionCategories(new ArrayList<>(Collections.singletonList(divCat)));
        org.setSubscriptionPlan(new SubscriptionPlan());

        when(orgDao.findById("ORG1")).thenReturn(Optional.of(org));
        assertTrue(service.updateOrganization(org));
    }

    @Test
    void testDeactivateOrgUser_Success_And_AppException() {
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertThrows(AppException.class, () -> service.deactivateOrgUser(user));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(userDao.findOrgIdByUser(anyString())).thenReturn("ORG1");
        when(userDao.findByOrg("ORG1")).thenReturn(Collections.singletonList("email1@test.com"));
        assertTrue(service.deactivateOrgUser(user));

        when(userDao.findByOrg("ORG1")).thenReturn(Collections.emptyList());
        assertTrue(service.deactivateOrgUser(user));
    }

    @Test
    void testNormalizePhone_And_GetSellerByEmail_Nulls() {
        assertNull(service.getSellerByEmail(null));

        User nullUser = new User();
        assertNull(service.getSellerByEmail(nullUser));

        nullUser.setUsername("user@test.com");
        assertNull(service.getSellerByEmail(nullUser));

        User inputUser = new User();
        inputUser.setUsername("user@test.com");
        inputUser.setPhone("09876543210"); // leading 0
        Role vRole = new Role();
        vRole.setRoleName("ROLE_VENDOR");
        when(roleDao.findByRoleNameAndActive(anyString(), eq(true))).thenReturn(vRole);
        when(userDao.findByUsernameAndPhoneAndActiveAndRole(eq("user@test.com"), eq("+919876543210"), any())).thenReturn(user);
        assertNotNull(service.getSellerByEmail(inputUser));

        inputUser.setPhone("919876543210"); // starts with 91
        when(userDao.findByUsernameAndPhoneAndActiveAndRole(eq("user@test.com"), eq("+919876543210"), any())).thenReturn(null);
        assertNull(service.getSellerByEmail(inputUser));

        inputUser.setPhone("+14155552671"); // other format
        when(userDao.findByUsernameAndPhoneAndActiveAndRole(eq("user@test.com"), eq("+14155552671"), any())).thenReturn(null);
        assertNull(service.getSellerByEmail(inputUser));
    }

    @Test
    void testSaveUserActivity_GMT() {
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(roleDao.findById("ROLE1")).thenReturn(Optional.of(role));

        Rfq mockRfq = new Rfq();
        mockRfq.setCreatedTS(new Date());
        when(rfqDao.findByRfqId("RFQ123")).thenReturn(mockRfq);

        UserActivity act = new UserActivity();
        when(userActivityDao.save(any())).thenReturn(act);

        UserActivityDto dto = new UserActivityDto();
        dto.setGmtOrBfs("GMT");
        dto.setOperationType("CREATE");
        dto.setOperationSubType("RFQ123");
        dto.setLoginTime(LocalDateTime.now());

        UserActivity res = service.saveUserActivity(dto, "test@org.com", "9876543210");
        assertNotNull(res);
    }

    @Test
    void testGetVendorSummarySearchResults_NullOrgType_And_EmptyVendors() {
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(null);
        assertTrue(service.getVendorSummarySearchResults("type", "val").isEmpty());

        OrgType vType = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(vType);
        when(orgDao.findVendorsBySearchType(anyString(), anyString(), anyString())).thenReturn(Collections.emptyList());
        assertTrue(service.getVendorSummarySearchResults("type", "val").isEmpty());
    }

    @Test
    void getUserByEmail_mapsPermissionsAndCoversAbsentAuthAndRoleBranches() {
        Permission named = new Permission();
        named.setPermissionName("READ");
        Permission blank = new Permission();
        blank.setPermissionName("");
        role.setPermission(Arrays.asList(named, blank));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(emailUserRepo.findByEmail(anyString())).thenReturn(null);
        when(rfqDao.findRfqCountByUser(anyString())).thenReturn(0);

        User result = service.getUserByEmail(user);
        assertFalse(result.isAuth());
        assertEquals(Collections.singletonList("READ"), result.getListofPermission());

        user.setRole(null);
        assertNotNull(service.getUserByEmail(user));
        role.setPermission(Collections.emptyList());
        user.setRole(role);
        assertNotNull(service.getUserByEmail(user));
    }

    @Test
    void validateOtp_coversMissingExpiredAndMismatchEntries() throws Exception {
        Field mapField = ProcUserServiceImpl.class.getDeclaredField("otpsMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, OtpDetails> map = (Map<String, OtpDetails>) mapField.get(service);
        String key = org.getEmail() + "|" + org.getOrganizationPhonenumber();

        assertFalse(service.validateOtp(org));
        map.put(key, new OtpDetails("123456", LocalDateTime.now().minusMinutes(1)));
        assertFalse(service.validateOtp(org));
        map.put(key, new OtpDetails("999999", LocalDateTime.now().plusMinutes(1)));
        assertFalse(service.validateOtp(org));
    }

    @Test
    void validateEmailOtp_coversVerifiedSelfClientAndUpdateFailure() throws Exception {
        OtpStore valid = new OtpStore("key", "123456", LocalDateTime.now().plusMinutes(5));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(valid));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        user.setVerificationStatus(StatusConstants.EMAIL_VERIFIED);
        when(userDao.updateActivityTs(anyString(), anyString(), anyString())).thenReturn(0);
        assertFalse(service.validateEmailOtp(org));

        user.setVerificationStatus("PENDING");
        user.setSelfClient(true);
        when(userDao.updateActivityTs(anyString(), anyString(), anyString())).thenReturn(1);
        assertTrue(service.validateEmailOtp(org));
    }

    @Test
    void updateOrganization_coversNotFoundAndAllNullOptionalFields() {
        Organization missing = new Organization();
        missing.setId("missing");
        when(orgDao.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateOrganization(missing));

        Organization input = new Organization();
        input.setId("ORG1");
        Organization existing = new Organization();
        existing.setBranches(new ArrayList<>());
        existing.setDivisionCategories(new ArrayList<>());
        when(orgDao.findById("ORG1")).thenReturn(Optional.of(existing));
        assertTrue(service.updateOrganization(input));
    }

    @Test
    void updateBuyer_coversAllFieldsCollectionsAndNotFound() {
        Organization missing = new Organization();
        missing.setId("missing");
        when(orgDao.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateBuyer(missing));

        Organization input = new Organization();
        input.setId("ORG1");
        input.setCompanyName("Company");
        input.setDetails("details");
        input.setGstin("gst");
        input.setAddress1("address");
        input.setState("state");
        input.setCity("city");
        input.setZipCode("zip");
        input.setContactPerson("person");
        input.setEmail("mail@test.com");
        input.setOrganizationPhonenumber("9876543210");
        input.setUserId("USER1");
        input.setDivisionCategories(Collections.emptyList());
        Organization existing = new Organization();
        existing.setDivisionCategories(new ArrayList<>());
        when(orgDao.findById("ORG1")).thenReturn(Optional.of(existing));
        assertTrue(service.updateBuyer(input));

        OrgDivisionCategory category = new OrgDivisionCategory();
        category.setDivision("division");
        category.setCategory("category");
        input.setDivisionCategories(Collections.singletonList(category));
        assertTrue(service.updateBuyer(input));
        assertEquals(1, existing.getDivisionCategories().size());
    }

    @Test
    void summaryMethods_coverDefaultsAndPerVendorException() {
        OrgType type = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(type);
        Organization defaults = new Organization();
        defaults.setId("DEFAULT");
        Organization broken = mock(Organization.class);
        when(broken.getId()).thenReturn("BROKEN");
        when(broken.getEmail()).thenThrow(new IllegalStateException("bad vendor"));
        List<Organization> vendors = Arrays.asList(defaults, broken);
        when(userDao.findLastLoginByOrgIds(any())).thenReturn(Collections.emptyList());
        when(orgDao.findVendors(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(vendors));
        when(orgDao.findVendorsBySearchType(anyString(), anyString(), anyString())).thenReturn(vendors);

        SimplePageResponse<VendorSummaryResponse> page = service.getVendorSummary(0, 10, null, null);
        assertEquals(2, page.getData().size());
        assertEquals("Unknown", page.getData().get(1).getSubscribed());
        List<VendorSummaryResponse> search = service.getVendorSummarySearchResults("name", "x");
        assertEquals(2, search.size());
        assertEquals("Unknown", search.get(1).getSubscribed());
    }

    @Test
    void nullAndFailurePaths_coverSaveEmailForgotPasswordAndGenerateOtp() {
        assertFalse(service.saveEmailuser(null));
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(userDao.save(any())).thenThrow(new RuntimeException("save failed"));
        assertFalse(service.forgotPassword(user));

        reset(userDao);
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(otpStoreDao.findByOtpKey(anyString())).thenThrow(new RuntimeException("store failed"));
        assertFalse(service.generateOtp(org, null));
    }

    @Test
    void normalizePhone_coversNullBlankTenDigitTwelveDigitAndFallback() {
        assertNull(ReflectionTestUtils.invokeMethod(service, "normalizePhone", (String) null));
        assertEquals(" ", ReflectionTestUtils.invokeMethod(service, "normalizePhone", " "));
        assertEquals("+919876543210", ReflectionTestUtils.invokeMethod(service, "normalizePhone", "9876543210"));
        assertEquals("+919876543210", ReflectionTestUtils.invokeMethod(service, "normalizePhone", "919876543210"));
        assertEquals("+12345", ReflectionTestUtils.invokeMethod(service, "normalizePhone", "0012345"));
    }

    @Test
    void remainingFeasibleBranches_coverNullFieldsCountersLastLoginAndFallbackPhone() throws Exception {
        Organization missingPhone = new Organization();
        missingPhone.setEmail("test@org.com");
        missingPhone.setEmailOtp("123456");
        assertFalse(service.validateOtp(missingPhone));
        assertFalse(service.validateEmailOtp(missingPhone));

        Organization missingOtp = new Organization();
        missingOtp.setEmail("test@org.com");
        missingOtp.setOrganizationPhonenumber("9876543210");
        assertFalse(service.validateOtp(missingOtp));

        Organization buyerInput = new Organization();
        buyerInput.setId("BRANCHES");
        Organization buyerExisting = new Organization();
        buyerExisting.setDivisionCategories(new ArrayList<>());
        when(orgDao.findById("BRANCHES")).thenReturn(Optional.of(buyerExisting));
        assertTrue(service.updateBuyer(buyerInput));

        OrgType vendorType = new OrgType();
        Organization nullCounters = new Organization();
        nullCounters.setId("NULL_COUNTERS");
        nullCounters.setRfqUsedCount(null);
        nullCounters.setQuoteSubmitted(null);
        List<Organization> vendors = Collections.singletonList(nullCounters);
        when(orgTypeDao.findByTypeName(ApplicationConstants.VENDOR)).thenReturn(vendorType);
        when(orgDao.findVendors(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(vendors));
        when(orgDao.findVendorsBySearchType(anyString(), anyString(), anyString())).thenReturn(vendors);
        when(userDao.findLastLoginByOrgIds(any()))
                .thenReturn(Collections.singletonList(new Object[]{"NULL_COUNTERS", new Date()}));

        assertEquals(1, service.getVendorSummary(0, 10, null, null).getData().size());
        assertEquals(1, service.getVendorSummarySearchResults("name", "x").size());
        assertEquals("+121234567890",
                ReflectionTestUtils.invokeMethod(service, "normalizePhone", "121234567890"));
    }

    @Test
    void testGetBuyerByEmailString_And_DeactivateOrgUser_And_UserActivityGMT() {
        // getBuyerByEmail(String) null or empty
        assertEquals(400, service.getBuyerByEmail((String) null).getStatusCode().value());
        assertEquals(400, service.getBuyerByEmail("   ").getStatusCode().value());

        // getBuyerByEmail(String) not found
        Role initRole = new Role();
        initRole.setId("INIT_ROLE");
        when(roleDao.findByRoleNameAndActive(StatusConstants.ClientInitiator, true)).thenReturn(initRole);
        when(userDao.findFirstByUsernameAndRoleAndActiveTrueOrderByCreatedTSDesc(eq("buyer@test.com"), eq(initRole)))
                .thenReturn(Optional.empty());
        assertEquals(404, service.getBuyerByEmail("buyer@test.com").getStatusCode().value());

        // getBuyerByEmail(String) found
        when(userDao.findFirstByUsernameAndRoleAndActiveTrueOrderByCreatedTSDesc(eq("buyer@test.com"), eq(initRole)))
                .thenReturn(Optional.of(user));
        assertEquals(200, service.getBuyerByEmail("buyer@test.com").getStatusCode().value());

        // deactivateOrgUser
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(userDao.findOrgIdByUser("USER1")).thenReturn("ORG1");
        when(userDao.findByOrg("ORG1")).thenReturn(Collections.emptyList());
        assertTrue(service.deactivateOrgUser(user));

        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(null);
        assertThrows(AppException.class, () -> service.deactivateOrgUser(user));

        // userActivity with GMT
        UserActivityDto gmtDto = new UserActivityDto();
        gmtDto.setGmtOrBfs("GMT");
        gmtDto.setOperationSubType("RFQ-123");
        gmtDto.setLoginTime(LocalDateTime.now());

        Rfq rfq = new Rfq();
        rfq.setCreatedTS(new Date());
        when(userDao.findByUsernameAndPhoneAndActive(anyString(), anyString(), eq(true))).thenReturn(user);
        when(roleDao.findById(anyString())).thenReturn(Optional.of(role));
        when(rfqDao.findByRfqId("RFQ-123")).thenReturn(rfq);
        when(userActivityDao.save(any())).thenReturn(new UserActivity());

        assertNotNull(service.saveUserActivity(gmtDto, "user1@test.com", "9876543210"));

        // getBuyerUserByEmail & getBuyerByEmail(User)
        assertNull(service.getBuyerUserByEmail(null));
        assertNull(service.getBuyerByEmail((User) null));

        User emptyEmailUser = new User();
        assertNull(service.getBuyerUserByEmail(emptyEmailUser));

        User buyerInputUser = new User();
        buyerInputUser.setUsername("buyer.client@test.com");
        when(userDao.findActiveUsersByUsernameAndRoleNames(eq("buyer.client@test.com"), anyList()))
                .thenReturn(Collections.emptyList());
        assertNull(service.getBuyerUserByEmail(buyerInputUser));
        assertNull(service.getBuyerByEmail(buyerInputUser));

        User foundBuyer = new User();
        foundBuyer.setId("BUYER1");
        foundBuyer.setOrg(new Organization());
        when(userDao.findActiveUsersByUsernameAndRoleNames(eq("buyer.client@test.com"), anyList()))
                .thenReturn(List.of(foundBuyer));
        assertNotNull(service.getBuyerUserByEmail(buyerInputUser));
        assertNotNull(service.getBuyerByEmail(buyerInputUser));
    }
}


