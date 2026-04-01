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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/zoho")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class ZohoWebhookController {

    @Value("${zoho.payments.account-id}")
    private Long expectedAccountId;

    private final ZohoWebhookService zohoWebhookService;
    private final ZohoWebhookSignatureUtil verifier;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request,
                                                @RequestBody String rawBody) throws Exception {

        String signatureHeader = request.getHeader("X-Zoho-Webhook-Signature");

        log.info("RAW BODY: {}", rawBody);
        log.info("HEADER SIG: {}", signatureHeader);

        if (!verifier.verifyZohoSignature(signatureHeader, rawBody)) {
            log.error("Invalid Zoho webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        ZohoPaymentLinkWebhookRequest event =
                objectMapper.readValue(rawBody, ZohoPaymentLinkWebhookRequest.class);

        log.info("Zoho webhook event: {}", event.getEvent_type());

        if (!expectedAccountId.equals(event.getAccount_id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid account");
        }

        zohoWebhookService.processWebhook(event);

        return ResponseEntity.ok("OK");
    }



}


