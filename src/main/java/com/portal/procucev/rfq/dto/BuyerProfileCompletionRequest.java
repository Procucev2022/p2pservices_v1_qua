package com.portal.procucev.rfq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerProfileCompletionRequest {
    private String email;
    private String fullName;
    private String companyName;
    private String phone;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String pincode;
    private String gstin;
}
