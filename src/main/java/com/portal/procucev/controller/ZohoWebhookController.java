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
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

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
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) {

        try {
            ContentCachingRequestWrapper wrapper =
                    WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);

            if (wrapper == null) {
                log.error("Request not wrapped");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Wrapper missing");
            }

            String rawBody = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            // 🔴 IMPORTANT: log all headers once
            Collections.list(request.getHeaderNames())
                    .forEach(h -> log.info("HEADER {} = {}", h, request.getHeader(h)));

            String signature = request.getHeader("X-Zoho-Webhook-Signature");

            log.info("RAW BODY: {}", rawBody);
            log.info("HEADER SIG: {}", signature);

            if (signature == null || signature.isBlank()) {
                log.error("Zoho signature header missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
            }

            String computedSig = verifier.computeSignature(rawBody);
            log.info("COMPUTED SIG: {}", computedSig);

            if (!verifier.verify(rawBody, signature)) {
                log.error("Invalid Zoho webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            ZohoPaymentLinkWebhookRequest event =
                    objectMapper.readValue(rawBody, ZohoPaymentLinkWebhookRequest.class);

            log.info("Zoho Event Type: {}", event.getEvent_type());

            zohoWebhookService.processWebhook(event);

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }
    }


}


