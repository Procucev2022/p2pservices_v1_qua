package com.portal.procucev.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtRequestFilter jwtRequestFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void testFilterChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        CorsConfigurer corsConfigurer = mock(CorsConfigurer.class);
        CsrfConfigurer csrfConfigurer = mock(CsrfConfigurer.class);
        AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry registry = mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class, RETURNS_DEEP_STUBS);
        SessionManagementConfigurer sessionConfigurer = mock(SessionManagementConfigurer.class);

        doAnswer(invocation -> {
            Customizer<CorsConfigurer> customizer = invocation.getArgument(0);
            customizer.customize(corsConfigurer);
            return http;
        }).when(http).cors(any());

        doAnswer(invocation -> {
            Customizer<CsrfConfigurer> customizer = invocation.getArgument(0);
            customizer.customize(csrfConfigurer);
            return http;
        }).when(http).csrf(any());

        doAnswer(invocation -> {
            Customizer customizer = invocation.getArgument(0);
            customizer.customize(registry);
            return http;
        }).when(http).authorizeHttpRequests(any());

        doAnswer(invocation -> {
            Customizer customizer = invocation.getArgument(0);
            customizer.customize(sessionConfigurer);
            return http;
        }).when(http).sessionManagement(any());

        DefaultSecurityFilterChain sfc = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(sfc);

        SecurityFilterChain result = securityConfig.filterChain(http);
        assertNotNull(result);
    }

    @Test
    void testCorsConfigurationSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);
    }

    @Test
    void testPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
    }

    @Test
    void testCacheFilter() {
        FilterRegistrationBean<RequestBodyCacheFilter> registrationBean = securityConfig.cacheFilter();
        assertNotNull(registrationBean);
        assertNotNull(registrationBean.getFilter());
    }

    @Test
    void testAuthenticationManager() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager mgr = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(mgr);

        assertEquals(mgr, securityConfig.authenticationManager(config));
    }

    @Test
    void testSchedulerLockConfigAndShedLockConfiguration() {
        SchedulerLockConfig schedulerLockConfig = new SchedulerLockConfig();
        assertNotNull(schedulerLockConfig);

        ShedLockConfiguration shedLockConfiguration = new ShedLockConfiguration();
        javax.sql.DataSource dataSource = mock(javax.sql.DataSource.class);
        assertNotNull(shedLockConfiguration.lockProvider(dataSource));
    }
}
