package com.portal.procucev.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.ZohoPaymentLinkWebhookRequest;
import com.portal.procucev.service.ZohoWebhookService;
import com.portal.procucev.service.ZohoWebhookSignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/zoho")
@RequiredArgsConstructor
@Slf4j
public class ZohoWebhookController {

    @Value("${zoho.payments.account-id}")
    private Long expectedAccountId;

    private final ZohoWebhookService zohoWebhookService;
    private final ZohoWebhookSignatureUtil verifier;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request, @RequestBody String rawBody) throws Exception {

        String signature = request.getHeader("X-Zoho-Signature");

        log.info("RAW BODY: {}", rawBody);
        log.info("HEADER SIG: {}", signature);
        log.info("COMPUTED SIG: {}", verifier.computeSignature(rawBody));

        // 1. VERIFY SIGNATURE FIRST
        if (!verifier.verify(rawBody, signature)) {
            log.error("Invalid Zoho webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        //2. PARSE JSON AFTER SIGNATURE VALIDATION
        ZohoPaymentLinkWebhookRequest event = objectMapper.readValue(rawBody, ZohoPaymentLinkWebhookRequest.class);

        log.info("Zoho webhook event: {}", event.getEvent_type());

        // 3. VALIDATE ACCOUNT ID
        if (!expectedAccountId.equals(event.getAccount_id())) {
            log.error("Invalid account id: {}", event.getAccount_id());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid account");
        }

        // 4. PROCESS EVENT
        zohoWebhookService.processWebhook(event);

        return ResponseEntity.ok("OK");
    }

}


