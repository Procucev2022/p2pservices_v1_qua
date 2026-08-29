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
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

        User user = resolvePayer(userEmail, userPhone);

        if (Objects.isNull(user)) throw new AppException("User not found");

        if (!userDetails.getUsername().equalsIgnoreCase(user.getUsername()))
            throw new AppException("User details do not match the authenticated user");


        if (Objects.isNull(user.getOrg())) throw new AppException("User organization not found");
        log.info("Creating Payment Link for user {} with org Id {}", userDetails.getUsername(), user.getOrg().getId());

        SubscriptionPlan plan = planRepo.findById(String.valueOf(planId)).orElseThrow(() -> new AppException("Subscription Plan not found"));

        // Null-safe: a plan row with no launched_status value must not NPE here.
        if ("NO".equalsIgnoreCase(plan.getLaunchedStatus()))
            throw new AppException("Subscription plan is launching soon. Please try again later.");


        ZohoPaymentLinkRequest request = new ZohoPaymentLinkRequest();
        // Calculate base price (launch offer if present, otherwise subscription price)
        BigDecimal basePrice = BigDecimal.valueOf(plan.getLaunchOfferPrice() > 0 ? plan.getLaunchOfferPrice() : plan.getSubscriptionPrice());
        // Apply 18% GST on the total
        BigDecimal gstMultiplier = new BigDecimal("1.18");
        BigDecimal amountWithGst = basePrice.multiply(gstMultiplier).setScale(2, RoundingMode.HALF_UP);
        request.setAmount(amountWithGst);
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

        Map<String, Object> response;
        try {
            response = zohoApiClient.post(url, request, Map.class).getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            // A 401 here means the access token was rejected, which is a
            // credentials problem rather than anything the buyer can correct.
            log.error("Zoho rejected the access token for plan {}. Verify the Zoho payments "
                    + "credentials and refresh token.", planId, e);
            throw new AppException("Payment gateway authorisation failed. "
                    + "Please contact support: the payment provider credentials need attention.");
        } catch (Exception e) {
            // Surface gateway faults as a readable message instead of a bare 500.
            log.error("Zoho payment link creation failed for plan {}: {}", planId, e.getMessage(), e);
            throw new AppException("Payment gateway error: " + e.getMessage());
        }

        if (Objects.isNull(response) || Objects.isNull(response.get("payment_links"))) {
            log.error("Zoho returned no payment_links for plan {}. Response: {}", planId, response);
            throw new AppException("Payment gateway did not return a payment link");
        }

        Map<String, Object> links = (Map<String, Object>) response.get("payment_links");

        PaymentLink link = new PaymentLink();
        link.setPlan(plan);
        link.setAmount(request.getAmount());
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

    /**
     * Resolves the paying user from their login email and phone.
     *
     * The same username legitimately exists several times, once per
     * organization, each row carrying a different phone number. The derived
     * single-result finder throws when more than one row matches, which
     * surfaced as an unhandled 500, so the candidates are fetched as a list and
     * narrowed here.
     */
    private User resolvePayer(String userEmail, String userPhone) {
        List<User> candidates = userDao.findByUsernameAndPhone(userEmail, userPhone);

        if (candidates == null || candidates.isEmpty()) {
            // Fall back to the email alone: the stored phone may differ in format
            // from the one supplied by the caller.
            candidates = userDao.findByUsername(userEmail);
        }

        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        User active = candidates.stream()
                .filter(u -> u.isActive() && Objects.nonNull(u.getOrg()))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(active)) {
            return null;
        }

        if (candidates.size() > 1) {
            log.info("Resolved {} of {} matching accounts for {} during payment link creation",
                    active.getId(), candidates.size(), userEmail);
        }

        return active;
    }

}
