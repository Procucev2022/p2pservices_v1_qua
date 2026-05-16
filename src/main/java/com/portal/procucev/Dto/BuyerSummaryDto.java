package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BuyerSummaryDto {

	    @JsonProperty("BuyerName")
	    private String buyerName;

	    @JsonProperty("BuyerMobileNo")
	    private String buyerMobileNo;

	    @JsonProperty("CompanyName")
	    private String companyName;

	    @JsonProperty("EmailID")
	    private String emailId;

	    @JsonProperty("CompanyLocation")
	    private String companyLocation;

	    @JsonProperty("NumberOfRFQs")
	    private Long numberOfRFQs;

	    @JsonProperty("DaysSinceLastRFQ")
	    private Long daysSinceLastRFQ;

	    @JsonProperty("RegisteredCategories")
	    private String registeredCategories;

	    @JsonProperty("Last10RFQCategories")
	    private String last10RFQCategories;

	    @JsonProperty("LastLoginDate")
	    private Date lastLoginDate;

	    @JsonProperty("DaysSinceLastLogin")
	    private Long daysSinceLastLogin;

	    @JsonProperty("RegistrationSource")
	    private String registrationSource;

	    @JsonProperty("TotalQuotes")
	    private Long totalQuotes;

	    @JsonProperty("TotalSellersRFQsSentDownloaded")
	    private Long totalSellersRFQsSentDownloaded;

	    // Constructor for JPQL
	    public BuyerSummaryDto(String buyerName, String buyerMobileNo,
	                           String companyName, String emailId,
	                           String companyLocation, Long numberOfRFQs,
	                           Long daysSinceLastRFQ, String registeredCategories,
	                           String last10RFQCategories, Date lastLoginDate,
	                           Long daysSinceLastLogin, String registrationSource,
	                           Long totalQuotes, Long totalSellersRFQsSentDownloaded) {
	        this.buyerName = buyerName;
	        this.buyerMobileNo = buyerMobileNo;
	        this.companyName = companyName;
	        this.emailId = emailId;
	        this.companyLocation = companyLocation;
	        this.numberOfRFQs = numberOfRFQs;
	        this.daysSinceLastRFQ = daysSinceLastRFQ;
	        this.registeredCategories = registeredCategories;
	        this.last10RFQCategories = last10RFQCategories;
	        this.lastLoginDate = lastLoginDate;
	        this.daysSinceLastLogin = daysSinceLastLogin;
	        this.registrationSource = registrationSource;
	        this.totalQuotes = totalQuotes;
	        this.totalSellersRFQsSentDownloaded = totalSellersRFQsSentDownloaded;
	    }
	

}
