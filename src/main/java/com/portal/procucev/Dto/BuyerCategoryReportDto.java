package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BuyerCategoryReportDto {

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

	    @JsonProperty("LastLoginDate")
	    private Date lastLoginDate;

	    @JsonProperty("Category1")
	    private String category1;

	    @JsonProperty("Category2")
	    private String category2;

	    @JsonProperty("Category3")
	    private String category3;

	    @JsonProperty("Category4")
	    private String category4;

	    @JsonProperty("Category5")
	    private String category5;

	    @JsonProperty("Category6")
	    private String category6;

	    @JsonProperty("Category7")
	    private String category7;

	    @JsonProperty("Category8")
	    private String category8;

	    @JsonProperty("Category9")
	    private String category9;

	    @JsonProperty("Category10")
	    private String category10;

	    // Constructor (base fields only — categories set via setters)
	    public BuyerCategoryReportDto(String buyerName, String emailId,
	                                   String mobileNo, String companyName,
	                                   String companyLocation, Date lastLoginDate) {
	        this.buyerName = buyerName;
	        this.emailId = emailId;
	        this.mobileNo = mobileNo;
	        this.companyName = companyName;
	        this.companyLocation = companyLocation;
	        this.lastLoginDate = lastLoginDate;
	    }
	

}
