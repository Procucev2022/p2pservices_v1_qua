package com.portal.procucev.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.PaymentLinks;
import com.portal.procucev.Dto.ZohoPaymentLinkWebhookRequest;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.dao.ZohoPaymentWebhookRepository;
import com.portal.procucev.model.ZohoPaymentWebhookEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZohoWebhookServiceImpl implements ZohoWebhookService {

    private final PaymentLinkRepository paymentLinkRepo;
    private final ZohoPaymentWebhookRepository webhookRepo;
    private final ObjectMapper objectMapper;

    @Override
    public void processWebhook(final ZohoPaymentLinkWebhookRequest request) {

        log.info("Zoho webhook received: {}", request.getEvent_type());

        PaymentLinks links = request.getEvent_object().getPayment_links();

        // ✅ Save raw webhook
        ZohoPaymentWebhookEntity entity = new ZohoPaymentWebhookEntity();
        entity.setEventId(request.getEvent_id());
        entity.setEventType(request.getEvent_type());
        entity.setPaymentLinkId(links.getPayment_link_id());
        entity.setStatus(links.getStatus());
        try {
            entity.setRawPayload(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        webhookRepo.save(entity);

        // ✅ Update payment link status
        paymentLinkRepo.findByZohoPaymentLinkId(links.getPayment_link_id()).ifPresent(link -> {

            switch (request.getEvent_type()) {

                case "payment_link.paid":
                    link.setStatus("PAID");
                    //TODO trigger post-payment actions like activating subscription
                    break;

                case "payment_link.canceled":
                    link.setStatus("CANCELED");
                    break;

                case "payment_link.expired":
                    link.setStatus("EXPIRED");
                    break;

                default:
                    log.warn("Unhandled event type: {}", request.getEvent_type());
            }

            link.setZohoRawWebhook(entity.getRawPayload());
            paymentLinkRepo.save(link);
        });
    }
}
