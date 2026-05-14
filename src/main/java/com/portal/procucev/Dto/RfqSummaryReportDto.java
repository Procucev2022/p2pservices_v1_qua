package com.portal.procucev.Dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RfqSummaryReportDto {
	
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

	    @JsonProperty("DeliveryLocation")
	    private String deliveryLocation;

	    @JsonProperty("SellerCompanyName")
	    private String sellerCompanyName;

	    @JsonProperty("SellerMobile")
	    private String sellerMobile;

	    @JsonProperty("SellerEmailID")
	    private String sellerEmailId;

	    @JsonProperty("SellerLocation")
	    private String sellerLocation;

	    @JsonProperty("SellerCategories")
	    private String sellerCategories;

	    @JsonProperty("RFQDownloadDate")
	    private Date rfqDownloadDate;

	    @JsonProperty("RFQForwardDate")
	    private Date rfqForwardDate;

	    @JsonProperty("QuoteSubmittedDate")
	    private Date quoteSubmittedDate;

	    @JsonProperty("Query")
	    private String query;

	    // Constructor for JPQL
	    public RfqSummaryReportDto(Date rfqDate, String rfqId, String buyerName,
	                               String emailId, String mobileNo, String companyName,
	                               String deliveryLocation, String sellerCompanyName,
	                               String sellerMobile, String sellerEmailId,
	                               String sellerLocation, String sellerCategories,
	                               Date rfqDownloadDate, Date rfqForwardDate,
	                               Date quoteSubmittedDate, String query) {
	        this.rfqDate = rfqDate;
	        this.rfqId = rfqId;
	        this.buyerName = buyerName;
	        this.emailId = emailId;
	        this.mobileNo = mobileNo;
	        this.companyName = companyName;
	        this.deliveryLocation = deliveryLocation;
	        this.sellerCompanyName = sellerCompanyName;
	        this.sellerMobile = sellerMobile;
	        this.sellerEmailId = sellerEmailId;
	        this.sellerLocation = sellerLocation;
	        this.sellerCategories = sellerCategories;
	        this.rfqDownloadDate = rfqDownloadDate;
	        this.rfqForwardDate = rfqForwardDate;
	        this.quoteSubmittedDate = quoteSubmittedDate;
	        this.query = query;
	    }

}
