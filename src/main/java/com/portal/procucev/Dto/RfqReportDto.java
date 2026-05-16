package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RfqReportDto {
	
	@JsonProperty("RFQDate")
    private Date rfqDate;

    @JsonProperty("RFQID")
    private String rfqId;

    @JsonProperty("BuyerName")
    private String buyerName;

    @JsonProperty("EmailID")
    private String emailId;

    @JsonProperty("MobileNo")
    private String mobileNo;

    @JsonProperty("CompanyName")
    private String companyName;

    @JsonProperty("CompanyLocation")
    private String companyLocation;

    @JsonProperty("ItemDescription")
    private String itemDescription;

    @JsonProperty("ItemCategory")
    private String itemCategory;

    @JsonProperty("DeliveryLocation")
    private String deliveryLocation;

    @JsonProperty("TotalVendors")
    private Long totalVendors;

    @JsonProperty("NumberOfQuotesReceived")
    private Long numberOfQuotesReceived;

    @JsonProperty("FirstQuoteDate")
    private Date firstQuoteDate;

    // Constructor for JPQL
    public RfqReportDto(Date rfqDate, String rfqId, String buyerName,
                        String emailId, String mobileNo, String companyName,
                        String companyLocation, String itemDescription,
                        String itemCategory, String deliveryLocation,
                        Long totalVendors, Long numberOfQuotesReceived,
                        Date firstQuoteDate) {
        this.rfqDate = rfqDate;
        this.rfqId = rfqId;
        this.buyerName = buyerName;
        this.emailId = emailId;
        this.mobileNo = mobileNo;
        this.companyName = companyName;
        this.companyLocation = companyLocation;
        this.itemDescription = itemDescription;
        this.itemCategory = itemCategory;
        this.deliveryLocation = deliveryLocation;
        this.totalVendors = totalVendors;
        this.numberOfQuotesReceived = numberOfQuotesReceived;
        this.firstQuoteDate = firstQuoteDate;
    }

}
