package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BuyerReportDto {

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

	    @JsonProperty("LastLogin")
	    private Date lastLogin;

	    @JsonProperty("RFQSource")
	    private String rfqSource;

	    @JsonProperty("RFQQueriesCount")
	    private Long rfqQueriesCount;

	    // Constructor for JPQL
	    public BuyerReportDto(Date rfqDate, String rfqId, String buyerName,
	                          String emailId, String mobileNo, String companyName,
	                          String companyLocation, String itemDescription,
	                          String itemCategory, String deliveryLocation,
	                          Date lastLogin, String rfqSource, Long rfqQueriesCount) {
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
	        this.lastLogin = lastLogin;
	        this.rfqSource = rfqSource;
	        this.rfqQueriesCount = rfqQueriesCount;
	    }
	

}
