package com.portal.procucev.Dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class ZohoPaymentLinkRequest {

    private BigDecimal amount;
    private String currency;        // INR
    private String email;
    private String description;
    private String phone;
    private String reference_id;
    private String expires_at;      // ISO datetime
    private Boolean notify_user;
    private String return_url;
}

