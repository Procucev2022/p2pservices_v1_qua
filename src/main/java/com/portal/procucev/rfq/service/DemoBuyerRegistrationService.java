package com.portal.procucev.rfq.service;

import com.portal.procucev.dao.ClientDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgTypeDao;
import com.portal.procucev.dao.RoleDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.Role;
import com.portal.procucev.model.User;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.utils.ApplicationConstants;
import com.portal.procucev.utils.PhoneNumberUtils;
import com.portal.procucev.utils.ProcucevUtils;
import com.portal.procucev.utils.StatusConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Creates a temporary/demo buyer account for unregistered email senders.
 *
 * <p>Reuses the existing registration infrastructure ({@link SelfRegistrationService},
 * {@link UserDao}, {@link ClientDao}, {@link RoleDao}, etc.) to create a real but
 * <strong>unverified</strong> portal User and Organization. The account is marked with
 * {@code verificationStatus = DEMO_BUYER} and must NOT be treated as eligible for
 * RFQ creation until the buyer completes profile verification.</p>
 *
 * <p>The predefined demo phone number is used only as a placeholder so the buyer can
 * log in initially. It is <strong>never</strong> treated as verified.</p>
 */
@Slf4j
@Service
public class DemoBuyerRegistrationService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private ClientDao clientDao;

    @Autowired
    private OrgTypeDao orgTypeDao;

    @Autowired
    private MasterStatusDao masterStatusDao;

    @Autowired
    private SelfRegistrationService selfRegistrationService;

    @Value("${app.rfq.demo-phone:9999999991}")
    private String demoPhoneNumber;

    @Value("${app.rfq.demo-password:Welcome@123}")
    private String demoPassword;

    /**
     * Creates a demo buyer account for the given email address.
     *
     * <p>If a user with this email already exists (active), the existing user is returned
     * without creating a duplicate. This ensures idempotency when the scheduler picks up
     * the same email multiple times.</p>
     *
     * @param senderEmail the email address extracted from the incoming RFQ email
     * @param senderName  optional sender display name (extracted from email headers)
     * @return the created or existing {@link User}, or {@code null} if creation fails
     */
    @Transactional
    public User createDemoBuyer(String senderEmail, String senderName) {
        String normalizedEmail = senderEmail.trim().toLowerCase();
        log.info("Creating demo buyer account for unregistered sender: {}", normalizedEmail);

        // ── Idempotency: check if user already exists ──
        User existingUser = userDao.findByUsernameAndActive(normalizedEmail, true);
        if (existingUser != null) {
            log.info("User already exists for email {}: userId={}, verificationStatus={}",
                    normalizedEmail, existingUser.getId(), existingUser.getVerificationStatus());
            if (StatusConstants.DEMO_BUYER.equals(existingUser.getVerificationStatus())) {
                String expectedPhone = PhoneNumberUtils.normalize(demoPhoneNumber);
                boolean changed = false;
                if (expectedPhone != null && !expectedPhone.equals(existingUser.getPhone())) {
                    log.info("Syncing demo buyer phone for {} from {} to {}", normalizedEmail, existingUser.getPhone(), expectedPhone);
                    existingUser.setPhone(expectedPhone);
                    if (existingUser.getOrg() != null) {
                        existingUser.getOrg().setOrganizationPhonenumber(expectedPhone);
                        clientDao.save(existingUser.getOrg());
                    }
                    changed = true;
                }
                String initialPassword = (demoPassword != null && !demoPassword.isBlank())
                        ? demoPassword.trim() : "Welcome@123";
                if (!initialPassword.equals(existingUser.getPassword())) {
                    existingUser.setPassword(initialPassword);
                    changed = true;
                }
                if (changed) {
                    existingUser = userDao.save(existingUser);
                }
            }
            return existingUser;
        }

        try {
            // ── Create Organization ──
            OrgType clientOrgType = orgTypeDao.findByTypeName(ApplicationConstants.CLIENT);
            if (clientOrgType == null) {
                log.error("Cannot create demo buyer: CLIENT org type not configured");
                return null;
            }

            String companyName = buildCompanyName(normalizedEmail, senderName);

            Organization org = new Organization();
            org.setCompanyName(companyName);
            org.setEmail(normalizedEmail);
            org.setOrganizationPhonenumber(PhoneNumberUtils.normalize(demoPhoneNumber));
            org.setOrgType(clientOrgType);
            org.setSelfClient(true);
            org.setGmtName("GMT Basic");
            org.setBfsName(StatusConstants.BFS_PRO);
            org.setSourceType("EMAIL"); // distinguishes from TOOL/APP registrations
            org.setCompanyId(selfRegistrationService.generateId(companyName));

            Organization savedOrg = clientDao.save(org);
            log.info("Demo buyer organization created: orgId={}, companyName={}", savedOrg.getId(), companyName);

            // ── Create User ──
            MasterStatus clientNewStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_NEW);
            Role initiatorRole = roleDao.findByRoleNameAndActive(StatusConstants.ClientInitiator, true);

            if (clientNewStatus == null || initiatorRole == null) {
                log.error("Cannot create demo buyer: CLIENT_NEW status or ClientInitiator role not configured");
                return null;
            }

            User user = new User();
            user.setOrg(savedOrg);
            user.setUsername(normalizedEmail);
            user.setEmail(normalizedEmail);
            user.setFullName(senderName != null && !senderName.isBlank() ? senderName : extractNameFromEmail(normalizedEmail));
            user.setPhone(PhoneNumberUtils.normalize(demoPhoneNumber));
            user.setApproved(true);
            user.setResetPassword(true);
            user.setActive(true);
            user.setSelfClient(true);
            user.setClientStatus(clientNewStatus);
            user.setRole(initiatorRole);
            user.setUniqueId(selfRegistrationService.generateId(demoPhoneNumber));
            user.setSourceType("EMAIL");
            user.setActivityTs(null);
            user.setCreatedTS(new Date());
            String initialPassword = (demoPassword != null && !demoPassword.isBlank())
                    ? demoPassword.trim()
                    : "Welcome@123";
            user.setPassword(initialPassword);
            // Mark as DEMO_BUYER — NOT eligible for RFQ creation until verified
            user.setVerificationStatus(StatusConstants.DEMO_BUYER);

            User savedUser = userDao.save(user);
            log.info("Demo buyer user created: userId={}, email={}, verificationStatus={}",
                    savedUser.getId(), normalizedEmail, savedUser.getVerificationStatus());

            return savedUser;

        } catch (Exception e) {
            log.error("Failed to create demo buyer account for {}: {}", normalizedEmail, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Checks whether the given user is a fully verified buyer eligible for RFQ creation.
     * A demo/unverified buyer must NOT have RFQs created on their behalf.
     */
    public boolean isBuyerFullyVerified(User user) {
        if (user == null || !user.isActive()) {
            return false;
        }
        String status = user.getVerificationStatus();
        // Only PROFILE_COMPLETED or EMAIL_VERIFIED (legacy fully-verified state) qualifies
        return StatusConstants.PROFILE_COMPLETED.equals(status)
                || StatusConstants.EMAIL_VERIFIED.equals(status);
    }

    /**
     * Returns the configured demo phone number.
     */
    public String getDemoPhoneNumber() {
        return demoPhoneNumber;
    }

    private String buildCompanyName(String email, String senderName) {
        if (senderName != null && !senderName.isBlank()) {
            return senderName.trim();
        }
        // Use email domain as company name placeholder
        String domain = email.contains("@") ? email.substring(email.indexOf("@") + 1) : email;
        // Remove common suffixes
        domain = domain.replaceAll("\\.(com|co\\.in|in|org|net|io)$", "");
        return Character.toUpperCase(domain.charAt(0)) + domain.substring(1) + " (Demo)";
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return "Valued Buyer";
        String prefix = email.substring(0, email.indexOf("@"));
        String[] parts = prefix.split("[._\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "Valued Buyer" : result;
    }
}
