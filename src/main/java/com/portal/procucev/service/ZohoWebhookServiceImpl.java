package com.portal.procucev.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.PaymentLinks;
import com.portal.procucev.Dto.ZohoPaymentLinkWebhookRequest;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.dao.ZohoPaymentWebhookRepository;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.model.ZohoPaymentWebhookEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZohoWebhookServiceImpl implements ZohoWebhookService {

    private final PaymentLinkRepository paymentLinkRepo;
    private final ZohoPaymentWebhookRepository webhookRepo;
    private final OrgDao orgDao;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processWebhook(final ZohoPaymentLinkWebhookRequest request) {

        log.info("Zoho webhook received: {}", request.getEvent_type());

        PaymentLinks links = request.getEvent_object().getPayment_links();

        // ✅ Idempotency check: if we already processed this eventId, skip
        ZohoPaymentWebhookEntity existing = webhookRepo.findByEventId(request.getEvent_id());
        if (existing != null && Boolean.TRUE.equals(existing.isProcessed())) {
            log.info("Skipping already processed Zoho event_id={}", request.getEvent_id());
            return;
        }

        // ✅ Save raw webhook (new or update existing)
        ZohoPaymentWebhookEntity entity = existing == null ? new ZohoPaymentWebhookEntity() : existing;
        entity.setEventId(request.getEvent_id());
        entity.setEventType(request.getEvent_type());
        entity.setPaymentLinkId(links.getPayment_link_id());
        entity.setStatus(links.getStatus());
        try {
            entity.setRawPayload(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        entity.setProcessed(false);
        entity.setProcessedAt(null);
        webhookRepo.save(entity);

        // ✅ Update payment link status
        paymentLinkRepo.findByZohoPaymentLinkId(links.getPayment_link_id()).ifPresent(link -> {

            switch (request.getEvent_type()) {

                case "payment_link.paid":
                    link.setStatus("PAID");
                    activateSubscription(link, request);
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

            // mark webhook processed after successful handling
            entity.setProcessed(true);
            entity.setProcessedAt(Instant.now());
            webhookRepo.save(entity);
        });
    }

    // Extracted helper: activates subscription for the org associated with the payment link.
    private void activateSubscription(com.portal.procucev.model.PaymentLink link, ZohoPaymentLinkWebhookRequest request) {
        try {
            com.portal.procucev.model.Organization org = link.getOrganization();
            com.portal.procucev.model.SubscriptionPlan plan = link.getPlan();

            if (org == null || plan == null) {
                log.warn("Cannot update subscription: organization or plan missing for payment link id={}", link.getId());
                return;
            }

            // Determine start instant: prefer Zoho event_time (milliseconds) if present, otherwise now
            Instant startInstant = Instant.now();
            try {
                if (request != null && request.getEvent_time() != null && request.getEvent_time() > 0) {
                    startInstant = Instant.ofEpochMilli(request.getEvent_time());
                }
            } catch (Exception ex) {
                log.debug("Failed to parse event_time from Zoho request, using server time", ex);
            }

            // Compute expiry by adding months from plan
            int months = Math.max(0, plan.getSubscriptionPeriodMonths());
            ZonedDateTime zdt = ZonedDateTime.ofInstant(startInstant, ZoneId.systemDefault()).plusMonths(months);
            Instant endInstant = zdt.toInstant();

            org.setSubscriptionPlan(plan);
            org.setRfqCredits(org.getRfqCredits()+plan.getRfqBundleSize());
            org.setSubscriptionStart(java.util.Date.from(startInstant));
            org.setSubscriptionExpiry(java.util.Date.from(endInstant));

            orgDao.save(org);
            log.info("Activated subscription for org={} plan={} start={} end={}", org.getId(), plan.getId(), org.getSubscriptionStart(), org.getSubscriptionExpiry());
        } catch (Exception e) {
            log.error("Error while activating subscription for payment link {}", link.getZohoPaymentLinkId(), e);
        }
    }
}
