package com.portal.procucev.rfq;

import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.OtpStoreDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.repository.BuyerRepository;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.rfq.repository.RFQRepository;
import com.portal.procucev.rfq.service.DemoBuyerCleanupService;
import com.portal.procucev.utils.StatusConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoBuyerCleanupServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private ClientDao clientDao;

    @Mock
    private OtpStoreDao otpStoreDao;

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private EmailTransactionRepository emailTransactionRepository;

    @Mock
    private RfqDao rfqDao;

    @Mock
    private RFQRepository rfqRepository;

    @InjectMocks
    private DemoBuyerCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService.setRfqRepository(rfqRepository);
    }

    @Test
    @DisplayName("isExpired: returns false for null user")
    void testIsExpired_NullUser() {
        assertFalse(cleanupService.isExpired(null));
    }

    @Test
    @DisplayName("isExpired: returns false for non-email demo buyer")
    void testIsExpired_NonEmailDemo() {
        User user = new User();
        user.setSourceType("WEB");
        user.setVerificationStatus("OTHER");
        assertFalse(cleanupService.isExpired(user));
    }

    @Test
    @DisplayName("isExpired: returns false when profile is completed")
    void testIsExpired_ProfileCompleted() {
        User user = new User();
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);
        assertFalse(cleanupService.isExpired(user));
    }

    @Test
    @DisplayName("isExpired: returns false when buyer has logged in (activityTs non-null)")
    void testIsExpired_ActiveLogin() {
        User user = new User();
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);
        user.setActivityTs(new Date());
        assertFalse(cleanupService.isExpired(user));
    }

    @Test
    @DisplayName("isExpired: returns false when createdTS is null")
    void testIsExpired_CreatedTsNull() {
        User user = new User();
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);
        user.setCreatedTS(null);
        assertFalse(cleanupService.isExpired(user));
    }

    @Test
    @DisplayName("isExpired: returns false if within 3 hours")
    void testIsExpired_Within3Hours() {
        User user = new User();
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);
        // Created 2 hours ago
        user.setCreatedTS(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000L));
        assertFalse(cleanupService.isExpired(user));
    }

    @Test
    @DisplayName("isExpired: returns true if exceeded 3 hours")
    void testIsExpired_Exceeded3Hours() {
        User user = new User();
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);
        // Created 3.5 hours ago
        user.setCreatedTS(new Date(System.currentTimeMillis() - (long) (3.5 * 60 * 60 * 1000L)));
        assertTrue(cleanupService.isExpired(user));
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: returns false for null user")
    void testDelete_NullUser() {
        assertFalse(cleanupService.deleteExpiredDemoBuyer(null));
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: returns false for non-email demo buyer")
    void testDelete_NonEmailDemo() {
        User user = new User();
        user.setUsername("regular@user.com");
        user.setSourceType("WEB");
        user.setVerificationStatus("REGISTERED");
        assertFalse(cleanupService.deleteExpiredDemoBuyer(user));
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: returns false if profile is already completed")
    void testDelete_ProfileCompleted() {
        User user = new User();
        user.setUsername("completed@user.com");
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.PROFILE_COMPLETED);
        assertFalse(cleanupService.deleteExpiredDemoBuyer(user));
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: cleanly deletes expired buyer, OTPs, RFQs, transactions, org")
    void testDelete_SuccessFullCleanup() {
        User user = new User();
        user.setId("U100");
        user.setUsername("demo@corp.com");
        user.setPhone("9876543210");
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);

        Organization org = new Organization();
        org.setId("ORG100");
        org.setCompanyName("Demo Corp");
        user.setOrg(org);

        EmailTransaction tx = EmailTransaction.builder()
                .id(1L)
                .senderEmail("demo@corp.com")
                .status(StatusConstants.PENDING_BUYER_REGISTRATION)
                .build();
        when(emailTransactionRepository.findBySenderEmailIgnoreCase("demo@corp.com"))
                .thenReturn(List.of(tx));

        when(buyerRepository.findByEmailIgnoreCase("demo@corp.com"))
                .thenReturn(Optional.empty());

        Rfq rfq = new Rfq();
        rfq.setRfqId("RFQ1");
        when(rfqDao.findNoPrRfqByClient("U100")).thenReturn(List.of(rfq));

        RFQEntity entity = new RFQEntity();
        entity.setRfqNumber("RFQ1");
        when(rfqRepository.findByBuyerEmail("demo@corp.com")).thenReturn(List.of(entity));

        when(userDao.findByOrg(org)).thenReturn(List.of(user));

        boolean result = cleanupService.deleteExpiredDemoBuyer(user);

        assertTrue(result);
        verify(otpStoreDao).deleteByOtpKey("demo@corp.com_MOBILE_9876543210");
        verify(otpStoreDao).deleteByOtpKey("9876543210_EMAIL_demo@corp.com");
        verify(emailTransactionRepository).save(argThat(savedTx ->
                "EXPIRED_DELETED".equals(savedTx.getStatus())
                && savedTx.getErrorMessage().contains("3 hours")
        ));
        verify(rfqDao).deleteAll(List.of(rfq));
        verify(rfqRepository).deleteAll(List.of(entity));
        verify(userDao).delete(user);
        verify(clientDao).delete(org);
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: does not delete org if other users remain in org")
    void testDelete_DoNotDeleteOrgWithMultipleUsers() {
        User user = new User();
        user.setId("U100");
        user.setUsername("demo@corp.com");
        user.setPhone("9876543210");
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);

        User otherUser = new User();
        otherUser.setId("U200");

        Organization org = new Organization();
        org.setId("ORG100");
        user.setOrg(org);

        when(emailTransactionRepository.findBySenderEmailIgnoreCase("demo@corp.com"))
                .thenReturn(Collections.emptyList());
        when(buyerRepository.findByEmailIgnoreCase("demo@corp.com"))
                .thenReturn(Optional.empty());
        when(rfqDao.findNoPrRfqByClient("U100")).thenReturn(Collections.emptyList());
        when(rfqRepository.findByBuyerEmail("demo@corp.com")).thenReturn(Collections.emptyList());
        when(userDao.findByOrg(org)).thenReturn(List.of(user, otherUser));

        boolean result = cleanupService.deleteExpiredDemoBuyer(user);

        assertTrue(result);
        verify(userDao).delete(user);
        verify(clientDao, never()).delete(any());
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: returns false when userDao.delete throws exception")
    void testDelete_UserDaoThrows() {
        User user = new User();
        user.setId("U100");
        user.setUsername("demo@corp.com");
        user.setSourceType("EMAIL");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);

        doThrow(new RuntimeException("DB error")).when(userDao).delete(user);

        boolean result = cleanupService.deleteExpiredDemoBuyer(user);

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: continues gracefully when individual cleanup steps throw exceptions")
    void testDelete_ExceptionInCleanupSteps() {
        User user = new User();
        user.setId("U100");
        user.setUsername("demo@corp.com");
        user.setPhone("9876543210");
        user.setVerificationStatus(StatusConstants.DEMO_BUYER);

        Organization org = new Organization();
        org.setId("ORG100");
        user.setOrg(org);

        doThrow(new RuntimeException("OTP error")).when(otpStoreDao).deleteByOtpKey(anyString());
        when(emailTransactionRepository.findBySenderEmailIgnoreCase("demo@corp.com"))
                .thenThrow(new RuntimeException("Tx error"));
        when(buyerRepository.findByEmailIgnoreCase("demo@corp.com"))
                .thenThrow(new RuntimeException("Buyer error"));
        when(rfqDao.findNoPrRfqByClient("U100")).thenThrow(new RuntimeException("RFQ error"));
        when(rfqRepository.findByBuyerEmail("demo@corp.com")).thenThrow(new RuntimeException("RFQ repo error"));
        when(userDao.findByOrg(org)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Org error")).when(clientDao).delete(org);

        boolean result = cleanupService.deleteExpiredDemoBuyer(user);

        assertTrue(result);
        verify(userDao).delete(user);
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: handles null phone, null email, and null rfqRepository")
    void testDelete_NullFieldsAndNullRepo() {
        cleanupService.setRfqRepository(null);

        User user = new User();
        user.setId("U100");
        user.setSourceType("EMAIL");
        user.setVerificationStatus("OTHER");

        when(emailTransactionRepository.findBySenderEmailIgnoreCase(null))
                .thenReturn(Collections.emptyList());
        when(rfqDao.findNoPrRfqByClient("U100")).thenReturn(null);

        boolean result = cleanupService.deleteExpiredDemoBuyer(user);

        assertTrue(result);
        verifyNoInteractions(otpStoreDao);
        verify(userDao).delete(user);
    }

    @Test
    @DisplayName("deleteExpiredDemoBuyer: deletes org when remainingUsers is null")
    void testDelete_RemainingUsersNull() {
        User user = new User();
        user.setId("U100");
        user.setUsername("demo@corp.com");
        user.setSourceType("EMAIL");

        Organization org = new Organization();
        org.setId("ORG100");
        user.setOrg(org);

        when(userDao.findByOrg(org)).thenReturn(null);

        boolean result = cleanupService.deleteExpiredDemoBuyer(user);

        assertTrue(result);
        verify(clientDao).delete(org);
    }

    @Test
    @DisplayName("cleanupExpiredDemoBuyers: handles failed deletions gracefully")
    void testCleanupExpiredDemoBuyers_DeletionFails() {
        User expiredUser = new User();
        expiredUser.setId("U2");
        expiredUser.setUsername("expired@corp.com");
        expiredUser.setSourceType("EMAIL");
        expiredUser.setVerificationStatus(StatusConstants.DEMO_BUYER);
        expiredUser.setCreatedTS(new Date(System.currentTimeMillis() - 4 * 60 * 60 * 1000L));

        when(userDao.findBySourceTypeAndVerificationStatus("EMAIL", StatusConstants.DEMO_BUYER))
                .thenReturn(List.of(expiredUser));
        doThrow(new RuntimeException("DB delete fail")).when(userDao).delete(expiredUser);

        int count = cleanupService.cleanupExpiredDemoBuyers();

        assertEquals(0, count);
    }

    @Test
    @DisplayName("cleanupExpiredDemoBuyers: scans and cleans up only expired buyers")
    void testCleanupExpiredDemoBuyers() {
        User validUser = new User();
        validUser.setId("U1");
        validUser.setUsername("valid@corp.com");
        validUser.setSourceType("EMAIL");
        validUser.setVerificationStatus(StatusConstants.DEMO_BUYER);
        validUser.setCreatedTS(new Date(System.currentTimeMillis() - 1000L)); // fresh

        User expiredUser = new User();
        expiredUser.setId("U2");
        expiredUser.setUsername("expired@corp.com");
        expiredUser.setSourceType("EMAIL");
        expiredUser.setVerificationStatus(StatusConstants.DEMO_BUYER);
        expiredUser.setCreatedTS(new Date(System.currentTimeMillis() - 4 * 60 * 60 * 1000L)); // 4 hrs ago

        when(userDao.findBySourceTypeAndVerificationStatus("EMAIL", StatusConstants.DEMO_BUYER))
                .thenReturn(List.of(validUser, expiredUser));

        int count = cleanupService.cleanupExpiredDemoBuyers();

        assertEquals(1, count);
        verify(userDao, never()).delete(validUser);
        verify(userDao).delete(expiredUser);
    }

    @Test
    @DisplayName("cleanupExpiredDemoBuyers: returns 0 when no demo buyers found")
    void testCleanupExpiredDemoBuyers_EmptyList() {
        when(userDao.findBySourceTypeAndVerificationStatus("EMAIL", StatusConstants.DEMO_BUYER))
                .thenReturn(Collections.emptyList());

        int count = cleanupService.cleanupExpiredDemoBuyers();
        assertEquals(0, count);
    }
}
