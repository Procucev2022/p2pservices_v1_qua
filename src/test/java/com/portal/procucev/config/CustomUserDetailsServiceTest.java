package com.portal.procucev.config;

import com.portal.procucev.Dto.AuthUserView;
import com.portal.procucev.dao.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserDao userDao;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userDao);
    }

    private AuthUserView authView(String username, String phone, String password) {
        return new AuthUserView() {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public String getPhone() {
                return phone;
            }
        };
    }

    @Test
    void testLoadUserByUsernameAndPhone_Success() {
        when(userDao.findAuthViewByUsernameAndPhone("testuser", "9876543210"))
                .thenReturn(authView("testuser", "9876543210", "pass"));

        UserDetails userDetails = service.loadUserByUsernameAndPhone("testuser", "9876543210");
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("pass", userDetails.getPassword());
    }

    @Test
    void testLoadUserByUsernameAndPhone_NotFound() {
        when(userDao.findAuthViewByUsernameAndPhone("testuser", "9876543210")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsernameAndPhone("testuser", "9876543210"));
    }

    @Test
    void testLoadUserByUsernameAndPhone_NullPasswordIsAnAuthenticationFailure() {
        when(userDao.findAuthViewByUsernameAndPhone("testuser", "9876543210"))
                .thenReturn(authView("testuser", "9876543210", null));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsernameAndPhone("testuser", "9876543210"));
    }

    @Test
    void testLoadUserByUsername_Unsupported() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));
    }
}
