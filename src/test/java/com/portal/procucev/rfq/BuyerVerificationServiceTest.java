package com.portal.procucev.rfq;

import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.entity.BuyerEntity;
import com.portal.procucev.rfq.model.Buyer;
import com.portal.procucev.rfq.repository.BuyerRepository;
import com.portal.procucev.rfq.service.BuyerVerificationService;
import com.portal.procucev.utils.StatusConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class BuyerVerificationServiceTest {

    private BuyerRepository buyerRepository;
    private UserDao userDao;
    private BuyerVerificationService buyerVerificationService;

    @BeforeEach
    void setUp() {
        buyerRepository = Mockito.mock(BuyerRepository.class);
        userDao = Mockito.mock(UserDao.class);
        buyerVerificationService = new BuyerVerificationService(buyerRepository, userDao);
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer with null or blank email")
    void testVerifyAndGetBuyerNullOrBlank() {
        Buyer b1 = buyerVerificationService.verifyAndGetBuyer(null);
        assertNull(b1.getEmail());
        assertFalse(b1.isVerified());

        Buyer b2 = buyerVerificationService.verifyAndGetBuyer("   ");
        assertEquals("   ", b2.getEmail());
        assertFalse(b2.isVerified());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer when email is not registered in UserDao")
    void testVerifyAndGetBuyerUnregistered() {
        String email = "unregistered.test@test.com";
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of());

        BuyerEntity staleBuyer = BuyerEntity.builder().id(10L).email(email).build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(staleBuyer));

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);

        assertFalse(buyer.isVerified());
        assertEquals(email, buyer.getEmail());
        assertEquals("Unregistered Test", buyer.getName());
        Mockito.verify(buyerRepository).delete(staleBuyer);
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer when registered user has full organization details (New BuyerEntity)")
    void testVerifyAndGetBuyerRegisteredWithOrgNewEntity() {
        String email = "buyer@test.com";

        Organization org = new Organization();
        org.setId("101");
        org.setCompanyName("Acme Corp");
        org.setCity("Bangalore");
        org.setState("Karnataka");
        org.setZipCode("560001");
        org.setAddress1("123 Main St");
        org.setAddress2("Suite 4");

        User user = new User();
        user.setId("501");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("9876543210");
        user.setOrg(org);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user));

        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        Mockito.when(buyerRepository.save(any(BuyerEntity.class))).thenAnswer(invocation -> {
            BuyerEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);

        assertTrue(buyer.isVerified());
        assertEquals(1L, buyer.getId());
        assertEquals(email, buyer.getEmail());
        assertEquals("John Doe", buyer.getName());
        assertEquals("John Doe", buyer.getContactPerson());
        assertEquals("Acme Corp", buyer.getCompanyName());
        assertEquals("101", buyer.getOrgId());
        assertEquals("501", buyer.getUserId());
        assertEquals("Bangalore", buyer.getCity());
        assertEquals("Karnataka", buyer.getState());
        assertEquals("560001", buyer.getPincode());
        assertEquals("123 Main St Suite 4", buyer.getAddress());
        assertEquals("9876543210", buyer.getPhone());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer when registered user has no org and uses fullName fallback (Existing unverified BuyerEntity)")
    void testVerifyAndGetBuyerRegisteredNoOrgExistingEntity() {
        String email = "buyer2@test.com";

        User user = new User();
        user.setId("502");
        user.setFullName("Alice Smith");
        user.setPhone("1112223333");
        Organization org = new Organization();
        org.setId("1");
        user.setOrg(org);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user));

        BuyerEntity existingEntity = BuyerEntity.builder()
                .id(2L)
                .email(email)
                .verified(false)
                .address(null)
                .build();

        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingEntity));
        Mockito.when(buyerRepository.save(any(BuyerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);

        assertTrue(buyer.isVerified());
        assertEquals(2L, buyer.getId());
        assertEquals("Alice Smith", buyer.getName());
        assertEquals("Portal Buyer", buyer.getCompanyName());
        assertEquals("1", buyer.getOrgId());
        assertEquals("502", buyer.getUserId());
        assertNull(buyer.getAddress());
    }

    @Test
    @DisplayName("Test extractNameFromEmail helper variations")
    void testExtractNameFromEmail() {
        String email = "john.doe_smith@test.com";
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of());

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertEquals("John Doe Smith", buyer.getName());

        Buyer invalidEmailBuyer = buyerVerificationService.verifyAndGetBuyer("invalidemail");
        assertEquals("Valued Buyer", invalidEmailBuyer.getName());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer with user having empty names and null fullName")
    void testVerifyAndGetBuyerEmptyNames() {
        String email = "empty.names@test.com";

        User user = new User();
        user.setId("503");
        user.setFirstName(null);
        user.setLastName(null);
        user.setFullName(null);
        user.setOrg(null);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        Mockito.when(buyerRepository.save(any(BuyerEntity.class))).thenAnswer(i -> {
            BuyerEntity e = i.getArgument(0);
            e.setId(3L);
            return e;
        });

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertTrue(buyer.isVerified());
        assertEquals(email, buyer.getName());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer with already verified buyer with existing address (no update)")
    void testVerifyAndGetBuyerAlreadyVerifiedWithAddress() {
        String email = "verified@test.com";

        Organization org = new Organization();
        org.setId("101");
        org.setCompanyName("Corp");

        User user = new User();
        user.setId("504");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setOrg(org);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user));

        BuyerEntity existingEntity = BuyerEntity.builder()
                .id(5L).email(email).verified(true).address("Existing Address").build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingEntity));

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertTrue(buyer.isVerified());
        assertEquals(5L, buyer.getId());
        // save should NOT have been called because entity is already verified and has address
        Mockito.verify(buyerRepository, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer when delete stale buyer throws exception (gracefully ignored)")
    void testVerifyAndGetBuyerDeleteStaleException() {
        String email = "stale-exception@test.com";
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of());
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email))
                .thenThrow(new RuntimeException("DB Error"));

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertFalse(buyer.isVerified());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer with org address1/address2 null")
    void testVerifyAndGetBuyerOrgAddressNull() {
        String email = "nulladdr@test.com";

        Organization org = new Organization();
        org.setId("102");
        org.setCompanyName("Corp2");
        org.setAddress1(null);
        org.setAddress2(null);

        User user = new User();
        user.setId("505");
        user.setFirstName("Bob");
        user.setLastName("");
        user.setOrg(org);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        Mockito.when(buyerRepository.save(any(BuyerEntity.class))).thenAnswer(i -> {
            BuyerEntity e = i.getArgument(0);
            e.setId(6L);
            return e;
        });

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertTrue(buyer.isVerified());
        assertNull(buyer.getAddress());
    }

    @Test
    @DisplayName("Test extractNameFromEmail with @-only email")
    void testExtractNameFromEmailAtOnly() {
        String email = "@test.com";
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of());

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertEquals("Valued Buyer", buyer.getName());
    }

    @Test
    @DisplayName("Test extractNameFromEmail with email without @ sign")
    void testExtractNameFromEmailNoAt() {
        Buyer buyer = buyerVerificationService.verifyAndGetBuyer("noatsign");
        assertEquals("Valued Buyer", buyer.getName());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer when buyerEntity is verified but address is null (updates address)")
    void testVerifyAndGetBuyerVerifiedNullAddress() {
        String email = "nulladdr.verified@test.com";

        Organization org = new Organization();
        org.setId("103");
        org.setCompanyName("Corp3");
        org.setAddress1("123 Street");

        User user = new User();
        user.setId("506");
        user.setFirstName("Verified");
        user.setLastName("User");
        user.setOrg(org);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user));

        BuyerEntity existingEntity = BuyerEntity.builder()
                .id(7L)
                .email(email)
                .verified(true)
                .address(null)
                .build();

        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingEntity));
        Mockito.when(buyerRepository.save(any(BuyerEntity.class))).thenAnswer(i -> i.getArgument(0));

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertTrue(buyer.isVerified());
        assertEquals("123 Street", buyer.getAddress());
        Mockito.verify(buyerRepository).save(existingEntity);
    }
}


