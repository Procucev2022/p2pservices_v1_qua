package com.portal.procucev.rfq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedRfqRequest {
    private String buyerEmail;
    private String buyerName;
    private String rawSubject;
    private String description;
    private String partNumber;
    private String specification;
    private String brand;
    private String quantity;
    private String uom;
    private String deliveryLocation;
    private String deliveryDate;
    private String reasonForFailure;
    private String processingReference;
}
