package com.portal.procucev.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PaymentLinkGenerateRequest {

    private Long planId;
    private String userPhone;
    private String userEmail;
}
