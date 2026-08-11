package com.portal.procucev.service;

import com.portal.procucev.Dto.ZohoTokenResponse;
import com.portal.procucev.dao.ZohoOAuthTokenRepository;
import com.portal.procucev.model.ZohoOAuthToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZohoOAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ZohoOAuthTokenRepository repo;

    @InjectMocks
    private ZohoOAuthService zohoOAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(zohoOAuthService, "tokenUrl", "https://accounts.zoho.com/oauth/v2/token");
        ReflectionTestUtils.setField(zohoOAuthService, "clientId", "CLIENT_ID");
        ReflectionTestUtils.setField(zohoOAuthService, "clientSecret", "CLIENT_SECRET");
    }

    @Test
    void testGetValidAccessToken_NotConfigured() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> zohoOAuthService.getValidAccessToken());
    }

    @Test
    void testGetValidAccessToken_UnexpiredToken() {
        ZohoOAuthToken token = new ZohoOAuthToken();
        token.setAccessToken("valid_access_token");
        token.setExpiryTime(Instant.now().plusSeconds(3600));

        when(repo.findById(1L)).thenReturn(Optional.of(token));

        String accessToken = zohoOAuthService.getValidAccessToken();
        assertEquals("valid_access_token", accessToken);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testGetValidAccessToken_NullAccessToken_Refreshes() {
        ZohoOAuthToken token = new ZohoOAuthToken();
        token.setAccessToken(null);
        token.setRefreshToken("ref_token");
        token.setExpiryTime(Instant.now().plusSeconds(3600));

        when(repo.findById(1L)).thenReturn(Optional.of(token));

        ZohoTokenResponse response = new ZohoTokenResponse();
        response.setAccessToken("new_access_token");
        response.setExpiresIn(3600);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ZohoTokenResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        String newAccessToken = zohoOAuthService.getValidAccessToken();
        assertEquals("new_access_token", newAccessToken);
    }

    @Test
    void testGetValidAccessToken_NullExpiryTime_Refreshes() {
        ZohoOAuthToken token = new ZohoOAuthToken();
        token.setAccessToken("old_token");
        token.setRefreshToken("ref_token");
        token.setExpiryTime(null);

        when(repo.findById(1L)).thenReturn(Optional.of(token));

        ZohoTokenResponse response = new ZohoTokenResponse();
        response.setAccessToken("new_access_token");
        response.setExpiresIn(3600);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ZohoTokenResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        String newAccessToken = zohoOAuthService.getValidAccessToken();
        assertEquals("new_access_token", newAccessToken);
    }

    @Test
    void testGetValidAccessToken_ExpiredToken_Refreshes() {
        ZohoOAuthToken token = new ZohoOAuthToken();
        token.setAccessToken("old_token");
        token.setRefreshToken("ref_token");
        token.setExpiryTime(Instant.now().minusSeconds(10));

        when(repo.findById(1L)).thenReturn(Optional.of(token));

        ZohoTokenResponse response = new ZohoTokenResponse();
        response.setAccessToken("new_access_token");
        response.setExpiresIn(3600);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ZohoTokenResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        String newAccessToken = zohoOAuthService.getValidAccessToken();
        assertEquals("new_access_token", newAccessToken);
        verify(repo).save(token);
    }

    @Test
    void testGetValidAccessToken_RefreshTokenNull() {
        ZohoOAuthToken token = new ZohoOAuthToken();
        token.setRefreshToken(null);
        token.setExpiryTime(Instant.now().minusSeconds(10));

        when(repo.findById(1L)).thenReturn(Optional.of(token));

        assertThrows(NullPointerException.class, () -> zohoOAuthService.getValidAccessToken());
    }
}
