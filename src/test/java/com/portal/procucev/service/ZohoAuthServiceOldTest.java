package com.portal.procucev.service;

import com.portal.procucev.model.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ZohoAuthServiceOldTest {

    private ZohoAuthServiceOld service;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        service = new ZohoAuthServiceOld();
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "clientId", "client_id");
        ReflectionTestUtils.setField(service, "clientSecret", "client_secret");
        ReflectionTestUtils.setField(service, "refreshToken", "refresh_token");
        ReflectionTestUtils.setField(service, "refreshUrl", "http://test.url");
    }

    @Test
    void testGetAccessToken_SuccessAndCached() {
        TokenResponse responseBody = new TokenResponse();
        responseBody.setAccessToken("token123");
        responseBody.setExpiresIn(3600);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(TokenResponse.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        String token1 = service.getAccessToken();
        assertEquals("token123", token1);

        // Second call should return cached token without calling restTemplate again
        String token2 = service.getAccessToken();
        assertEquals("token123", token2);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(TokenResponse.class));
    }

    @Test
    void testGetAccessToken_ExpiredToken() {
        TokenResponse responseBody = new TokenResponse();
        responseBody.setAccessToken("token123");
        responseBody.setExpiresIn(3600);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(TokenResponse.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        service.getAccessToken();
        ReflectionTestUtils.setField(service, "tokenExpiryTime", Instant.now().minusSeconds(10));

        service.getAccessToken();
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(TokenResponse.class));
    }

    @Test
    void testGetAccessToken_FailureResponse() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(TokenResponse.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        assertThrows(RuntimeException.class, () -> service.getAccessToken());
    }

    @Test
    void testGetAccessToken_NullBody() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(TokenResponse.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThrows(RuntimeException.class, () -> service.getAccessToken());
    }
}
