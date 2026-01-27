package com.portal.procucev.service;

import com.portal.procucev.Dto.ZohoPaymentLinkRequest;
import com.portal.procucev.config.ZohoApiClient;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.dao.SubscriptionPlanDao;
import com.portal.procucev.model.PaymentLink;
import com.portal.procucev.model.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentLinkService {

    @Value("${zoho.payments.base-url}")
    private String zohoPaymentsBaseUrl;

    @Value("${zoho.payments.account-id}")
    private String zohoAccountId;

    private final ZohoApiClient zohoApiClient;
    private final PaymentLinkRepository paymentLinkRepo;
    private final SubscriptionPlanDao planRepo;

    public PaymentLink createPaymentLink(Long planId,
                                         String customerEmail,
                                         String phone,
                                         String returnUrl) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Creating payment link for user: {},  User Id {}", userDetails.getUsername() , userDetails.getId());
        log.info("Organization Id: {}", userDetails.getOrgId());

        SubscriptionPlan plan = planRepo.findById(String.valueOf(planId))
                .orElseThrow(() -> new IllegalArgumentException("Subscription Plan not found"));

        ZohoPaymentLinkRequest request = new ZohoPaymentLinkRequest();
        request.setAmount(BigDecimal.valueOf(plan.getLaunchOfferPrice()));
        request.setCurrency("INR");
        request.setEmail(customerEmail);
        request.setPhone(phone);
        request.setDescription("Subscription: " + plan.getPlanName());
        request.setReference_id("PLAN-" + planId + "-" + System.currentTimeMillis());
        request.setNotify_user(true);
        request.setReturn_url(returnUrl);

        ZonedDateTime expiry =
                ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(1);

        request.setExpires_at(expiry.toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE));


        String url = zohoPaymentsBaseUrl + "/paymentlinks?account_id=" + zohoAccountId;

        Map<String, Object> response =
                zohoApiClient.post(url, request, Map.class).getBody();

        Map<String, Object> links =
                (Map<String, Object>) response.get("payment_links");

        PaymentLink link = new PaymentLink();
        link.setPlan(plan);
        link.setAmount(BigDecimal.valueOf(plan.getLaunchOfferPrice()));
        link.setZohoPaymentLinkId((String) links.get("payment_link_id"));
        link.setPaymentUrl((String) links.get("url"));
        link.setStatus((String) links.get("status"));

        paymentLinkRepo.save(link);

        return link;
    }

}
