package com.portal.procucev.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.Dto.ZohoPaymentLinkRequest;
import com.portal.procucev.config.ZohoApiClient;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.dao.PaymentLinkRepository;
import com.portal.procucev.dao.SubscriptionPlanDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.PaymentLink;
import com.portal.procucev.model.SubscriptionPlan;
import com.portal.procucev.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private final UserDao userDao;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentLink createPaymentLink(final Long planId, final String userPhone, final String userEmail, final String returnUrl) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userDao.findByUsernameAndPhoneAndActive(userEmail, userPhone, true);

        if (Objects.isNull(user)) throw new AppException("User not found");

        if (!userDetails.getUsername().equalsIgnoreCase(user.getUsername()))
            throw new AppException("User details do not match the authenticated user");


        if (Objects.isNull(user.getOrg())) throw new AppException("User organization not found");
        log.info("Creating Payment Link for user {} with org Id {}", userDetails.getUsername(), user.getOrg().getId());

        SubscriptionPlan plan = planRepo.findById(String.valueOf(planId)).orElseThrow(() -> new AppException("Subscription Plan not found"));

        ZohoPaymentLinkRequest request = new ZohoPaymentLinkRequest();
        request.setAmount(BigDecimal.valueOf(plan.getLaunchOfferPrice() > 0 ? plan.getLaunchOfferPrice() : plan.getSubscriptionPrice()));
        request.setCurrency("INR");
        request.setEmail(userEmail);
        request.setPhone(userPhone);
        request.setDescription("Subscription plan name : " + plan.getPlanName());
        request.setReference_id("PLAN-" + planId + "-" + System.currentTimeMillis());
        request.setNotify_user(true);
        request.setReturn_url(returnUrl);

        ZonedDateTime expiry = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(1);

        request.setExpires_at(expiry.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));


        String url = zohoPaymentsBaseUrl + "/paymentlinks?account_id=" + zohoAccountId;

        Map<String, Object> response = zohoApiClient.post(url, request, Map.class).getBody();

        Map<String, Object> links = (Map<String, Object>) response.get("payment_links");

        PaymentLink link = new PaymentLink();
        link.setPlan(plan);
        link.setAmount(BigDecimal.valueOf(plan.getLaunchOfferPrice()));
        link.setZohoPaymentLinkId((String) links.get("payment_link_id"));
        link.setPaymentUrl((String) links.get("url"));
        link.setStatus((String) links.get("status"));
        link.setUserId(user.getId());
        link.setOrganization(user.getOrg());
        try {
            link.setRawResponse(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            log.error("unable to save raw response for payment link creation", e);
        }

        paymentLinkRepo.save(link);

        return link;
    }

}
