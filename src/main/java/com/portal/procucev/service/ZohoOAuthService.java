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

        ZohoTokenResponse r;
        try {
            ResponseEntity<ZohoTokenResponse> response =
                    restTemplate.postForEntity(tokenUrl, request, ZohoTokenResponse.class);
            r = response.getBody();
        } catch (Exception e) {
            log.error("Zoho token refresh call failed: {}", e.getMessage(), e);
            throw new IllegalStateException(
                    "Could not reach the Zoho token service: " + e.getMessage(), e);
        }

        if (r == null) {
            throw new IllegalStateException("Zoho token service returned an empty response");
        }

        // Zoho answers a rejected refresh with HTTP 200 and an error body, so the
        // failure has to be detected here. Without this the null token was saved
        // and the stale one sent onward, surfacing later as an opaque 401.
        if (r.getError() != null && !r.getError().isBlank()) {
            log.error("Zoho rejected the token refresh: {}", r.getError());
            throw new IllegalStateException(describeTokenError(r.getError()));
        }

        if (r.getAccessToken() == null || r.getAccessToken().isBlank()) {
            throw new IllegalStateException(
                    "Zoho did not return an access token. Check the Zoho payments credentials.");
        }

        token.setAccessToken(r.getAccessToken());
        token.setExpiryTime(Instant.now().plusSeconds(r.getExpiresIn()));
        token.setLastUpdated(Instant.now());

        repo.save(token);

        log.info("Zoho access token refreshed, valid for {} seconds", r.getExpiresIn());

        return token.getAccessToken();
    }

    /** Turns a Zoho error code into a message that names the setting to fix. */
    private String describeTokenError(String error) {
        switch (error) {
            case "invalid_client_secret":
                return "Zoho rejected the client secret. Set ZOHO_CLIENT_SECRET to the secret "
                        + "for client id " + clientId + " from the Zoho API Console.";
            case "invalid_client":
                return "Zoho rejected the client id. Check zoho.client.id and ZOHO_CLIENT_SECRET "
                        + "belong to the same Zoho application.";
            case "invalid_code":
            case "invalid_grant":
                return "The Zoho refresh token is no longer valid. Generate a new one and update "
                        + "the zoho_oauth_token record.";
            default:
                return "Zoho token refresh failed: " + error;
        }
    }
}
