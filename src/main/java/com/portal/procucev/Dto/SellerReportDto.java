package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SellerReportDto {

    @JsonProperty("RfqDate")
    private Date rfqDate;

    @JsonProperty("RfqId")
    private String rfqId;

    @JsonProperty("Category")
    private String category;

    @JsonProperty("SellerCompanyName")
    private String sellerCompanyName;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("MobileNo")
    private String mobileNo;

    @JsonProperty("CompanyLocation")
    private String companyLocation;

    @JsonProperty("DeliveryLocation")
    private String deliveryLocation;

    @JsonProperty("LastLogin")
    private Date lastLogin;

    @JsonProperty("SourceType")
    private String sourceType;

    @JsonProperty("RfqDownloadDate")
    private Date rfqDownloadDate;

    @JsonProperty("QuoteSubmittedDate")
    private Date quoteSubmittedDate;

    @JsonProperty("Query")
    private String query;

    public SellerReportDto(Date rfqDate,
                           String rfqId,
                           String category,
                           String sellerCompanyName,
                           String email,
                           String mobileNo,
                           String companyLocation,
                           String deliveryLocation,
                           Date lastLogin,
                           String sourceType,
                           Date rfqDownloadDate,
                           Date quoteSubmittedDate,
                           String query) {

        this.rfqDate = rfqDate;
        this.rfqId = rfqId;
        this.category = category;
        this.sellerCompanyName = sellerCompanyName;
        this.email = email;
        this.mobileNo = mobileNo;
        this.companyLocation = companyLocation;
        this.deliveryLocation = deliveryLocation;
        this.lastLogin = lastLogin;
        this.sourceType = sourceType;
        this.rfqDownloadDate = rfqDownloadDate;
        this.quoteSubmittedDate = quoteSubmittedDate;
        this.query = query;
    }
}