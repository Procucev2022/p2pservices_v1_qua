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
        Organization org = new Organization();
        org.setId("503");
        user.setOrg(org);

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

    @Test
    @DisplayName("Test verifyAndGetBuyer when portalUser has null org or null org ID")
    void testVerifyAndGetBuyerNullOrgOrNullOrgId() {
        String email = "noorg@test.com";

        User userNoOrg = new User();
        userNoOrg.setId("601");
        userNoOrg.setOrg(null);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(userNoOrg));

        Buyer b1 = buyerVerificationService.verifyAndGetBuyer(email);
        assertFalse(b1.isVerified());

        User userNullOrgId = new User();
        userNullOrgId.setId("602");
        userNullOrgId.setOrg(new Organization());

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(userNullOrgId));

        Buyer b2 = buyerVerificationService.verifyAndGetBuyer(email);
        assertFalse(b2.isVerified());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer when org companyName is null and names are empty/null")
    void testVerifyAndGetBuyerNullCompanyNameAndEmptyNames() {
        String email = "nullcomp@test.com";

        Organization org = new Organization();
        org.setId("105");
        org.setCompanyName(null);
        org.setCity("City");
        org.setState("State");
        org.setZipCode("123456");

        User user = new User();
        user.setId("508");
        user.setFirstName("   ");
        user.setLastName("   ");
        user.setFullName(null);
        user.setOrg(org);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user));

        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        Mockito.when(buyerRepository.save(any(BuyerEntity.class))).thenAnswer(i -> {
            BuyerEntity e = i.getArgument(0);
            e.setId(8L);
            return e;
        });

        Buyer buyer = buyerVerificationService.verifyAndGetBuyer(email);
        assertTrue(buyer.isVerified());
        assertEquals("Portal Buyer", buyer.getCompanyName());
        assertEquals(email, buyer.getName());
    }

    @Test
    @DisplayName("Test verifyAndGetBuyer address combinations, name extraction, and verified buyer with address")
    void testAddressCombinationsAndNameExtraction() {
        // 1. a1 != null && a2 == null
        String email = "addr1@test.com";
        Organization org1 = new Organization();
        org1.setId("101");
        org1.setAddress1("Line 1 Only");
        org1.setAddress2(null);

        User user1 = new User();
        user1.setId("501");
        user1.setFullName("Full Name User");
        user1.setOrg(org1);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email), Mockito.anyList()))
                .thenReturn(List.of(user1));

        BuyerEntity existingVerified = BuyerEntity.builder()
                .id(10L)
                .email(email)
                .verified(true)
                .address("Line 1 Only")
                .build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingVerified));

        Buyer b1 = buyerVerificationService.verifyAndGetBuyer(email);
        assertTrue(b1.isVerified());
        assertEquals("Line 1 Only", b1.getAddress());
        assertEquals("Full Name User", b1.getName());

        // 2. a1 == null && a2 != null
        String email2 = "addr2@test.com";
        Organization org2 = new Organization();
        org2.setId("102");
        org2.setAddress1(null);
        org2.setAddress2("Line 2 Only");

        User user2 = new User();
        user2.setId("502");
        user2.setOrg(org2);

        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email2), Mockito.anyList()))
                .thenReturn(List.of(user2));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email2)).thenReturn(Optional.empty());
        Mockito.when(buyerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Buyer b2 = buyerVerificationService.verifyAndGetBuyer(email2);
        assertTrue(b2.isVerified());
        assertEquals("Line 2 Only", b2.getAddress());

        // 3. extractNameFromEmail
        assertEquals("Valued Buyer", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                buyerVerificationService, "extractNameFromEmail", (String) null));
        assertEquals("Valued Buyer", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                buyerVerificationService, "extractNameFromEmail", "no-at-sign-here"));

        // 4. delete throwing exception in unregistered branch
        String email3 = "unreg.ex@test.com";
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email3), Mockito.anyList()))
                .thenReturn(List.of());
        BuyerEntity bEntity = BuyerEntity.builder().id(99L).email(email3).build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email3)).thenReturn(Optional.of(bEntity));
        Mockito.doThrow(new RuntimeException("DB error")).when(buyerRepository).delete(bEntity);
        Buyer b3 = buyerVerificationService.verifyAndGetBuyer(email3);
        assertFalse(b3.isVerified());

        // 5. firstName only vs lastName only
        String email4 = "firstonly@test.com";
        Organization org4 = new Organization();
        org4.setId("104");
        User user4 = new User();
        user4.setId("504");
        user4.setFirstName("FirstName");
        user4.setLastName(null);
        user4.setOrg(org4);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email4), Mockito.anyList()))
                .thenReturn(List.of(user4));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email4)).thenReturn(Optional.empty());
        Buyer b4 = buyerVerificationService.verifyAndGetBuyer(email4);
        assertEquals("FirstName", b4.getName());

        String email5 = "lastonly@test.com";
        User user5 = new User();
        user5.setId("505");
        user5.setFirstName(null);
        user5.setLastName("LastName");
        user5.setOrg(org4);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email5), Mockito.anyList()))
                .thenReturn(List.of(user5));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email5)).thenReturn(Optional.empty());
        Buyer b5 = buyerVerificationService.verifyAndGetBuyer(email5);
        assertEquals("LastName", b5.getName());

        // 6. Null company name fallback to 'Portal Buyer', fullName fallback, and existing verified buyer with non-null address
        String email6 = "portalbuyer@test.com";
        Organization org6 = new Organization();
        org6.setId("106");
        org6.setCompanyName(null);
        User user6 = new User();
        user6.setId("506");
        user6.setFirstName(null);
        user6.setLastName(null);
        user6.setFullName("Full Name User");
        user6.setOrg(org6);

        BuyerEntity existingBuyer = BuyerEntity.builder()
                .id(60L)
                .email(email6)
                .verified(true)
                .address("Existing St")
                .build();
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email6), Mockito.anyList()))
                .thenReturn(List.of(user6));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email6)).thenReturn(Optional.of(existingBuyer));

        Buyer b6 = buyerVerificationService.verifyAndGetBuyer(email6);
        assertEquals("Portal Buyer", b6.getCompanyName());
        assertEquals("Full Name User", b6.getName());

        // 7. personName fallback to normalizedEmail when firstName, lastName, and fullName are null
        String email7 = "noname@test.com";
        Organization org7 = new Organization();
        org7.setId("107");
        User user7 = new User();
        user7.setId("507");
        user7.setOrg(org7);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email7), Mockito.anyList()))
                .thenReturn(List.of(user7));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email7))
                .thenReturn(Optional.empty());
        Mockito.when(buyerRepository.save(any(BuyerEntity.class)))
                .thenAnswer(inv -> {
                    BuyerEntity b = inv.getArgument(0);
                    b.setId(70L);
                    return b;
                });

        Buyer b7 = buyerVerificationService.verifyAndGetBuyer(email7);
        assertEquals(email7, b7.getName());

        // 8. existingBuyer with verified=false triggers save
        String email8 = "unverified-cached@test.com";
        Organization org8 = new Organization();
        org8.setId("108");
        User user8 = new User();
        user8.setId("508");
        user8.setOrg(org8);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email8), Mockito.anyList()))
                .thenReturn(List.of(user8));
        BuyerEntity unverifiedEntity = BuyerEntity.builder()
                .id(80L).email(email8).verified(false).address("Some St").build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email8))
                .thenReturn(Optional.of(unverifiedEntity));

        Buyer b8 = buyerVerificationService.verifyAndGetBuyer(email8);
        assertTrue(b8.isVerified());
        Mockito.verify(buyerRepository, Mockito.atLeastOnce()).save(unverifiedEntity);

        // 9. address1 only vs address2 only in org
        String email9 = "addr1only@test.com";
        Organization org9 = new Organization();
        org9.setId("109");
        org9.setAddress1("Building 5");
        org9.setAddress2(null);
        User user9 = new User();
        user9.setId("509");
        user9.setOrg(org9);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email9), Mockito.anyList()))
                .thenReturn(List.of(user9));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email9))
                .thenReturn(Optional.empty());
        Buyer b9 = buyerVerificationService.verifyAndGetBuyer(email9);
        assertEquals("Building 5", b9.getAddress());

        // 10. existingBuyer already verified and has address (no save needed)
        String email10 = "alreadyverified@test.com";
        Organization org10 = new Organization();
        org10.setId("110");
        User user10 = new User();
        user10.setId("510");
        user10.setOrg(org10);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(email10), Mockito.anyList()))
                .thenReturn(List.of(user10));
        BuyerEntity alreadyVerifiedEntity = BuyerEntity.builder()
                .id(90L).email(email10).verified(true).address("Existing Address").build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(email10))
                .thenReturn(Optional.of(alreadyVerifiedEntity));
        Buyer b10 = buyerVerificationService.verifyAndGetBuyer(email10);
        assertTrue(b10.isVerified());

        // 11. extractNameFromEmail branches
        assertEquals("Valued Buyer", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                buyerVerificationService, "extractNameFromEmail", (String) null));
        assertEquals("Valued Buyer", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                buyerVerificationService, "extractNameFromEmail", "justusername"));
        assertEquals("John Doe", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                buyerVerificationService, "extractNameFromEmail", "john..doe@domain.com"));

        // 12. portalUser with null org
        String emailNoOrg = "noorg@test.com";
        User userNoOrg = new User();
        userNoOrg.setId("599");
        userNoOrg.setOrg(null);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(emailNoOrg), Mockito.anyList()))
                .thenReturn(List.of(userNoOrg));
        Buyer bNoOrg = buyerVerificationService.verifyAndGetBuyer(emailNoOrg);
        assertFalse(bNoOrg.isVerified());

        // 13. portalUser == null with repository exception on cleanup
        String emailUnregEx = "unregistered-ex@test.com";
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(emailUnregEx), Mockito.anyList()))
                .thenReturn(List.of());
        Mockito.when(buyerRepository.findByEmailIgnoreCase(emailUnregEx))
                .thenThrow(new RuntimeException("DB cleanup error"));
        Buyer bUnregEx = buyerVerificationService.verifyAndGetBuyer(emailUnregEx);
        assertFalse(bUnregEx.isVerified());

        // 14. existingBuyer with verified=true but address=null
        String emailNullAddr = "nulladdr@test.com";
        Organization orgNullAddr = new Organization();
        orgNullAddr.setId("111");
        orgNullAddr.setAddress1("Suite 100");
        User userNullAddr = new User();
        userNullAddr.setId("511");
        userNullAddr.setOrg(orgNullAddr);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(emailNullAddr), Mockito.anyList()))
                .thenReturn(List.of(userNullAddr));
        BuyerEntity nullAddrEntity = BuyerEntity.builder()
                .id(91L).email(emailNullAddr).verified(true).address(null).build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(emailNullAddr))
                .thenReturn(Optional.of(nullAddrEntity));
        Buyer bNullAddr = buyerVerificationService.verifyAndGetBuyer(emailNullAddr);
        assertTrue(bNullAddr.isVerified());
        assertEquals("Suite 100", bNullAddr.getAddress());

        // 15. existingBuyer with verified=false, null company name, and full name fallback
        String emailUnver = "unver@test.com";
        Organization orgUnver = new Organization();
        orgUnver.setId("112");
        orgUnver.setCompanyName(null);
        orgUnver.setAddress1(null);
        orgUnver.setAddress2("Phase 2");
        User userUnver = new User();
        userUnver.setId("512");
        userUnver.setOrg(orgUnver);
        userUnver.setFirstName(null);
        userUnver.setLastName(null);
        userUnver.setFullName(null);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(emailUnver), Mockito.anyList()))
                .thenReturn(List.of(userUnver));
        BuyerEntity unverEntity = BuyerEntity.builder()
                .id(92L).email(emailUnver).verified(false).address("Old Addr").build();
        Mockito.when(buyerRepository.findByEmailIgnoreCase(emailUnver))
                .thenReturn(Optional.of(unverEntity));
        Buyer bUnver = buyerVerificationService.verifyAndGetBuyer(emailUnver);
        assertTrue(bUnver.isVerified());
        assertEquals("Phase 2", bUnver.getAddress());
        assertEquals("Portal Buyer", bUnver.getCompanyName());
        assertEquals(emailUnver, bUnver.getName());

        // 16. firstName only and address1 & address2 both null
        String emailFirstOnly = "firstonly@test.com";
        Organization orgFirstOnly = new Organization();
        orgFirstOnly.setId("113");
        orgFirstOnly.setAddress1(null);
        orgFirstOnly.setAddress2(null);
        User userFirstOnly = new User();
        userFirstOnly.setId("513");
        userFirstOnly.setOrg(orgFirstOnly);
        userFirstOnly.setFirstName("Charlie");
        userFirstOnly.setLastName(null);
        Mockito.when(userDao.findActiveUsersByUsernameAndRoleNames(eq(emailFirstOnly), Mockito.anyList()))
                .thenReturn(List.of(userFirstOnly));
        Mockito.when(buyerRepository.findByEmailIgnoreCase(emailFirstOnly))
                .thenReturn(Optional.empty());
        Buyer bFirstOnly = buyerVerificationService.verifyAndGetBuyer(emailFirstOnly);
        assertTrue(bFirstOnly.isVerified());
        assertEquals("Charlie", bFirstOnly.getName());
        assertNull(bFirstOnly.getAddress());
    }
}


