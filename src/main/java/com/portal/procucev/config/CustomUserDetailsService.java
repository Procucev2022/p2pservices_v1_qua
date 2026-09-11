package com.portal.procucev.config;

import java.util.ArrayList;

import com.portal.procucev.Dto.AuthUserView;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;
import com.portal.procucev.utils.PhoneNumberUtils;
import com.portal.procucev.utils.StatusConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.portal.procucev.rfq.service.DemoBuyerCleanupService;

/**
 * Resolves Spring Security principals from the username plus phone pair used across this app.
 */
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDao userRepository;
    private final DemoBuyerCleanupService cleanupService;

    public CustomUserDetailsService(UserDao userRepository) {
        this(userRepository, null);
    }

    @Autowired
    public CustomUserDetailsService(UserDao userRepository, @Lazy DemoBuyerCleanupService cleanupService) {
        this.userRepository = userRepository;
        this.cleanupService = cleanupService;
    }

    /**
     * Finds a user by email and phone. Uses {@link UserDao#findAuthViewByUsernameAndPhone} for a
     * narrow projection rather than loading the {@code User} entity: that entity's eager
     * {@code role} and {@code clientStatus} associations turned a single lookup into three
     * SELECTs, and none of that data is used here.
     *
     * @param username the account username
     * @param phone the account phone number
     * @return the principal, carrying username and password only
     */
    public UserDetails loadUserByUsernameAndPhone(String username, String phone) {
        // First, check if user exists and is an expired email demo buyer
        User fullUser = userRepository.findByUsernameAndActive(username, true);
        if (fullUser != null && cleanupService != null && cleanupService.isExpired(fullUser)) {
            log.warn("Login attempt for expired email demo buyer: {}. Cleaning up account...", username);
            cleanupService.deleteExpiredDemoBuyer(fullUser);
            throw new UsernameNotFoundException("Temporary login credentials have expired (valid for 1 hour only). The unverified account has been removed. Please submit a new RFQ.");
        }

        AuthUserView user = userRepository.findAuthViewByUsernameAndPhone(username, phone);

        if (user == null) {
            if (fullUser != null) {
                String normPhone = PhoneNumberUtils.normalize(phone);
                String userNorm = PhoneNumberUtils.normalize(fullUser.getPhone());
                if ((normPhone != null && normPhone.equals(userNorm)) || StatusConstants.DEMO_BUYER.equals(fullUser.getVerificationStatus())) {
                    if (fullUser.getPassword() == null) {
                        log.warn("Authentication attempted for user with no stored credential: {}", username);
                        throw new UsernameNotFoundException("User has no stored credential");
                    }
                    log.debug("Resolved principal for demo/flexible user: {}", fullUser.getUsername());
                    return new org.springframework.security.core.userdetails.User(fullUser.getUsername(), fullUser.getPassword(),
                            new ArrayList<>());
                }
            }
            throw new UsernameNotFoundException("User not found with username and phone");
        }
        if (user.getPassword() == null) {
            // Spring Security's User rejects a null password with IllegalArgumentException,
            // which would surface as a 500 rather than an authentication failure.
            log.warn("Authentication attempted for user with no stored credential: {}", username);
            throw new UsernameNotFoundException("User has no stored credential");
        }

        log.debug("Resolved principal for user: {}", user.getUsername());

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
                new ArrayList<>());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Not used in this flow; every caller authenticates with username plus phone.
        throw new UsernameNotFoundException("Username-only login is not supported. Use phone number too.");
    }

}
