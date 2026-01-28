package com.portal.procucev.service;

import com.portal.procucev.Dto.ZohoTokenResponse;
import com.portal.procucev.dao.ZohoOAuthTokenRepository;
import com.portal.procucev.model.ZohoOAuthToken;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoOAuthService {

    private final RestTemplate restTemplate;
    private final ZohoOAuthTokenRepository repo;

    @Value("${zoho.oauth.token.url}")
    private String tokenUrl;

    @Value("${zoho.client.id}")
    private String clientId;

    @Value("${zoho.client.secret}")
    private String clientSecret;

    @Transactional
    public String getValidAccessToken() {

        ZohoOAuthToken token = repo.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Zoho refresh token not configured"));

        if (token.getAccessToken() != null &&
                token.getExpiryTime() != null &&
                token.getExpiryTime().isAfter(Instant.now().plusSeconds(60))) {

            return token.getAccessToken();
        }

        return refreshAndStoreToken(token);
    }

    private String refreshAndStoreToken(ZohoOAuthToken token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", token.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<ZohoTokenResponse> response =
                restTemplate.postForEntity(tokenUrl, request, ZohoTokenResponse.class);

        ZohoTokenResponse r = response.getBody();

        token.setAccessToken(r.getAccessToken());
        token.setExpiryTime(Instant.now().plusSeconds(r.getExpiresIn()));
        token.setLastUpdated(Instant.now());

        repo.save(token);

        log.info("Zoho access token refreshed");

        return token.getAccessToken();
    }
}
