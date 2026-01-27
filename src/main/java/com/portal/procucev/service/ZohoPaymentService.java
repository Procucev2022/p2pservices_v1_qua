package com.portal.procucev.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ZohoPaymentService {

    @Autowired
    private ZohoAuthServiceOld zohoAuthService;

    private final RestTemplate restTemplate = new RestTemplate();

    public String createPayment(Object paymentRequest) {
        String accessToken = zohoAuthService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(paymentRequest, headers);

        String apiUrl = "https://www.zohoapis.com/books/v3/payments"; // Example endpoint

        ResponseEntity<String> response =
                restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        return response.getBody();
    }
}
