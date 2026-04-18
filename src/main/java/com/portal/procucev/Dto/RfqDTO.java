package com.portal.procucev.Dto;

import java.util.Date;

import com.portal.procucev.model.MasterStatus;

public class RfqDTO {

	private String id;

	private String rfqId;

	private String projectDesc;

	private String category;

	private String createdBy;

	private String companyName;
	
	private String companyId;
	
	private String phoneNumber;
	
	private boolean quotationReceived;
	
	private int count;

	private MasterStatus clientStatus;

	private String division;

	private Date createdTs;
	
	private boolean byClient;
	

	private Date deliveryDate;

	private int noOfQuotes;

	private long noOfVendors;
	
	private Date quoteSubmittedDate;

	private boolean newCommentAvailableVendor;
	
	//Added Soucrce Type
	private String sourceType;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRfqId() {
		return rfqId;
	}

	public void setRfqId(String rfqId) {
		this.rfqId = rfqId;
	}

	public String getProjectDesc() {
		return projectDesc;
	}

	public void setProjectDesc(String projectDesc) {
		this.projectDesc = projectDesc;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedTs() {
		return createdTs;
	}

	public void setCreatedTs(Date createdTs) {
		this.createdTs = createdTs;
	}

	public int getNoOfQuotes() {
		return noOfQuotes;
	}

	public void setNoOfQuotes(int noOfQuotes) {
		this.noOfQuotes = noOfQuotes;
	}

	public long getNoOfVendors() {
		return noOfVendors;
	}

	public void setNoOfVendors(long noOfVendors) {
		this.noOfVendors = noOfVendors;
	}

	public MasterStatus getClientStatus() {
		return clientStatus;
	}

	public void setClientStatus(MasterStatus clientStatus) {
		this.clientStatus = clientStatus;
	}

	public String getDivision() {
		return division;
	}

	public void setDivision(String division) {
		this.division = division;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public Date getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public boolean isByClient() {
		return byClient;
	}

	public void setByClient(boolean byClient) {
		this.byClient = byClient;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public boolean isQuotationReceived() {
		return quotationReceived;
	}

	public void setQuotationReceived(boolean quotationReceived) {
		this.quotationReceived = quotationReceived;
	}

	public String getCompanyId() {
		return companyId;
	}

	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	public Date getQuoteSubmittedDate() {
		return quoteSubmittedDate;
	}

	public void setQuoteSubmittedDate(Date quoteSubmittedDate) {
		this.quoteSubmittedDate = quoteSubmittedDate;
	}

	public boolean isNewCommentAvailableVendor() {
		return newCommentAvailableVendor;
	}

	public void setNewCommentAvailableVendor(boolean newCommentAvailableVendor) {
		this.newCommentAvailableVendor = newCommentAvailableVendor;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	
}
