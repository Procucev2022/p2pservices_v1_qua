package com.portal.procucev.config;

import com.portal.procucev.service.ZohoOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZohoApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ZohoOAuthService authService;

    @InjectMocks
    private ZohoApiClient zohoApiClient;

    @BeforeEach
    void setUp() {
        when(authService.getValidAccessToken()).thenReturn("valid_token");
    }

    @Test
    void testPost() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("success"));

        ResponseEntity<String> response = zohoApiClient.post("https://api.zoho.com/test", "body", String.class);
        assertNotNull(response);
    }

    @Test
    void testGet() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("success"));

        ResponseEntity<String> response = zohoApiClient.get("https://api.zoho.com/test", String.class);
        assertNotNull(response);
    }
}
