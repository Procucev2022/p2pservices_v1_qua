package com.portal.procucev.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentLinks {

    private String payment_link_id;
    private String reference_id;
    private String status;

    private String amount;
    private String amount_paid;
    private String currency;
    private String email;
    private String phone;
    private String url;

    private String expires_at;
    private String expires_at_formatted;

    private List<Payment> payments;
}

