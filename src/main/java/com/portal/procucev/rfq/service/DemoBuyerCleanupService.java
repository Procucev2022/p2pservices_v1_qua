package com.portal.procucev.rfq.service;

import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.OtpStoreDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.entity.EmailTransaction;
import com.portal.procucev.rfq.repository.BuyerRepository;
import com.portal.procucev.rfq.repository.EmailTransactionRepository;
import com.portal.procucev.utils.StatusConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import com.portal.procucev.rfq.entity.RFQEntity;
import com.portal.procucev.rfq.repository.RFQRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to manage lifecycle expiration and automatic cleanup of unverified
 * email-registered demo buyers (3-hour validity window).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DemoBuyerCleanupService {

    private final UserDao userDao;
    private final ClientDao clientDao;
    private final OtpStoreDao otpStoreDao;
    private final BuyerRepository buyerRepository;
    private final EmailTransactionRepository emailTransactionRepository;
    private final RfqDao rfqDao;

    @Autowired(required = false)
    private RFQRepository rfqRepository;

    public void setRfqRepository(RFQRepository rfqRepository) {
        this.rfqRepository = rfqRepository;
    }

    public static final long EXPIRATION_DURATION_MS = 3 * 60 * 60 * 1000L; // 3 hours

    /**
     * Checks if the given demo buyer has exceeded the 3-hour active window without logging in.
     */
    public boolean isExpired(User user) {
        if (user == null) {
            return false;
        }
        boolean isEmailDemo = "EMAIL".equalsIgnoreCase(user.getSourceType())
                || StatusConstants.DEMO_BUYER.equalsIgnoreCase(user.getVerificationStatus());
        if (!isEmailDemo) {
            return false;
        }
        if (StatusConstants.PROFILE_COMPLETED.equalsIgnoreCase(user.getVerificationStatus())) {
            return false;
        }
        // If the user has logged in, activityTs will be non-null
        if (user.getActivityTs() != null) {
            return false;
        }
        if (user.getCreatedTS() == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - user.getCreatedTS().getTime();
        return elapsed > EXPIRATION_DURATION_MS;
    }

    /**
     * Cleanly deletes an unverified demo buyer and their temporary organization and associated draft records.
     * Only applies to email-registered demo buyers.
     */
    @Transactional
    public boolean deleteExpiredDemoBuyer(User user) {
        if (user == null) {
            return false;
        }
        boolean isEmailDemo = "EMAIL".equalsIgnoreCase(user.getSourceType())
                || StatusConstants.DEMO_BUYER.equalsIgnoreCase(user.getVerificationStatus());
        if (!isEmailDemo) {
            log.warn("Attempted to delete non-email demo buyer account {}. Aborting deletion.", user.getUsername());
            return false;
        }
        if (StatusConstants.PROFILE_COMPLETED.equalsIgnoreCase(user.getVerificationStatus())) {
            log.warn("Account {} is already fully verified (PROFILE_COMPLETED). Aborting deletion.", user.getUsername());
            return false;
        }

        String email = user.getUsername();
        String phone = user.getPhone();
        Organization org = user.getOrg();
        log.info("Starting deletion of expired email demo buyer: email={}, phone={}, orgId={}",
                email, phone, org != null ? org.getId() : null);

        // 1. Remove mobile and email OTPs
        try {
            if (email != null && phone != null) {
                otpStoreDao.deleteByOtpKey(email + "_MOBILE_" + phone);
                otpStoreDao.deleteByOtpKey(phone + "_EMAIL_" + email);
            }
        } catch (Exception e) {
            log.warn("Error cleaning up OTP store for {}: {}", email, e.getMessage());
        }

        // 2. Mark pending email transactions as EXPIRED_DELETED
        try {
            List<EmailTransaction> txs = emailTransactionRepository.findBySenderEmailIgnoreCase(email);
            for (EmailTransaction tx : txs) {
                if (StatusConstants.PENDING_BUYER_REGISTRATION.equalsIgnoreCase(tx.getStatus())) {
                    tx.setStatus("EXPIRED_DELETED");
                    tx.setErrorMessage("Account expired after 3 hours without login and was automatically removed.");
                    emailTransactionRepository.save(tx);
                }
            }
        } catch (Exception e) {
            log.warn("Error updating email transactions for {}: {}", email, e.getMessage());
        }

        // 3. Remove unverified buyer from rfq_buyers cache
        try {
            buyerRepository.findByEmailIgnoreCase(email).ifPresent(buyerRepository::delete);
        } catch (Exception e) {
            log.warn("Error deleting rfq_buyers entry for {}: {}", email, e.getMessage());
        }

        // 4. Remove unverified RFQ headers created by this user
        try {
            List<Rfq> rfqs = rfqDao.findNoPrRfqByClient(user.getId());
            if (rfqs != null && !rfqs.isEmpty()) {
                rfqDao.deleteAll(rfqs);
            }
        } catch (Exception e) {
            log.warn("Error deleting unverified RFQs for {}: {}", email, e.getMessage());
        }

        // 4b. Remove unverified RFQ entities in rfq_records
        try {
            if (rfqRepository != null) {
                List<RFQEntity> records = rfqRepository.findByBuyerEmail(email);
                if (records != null && !records.isEmpty()) {
                    rfqRepository.deleteAll(records);
                }
            }
        } catch (Exception e) {
            log.warn("Error deleting rfq_records for {}: {}", email, e.getMessage());
        }

        // 5. Delete User entity
        try {
            userDao.delete(user);
            log.info("Deleted expired User entity: {}", email);
        } catch (Exception e) {
            log.error("Failed to delete expired User {}: {}", email, e.getMessage());
            return false;
        }

        // 6. Delete Organization entity if no other users are attached
        try {
            if (org != null) {
                List<User> remainingUsers = userDao.findByOrg(org);
                if (remainingUsers == null || remainingUsers.isEmpty()
                        || (remainingUsers.size() == 1 && remainingUsers.get(0).getId().equals(user.getId()))) {
                    clientDao.delete(org);
                    log.info("Deleted expired Organization entity: id={}, name={}", org.getId(), org.getCompanyName());
                }
            }
        } catch (Exception e) {
            log.warn("Error deleting expired Organization entity for {}: {}", email, e.getMessage());
        }

        log.info("Successfully completed auto-deletion of expired email demo buyer: {}", email);
        return true;
    }

    /**
     * Scans and cleans up all unverified demo buyers who have exceeded 1 hour without logging in.
     */
    @Transactional
    public int cleanupExpiredDemoBuyers() {
        List<User> demoUsers = userDao.findBySourceTypeAndVerificationStatus("EMAIL", StatusConstants.DEMO_BUYER);
        if (demoUsers == null || demoUsers.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (User user : demoUsers) {
            if (isExpired(user)) {
                log.info("Found expired email demo buyer {}: createdTS={}, activityTs={}",
                        user.getUsername(), user.getCreatedTS(), user.getActivityTs());
                boolean deleted = deleteExpiredDemoBuyer(user);
                if (deleted) {
                    count++;
                }
            }
        }
        if (count > 0) {
            log.info("Automatic demo buyer cleanup completed: {} account(s) deleted.", count);
        }
        return count;
    }
}
