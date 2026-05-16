package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SellerSubscriptionReportDto {
	
	@JsonProperty("LoginDateTime")
    private Date loginDateTime;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("MobileNo")
    private String mobileNo;

    @JsonProperty("CompanyName")
    private String companyName;

    @JsonProperty("EmailID")
    private String emailId;

    @JsonProperty("CompanyLocation")
    private String companyLocation;

    @JsonProperty("Subscribed")
    private String subscribed;

    @JsonProperty("RFQsDownloaded")
    private Long rfqsDownloaded;

    @JsonProperty("RFQID")
    private String rfqId;

    public SellerSubscriptionReportDto(Date loginDateTime, String name,
                                        String mobileNo, String companyName,
                                        String emailId, String companyLocation,
                                        String subscribed, Long rfqsDownloaded,
                                        String rfqId) {
        this.loginDateTime = loginDateTime;
        this.name = name;
        this.mobileNo = mobileNo;
        this.companyName = companyName;
        this.emailId = emailId;
        this.companyLocation = companyLocation;
        this.subscribed = subscribed;
        this.rfqsDownloaded = rfqsDownloaded;
        this.rfqId = rfqId;
    }

}
