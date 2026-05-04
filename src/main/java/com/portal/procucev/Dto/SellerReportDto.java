package com.portal.procucev.Dto;

import java.util.Date;

import lombok.Data;

@Data
public class SellerReportDto {

    private Date rfqDate;
    private String rfqId;
    private String sellerCompanyName;
    private String email;
    private String mobileNo;
    private String companyLocation;
    private String deliveryLocation;
    private Date lastLogin;
    private String sourceType;
    private Date rfqDownloadDate;
    private Date quoteSubmittedDate;
    private String query;

    public SellerReportDto(
            Date rfqDate,
            String rfqId,
            String sellerCompanyName,
            String email,
            String mobileNo,
            String companyLocation,
            String deliveryLocation,
            Date lastLogin,
            String sourceType,
            Date rfqDownloadDate,
            Date quoteSubmittedDate,
            String query
    ) {
        this.rfqDate = rfqDate;
        this.rfqId = rfqId;
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