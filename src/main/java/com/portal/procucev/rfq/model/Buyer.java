package com.portal.procucev.rfq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Buyer {
    private Long id;
    private String email;
    private String name;
    private String orgId;
    private String userId;
    private String companyName;
    private String contactPerson;
    private String phone;
    private String city;
    private String state;
    private String pincode;
    private String address;
    private String token;
    private boolean verified;
}
