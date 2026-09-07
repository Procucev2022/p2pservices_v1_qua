package com.portal.procucev.rfq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedRFQ {
    private String buyerEmail;
    private String deliveryLocation;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryPincode;
    private String deliveryDate;
    private String category;
    private List<RFQItem> items;
    private TokenUsageTelemetry tokenUsage;
}
