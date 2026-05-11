package com.portal.procucev.Dto;

import java.util.Date;

import lombok.Data;

@Data
public class SellerSummaryDto {

    private Integer sNo;
    private String sellerCompanyName;
    private String sellerMobileNo;
    private String emailId;
    private String companyLocation;
    private String vendorClass;
    private String subscribed; // Yes/No
    private Date subscriptionDate;
    private String subscriptionPlan;
    private Date subscriptionExpiryDate;
    private Integer rfqBalance;
    private Date lastLoginDate;
    private Integer rfqDownloaded;
    private Integer quoteSubmitted;
    private Integer rfqsPendingForQuotes;
    private Integer sellerCategoryRfqCount;
    private String registrationSource;

    // Default constructor (required)
    public SellerSummaryDto() {
    }

    // ✅ Constructor matching JPQL query EXACTLY
    public SellerSummaryDto(
            String companyName,
            String organizationPhonenumber,
            String email,
            String city,
            String vendorClass,
            String subscribed,
            Date subscriptionStart,
            String planName,
            Date subscriptionExpiry,
            Integer rfqCredits,
            Date updatedTS,
            Long rfqUsedCount,
            Long quoteSubmitted,
            Long pending,
            Integer categoryCount,
            String sourceType
    ) {
        this.sellerCompanyName = companyName;
        this.sellerMobileNo = organizationPhonenumber;
        this.emailId = email;
        this.companyLocation = city;
        this.vendorClass = vendorClass;
        this.subscribed = subscribed;
        this.subscriptionDate = subscriptionStart;
        this.subscriptionPlan = planName;
        this.subscriptionExpiryDate = subscriptionExpiry;
        this.rfqBalance = rfqCredits;
        this.lastLoginDate = updatedTS;

        // Handle Long → Integer safely
        this.rfqDownloaded = rfqUsedCount != null ? rfqUsedCount.intValue() : 0;
        this.quoteSubmitted = quoteSubmitted != null ? quoteSubmitted.intValue() : 0;
        this.rfqsPendingForQuotes = pending != null ? pending.intValue() : 0;

        this.sellerCategoryRfqCount = categoryCount;
        this.registrationSource = sourceType;
    }
}