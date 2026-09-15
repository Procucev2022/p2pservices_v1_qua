package com.portal.procucev.rfq;

import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.RoleDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.service.DemoBuyerRegistrationService;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.StatusConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoBuyerRegistrationServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private RoleDao roleDao;

    @Mock
    private ClientDao clientDao;

    @Mock
    private OrgTypeDao orgTypeDao;

    @Mock
    private MasterStatusDao masterStatusDao;

    @Mock
    private SelfRegistrationService selfRegistrationService;

    @InjectMocks
    private DemoBuyerRegistrationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "demoPhoneNumber", "9999999991");
    }

    @Test
    @DisplayName("createDemoBuyer: creates new org and user when user does not exist")
    void testCreateDemoBuyer_Success() throws Exception {
        when(userDao.findByUsernameAndActive("newbuyer@example.com", true)).thenReturn(null);

        OrgType clientOrgType = new OrgType();
        clientOrgType.setTypeName(ApplicationConstants.CLIENT);
        when(orgTypeDao.findByTypeName(ApplicationConstants.CLIENT)).thenReturn(clientOrgType);

        when(selfRegistrationService.generateId(anyString())).thenReturn("ID-123");

        Organization savedOrg = new Organization();
        savedOrg.setId("ORG-99");
        savedOrg.setCompanyName("Example (Demo)");
        when(clientDao.save(any(Organization.class))).thenReturn(savedOrg);

        MasterStatus status = new MasterStatus();
        status.setStatus(StatusConstants.CLIENT_NEW);
        when(masterStatusDao.findByStatus(StatusConstants.CLIENT_NEW)).thenReturn(status);

        Role role = new Role();
        role.setRoleName(StatusConstants.ClientInitiator);
        when(roleDao.findByRoleNameAndActive(StatusConstants.ClientInitiator, true)).thenReturn(role);

        User savedUser = new User();
        savedUser.setId("USER-1");
        savedUser.setUsername("newbuyer@example.com");
        savedUser.setVerificationStatus(StatusConstants.DEMO_BUYER);
        savedUser.setActive(true);
        savedUser.setApproved(true);
        when(userDao.save(any(User.class))).thenReturn(savedUser);

        User result = service.createDemoBuyer("newbuyer@example.com", "John Doe");

        assertNotNull(result);
        assertEquals("USER-1", result.getId());
        assertEquals("newbuyer@example.com", result.getUsername());
        assertEquals(StatusConstants.DEMO_BUYER, result.getVerificationStatus());
        assertTrue(result.isActive());

        verify(clientDao).save(argThat(org ->
                "EMAIL".equals(org.getSourceType()) && org.isSelfClient()));
        verify(userDao).save(argThat(u ->
                StatusConstants.DEMO_BUYER.equals(u.getVerificationStatus()) && u.isApproved()));
    }

    @Test
    @DisplayName("createDemoBuyer: idempotent when user already exists")
    void testCreateDemoBuyer_AlreadyExists() {
        User existing = new User();
        existing.setId("USER-EXISTING");
        existing.setUsername("existing@example.com");
        existing.setVerificationStatus(StatusConstants.DEMO_BUYER);
        existing.setActive(true);
        existing.setPhone("9999999991");
        existing.setPassword("Welcome@123");

        when(userDao.findByUsernameAndActive("existing@example.com", true)).thenReturn(existing);

        User result = service.createDemoBuyer("existing@example.com", "Existing User");

        assertNotNull(result);
        assertEquals("USER-EXISTING", result.getId());
        verifyNoInteractions(clientDao);
    }

    @Test
    @DisplayName("isBuyerFullyVerified: checks verification status correctly")
    void testIsBuyerFullyVerified() {
        User nullUser = null;
        assertFalse(service.isBuyerFullyVerified(nullUser));

        User inactive = new User();
        inactive.setActive(false);
        inactive.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);
        assertFalse(service.isBuyerFullyVerified(inactive));

        User demo = new User();
        demo.setActive(true);
        demo.setVerificationStatus(StatusConstants.DEMO_BUYER);
        assertFalse(service.isBuyerFullyVerified(demo));

        User phoneVerified = new User();
        phoneVerified.setActive(true);
        phoneVerified.setVerificationStatus(StatusConstants.PHONE_VERIFIED);
        assertFalse(service.isBuyerFullyVerified(phoneVerified));

        User completed = new User();
        completed.setActive(true);
        completed.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);
        assertTrue(service.isBuyerFullyVerified(completed));

        User legacyVerified = new User();
        legacyVerified.setActive(true);
        legacyVerified.setVerificationStatus(StatusConstants.EMAIL_VERIFIED);
        assertTrue(service.isBuyerFullyVerified(legacyVerified));
    }

    @Test
    @DisplayName("getDemoPhoneNumber returns configured value")
    void testGetDemoPhoneNumber() {
        assertEquals("9999999991", service.getDemoPhoneNumber());
    }
}
