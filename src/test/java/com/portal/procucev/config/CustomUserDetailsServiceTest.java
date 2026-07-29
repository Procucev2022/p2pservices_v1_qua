package com.portal.procucev.config;

import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;
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

    @Test
    void testLoadUserByUsernameAndPhone_Success() {
        User user = new User();
        user.setUsername("testuser");
        user.setPhone("9876543210");
        user.setPassword("pass");

        when(userDao.findByUsernameAndPhoneAndActive("testuser", "9876543210", true)).thenReturn(user);

        UserDetails userDetails = service.loadUserByUsernameAndPhone("testuser", "9876543210");
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
    }

    @Test
    void testLoadUserByUsernameAndPhone_NotFound() {
        when(userDao.findByUsernameAndPhoneAndActive("testuser", "9876543210", true)).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsernameAndPhone("testuser", "9876543210"));
    }

    @Test
    void testLoadUserByUsername_Unsupported() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));
    }
}
