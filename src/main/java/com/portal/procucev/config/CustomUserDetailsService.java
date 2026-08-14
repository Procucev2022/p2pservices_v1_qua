package com.portal.procucev.config;

import java.util.ArrayList;

import com.portal.procucev.Dto.AuthUserView;
import com.portal.procucev.dao.UserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Resolves Spring Security principals from the username plus phone pair used across this app.
 */
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDao userRepository;

    public CustomUserDetailsService(UserDao userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the principal for the given username and phone.
     *
     * <p>This runs once per authenticated request from {@link JwtRequestFilter}, so it reads a
     * narrow projection rather than loading the {@code User} entity: that entity's eager
     * {@code role} and {@code clientStatus} associations turned a single lookup into three
     * SELECTs, and none of that data is used here.
     *
     * @param username the account username
     * @param phone the account phone number
     * @return the principal, carrying username and password only
     */
    public UserDetails loadUserByUsernameAndPhone(String username, String phone) {
        AuthUserView user = userRepository.findAuthViewByUsernameAndPhone(username, phone);

        if (user == null) {
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
