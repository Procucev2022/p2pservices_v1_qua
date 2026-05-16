package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BuyerSellerReportDto {
	
	 @JsonProperty("LoginDateTime")
	    private Date loginDateTime;

	    @JsonProperty("BuyerSeller")
	    private String buyerSeller;

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

	    @JsonProperty("GMTBFS")
	    private String gmtBfs;

	    @JsonProperty("RFQID")
	    private String rfqId;

	    @JsonProperty("ItemSearch")
	    private String itemSearch;

	    public BuyerSellerReportDto(Date loginDateTime, String buyerSeller,
	                                 String name, String mobileNo,
	                                 String companyName, String emailId,
	                                 String companyLocation, String gmtBfs,
	                                 String rfqId, String itemSearch) {
	        this.loginDateTime = loginDateTime;
	        this.buyerSeller = buyerSeller;
	        this.name = name;
	        this.mobileNo = mobileNo;
	        this.companyName = companyName;
	        this.emailId = emailId;
	        this.companyLocation = companyLocation;
	        this.gmtBfs = gmtBfs;
	        this.rfqId = rfqId;
	        this.itemSearch = itemSearch;
	    }

}
