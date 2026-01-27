package com.portal.procucev.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.model.PaymentLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private final PaymentLinkRepository paymentLinkRepo;
    private final ZohoApiClient zohoApiClient;
    private final ObjectMapper objectMapper;

    @Value("${zoho.payments.base-url}")
    private String baseUrl;

    @Value("${zoho.payments.account-id}")
    private String accountId;

    @Value("${jobs.enabled}")
    private boolean isEnabled;

    @Scheduled(cron = "${zoho.recon.job.cron}")
    @Transactional
    public void reconcilePayments() {

        if (isEnabled) {
            List<PaymentLink> pendingLinks = paymentLinkRepo.findByStatusIn(List.of("CREATED", "active", "pending"));

            log.info("Recon job started. Pending links: {}", pendingLinks.size());

            for (PaymentLink link : pendingLinks) {
                try {
                    syncPaymentLink(link);
                } catch (Exception e) {
                    log.error("Recon failed for link {}", link.getZohoPaymentLinkId(), e);
                }
            }
        }else {
            log.info("Recon job is disabled. Skipping execution.");
        }
    }

    private void syncPaymentLink(final PaymentLink link) throws Exception {

        log.info("Recon syncing payment link {}", link.getZohoPaymentLinkId());
        String url = baseUrl + "/paymentlinks/" + link.getZohoPaymentLinkId() + "?account_id=" + accountId;

        Map<String, Object> response = zohoApiClient.get(url, Map.class).getBody();

        Map<String, Object> pl = (Map<String, Object>) response.get("payment_links");
        log.info("Payment link data from Zoho: {}", pl);
        String status = (String) pl.get("status");
        log.info("Payment link {} status from Zoho: {}", link.getZohoPaymentLinkId(), status);
        String amountPaid = (String) pl.get("amount_paid");

        link.setStatus(status);
        link.setZohoJobRawResponse(objectMapper.writeValueAsString(response));

        paymentLinkRepo.save(link);

        // TODO if needed, activate subscription based on status
//        if ("paid".equalsIgnoreCase(status)) {
//            subscriptionService.activateSubscription(link);
//        }

        log.info("Recon synced payment link {} status {}", link.getZohoPaymentLinkId(), status);
    }
}
