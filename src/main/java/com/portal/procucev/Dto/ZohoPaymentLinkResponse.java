package com.portal.procucev.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoPaymentLinkResponse {

    private int code;
    private String message;

    @JsonProperty("payment_links")
    private PaymentLinks paymentLinks;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentLinks {
        private String payment_link_id;
        private String url;
        private String status;
        private String amount;
        private String amount_paid;
        private String expires_at;
        private String reference_id;
    }
}
