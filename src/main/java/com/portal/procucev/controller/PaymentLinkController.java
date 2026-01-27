package com.portal.procucev.controller;

import com.portal.procucev.model.PaymentLink;
import com.portal.procucev.service.PaymentLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/api/payments")
@RequiredArgsConstructor
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;

    @PostMapping("/link/{planId}")
    public PaymentLink generatePaymentLink(
            @PathVariable Long planId,
            @RequestParam String customerName,
            @RequestParam String customerEmail
    ) {

        String redirectUrl = "https://procucev.com/payment-success";
        String webhookUrl = "https://procucev.com/api/zoho/webhook";

        return paymentLinkService.createPaymentLink(planId, "chrnjvr@gmail.com", "+91 8125492449", redirectUrl);
    }
}
