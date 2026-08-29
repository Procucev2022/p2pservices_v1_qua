package com.portal.procucev.controller;

import com.portal.procucev.Dto.PaymentLinkGenerateRequest;
import com.portal.procucev.customexception.AppException;
import com.portal.procucev.model.PaymentLink;
import com.portal.procucev.service.PaymentLinkService;
import com.portal.procucev.utils.EmailValidatorUtil;
import com.portal.procucev.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/rest/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class PaymentLinkController {

    @Value("${zoho.payments.redirect.url}")
    private String redirectUrl;

    private final PaymentLinkService paymentLinkService;

    @PostMapping("/link/generate")
    public PaymentLink generatePaymentLink(@RequestBody PaymentLinkGenerateRequest request) {

       // String redirectUrl = "https://procucev.com/payment-success";


        if (Objects.isNull(request.getPlanId())) {
            throw new AppException("Plan ID is required");
        }
        if (Objects.isNull(request.getUserPhone()) || StringUtils.isEmpty(request.getUserPhone())) {
            throw new AppException("User phone number is required");
        }
        if (Objects.isNull(request.getUserEmail()) || StringUtils.isEmpty(request.getUserEmail())) {
            throw new AppException("User email is required");
        }

        // Only malformed addresses are rejected. An address on a domain that
        // cannot receive mail is allowed through: checkout completes in the
        // browser via the returned payment URL, so a missing receipt email must
        // not stop the user from paying. The gap is logged for follow-up.
        if (!EmailValidatorUtil.isFormatValid(request.getUserEmail())) {
            throw new AppException("Invalid email address: " + request.getUserEmail());
        }

        if (!EmailValidatorUtil.isDeliverable(request.getUserEmail())) {
            log.warn("Payment link requested for undeliverable email {}. Proceeding, "
                    + "but the Zoho notification will not arrive.", request.getUserEmail());
        }

        return paymentLinkService.createPaymentLink(request.getPlanId(), PhoneNumberUtils.normalize(request.getUserPhone()), request.getUserEmail(), redirectUrl+"/categorymgr/payment-success");
    }
}
