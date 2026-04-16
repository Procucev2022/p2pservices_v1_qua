package com.portal.procucev.Dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorSummaryResponse {
	private String id;
	private String companyId;
	private String companyName;
    private String name;
    private String subscribed;   // Yes/No
    private Long rfqsCreated;
    private Long rfqsConsumed;
    private Date subscriptionExpiry;
    private String vendorClass;
    private Date lastLogin;
    private Long quotesSubmitted;
    private String error;
    private String email;
    private String fullName;
    private String pincode;
    private String gst;
    private String details;
    private String phoneNumber;
    private String sourceType;
    private Date createdTS;
}
