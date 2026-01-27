package com.portal.procucev.config;

import com.portal.procucev.service.ZohoOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoApiClient {

    private final RestTemplate restTemplate;
    private final ZohoOAuthService authService;

    public <T> ResponseEntity<T> post(String url, Object body, Class<T> clazz) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Zoho-oauthtoken " + authService.getValidAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);

        log.info("Calling Zoho API: {}", url);

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                clazz
        );
    }

    public <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Zoho-oauthtoken " + authService.getValidAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    }

}

