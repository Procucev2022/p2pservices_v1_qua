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
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/rest/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;

    @PostMapping("/link/generate")
    public PaymentLink generatePaymentLink(@RequestBody PaymentLinkGenerateRequest request) {

        String redirectUrl = "https://procucev.com/payment-success";

        if (Objects.isNull(request.getPlanId())) {
            throw new AppException("Plan ID is required");
        }
        if (Objects.isNull(request.getUserPhone()) || StringUtils.isEmpty(request.getUserPhone())) {
            throw new AppException("User phone number is required");
        }
        if (Objects.isNull(request.getUserEmail()) || StringUtils.isEmpty(request.getUserEmail())) {
            throw new AppException("User email is required");
        }

        List<String> invalidEmails = new ArrayList<>();
        EmailValidatorUtil.validateEmail(request.getUserEmail(), invalidEmails);

        if (!invalidEmails.isEmpty()) {
            log.info("Invalid email found: {}", invalidEmails);
            throw new AppException("Invalid email address: " + invalidEmails.get(0));
        }

        return paymentLinkService.createPaymentLink(request.getPlanId(), PhoneNumberUtils.normalize(request.getUserPhone()), request.getUserEmail(), redirectUrl);
    }
}
