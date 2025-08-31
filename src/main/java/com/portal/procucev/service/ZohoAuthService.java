package com.portal.procucev.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.portal.procucev.model.TokenResponse;

@Service
public class ZohoAuthService {

    @Value("${zoho.client.id}")
    private String clientId;

    @Value("${zoho.client.secret}")
    private String clientSecret;

    @Value("${zoho.refresh.token}")
    private String refreshToken;

    @Value("${zoho.refresh.url}")
    private String refreshUrl;

    private String cachedAccessToken;
    private Instant tokenExpiryTime;

    private final RestTemplate restTemplate = new RestTemplate();

    public synchronized String getAccessToken() {
        if (cachedAccessToken == null || Instant.now().isAfter(tokenExpiryTime)) {
            refreshAccessToken();
        }
        return cachedAccessToken;
    }

    private void refreshAccessToken() {
        String url = refreshUrl +
                "?refresh_token=" + refreshToken +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&grant_type=refresh_token";

        ResponseEntity<TokenResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, null, TokenResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            TokenResponse tokenResponse = response.getBody();
            this.cachedAccessToken = tokenResponse.getAccessToken();
            this.tokenExpiryTime = Instant.now().plusSeconds(tokenResponse.getExpiresIn() - 60); // refresh 1 min early
        } else {
            throw new RuntimeException("Failed to refresh Zoho access token: " + response);
        }
    }
}
