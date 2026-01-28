package com.portal.procucev.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;



@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoPaymentLinkWebhookRequest {

    private Long event_id;
    private String event_type;
    private Long account_id;
    private Boolean live_mode;
    private Long event_time;
    private EventObject event_object;
}
