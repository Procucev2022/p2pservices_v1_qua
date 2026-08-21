package com.portal.procucev.Dto;

import java.util.Date;

import com.portal.procucev.model.MasterStatus;

public class ClientRFQDto {

	private String id;
	
	private String projectDesc;

	private String rfqId;
	
	private String division;
	
	private String user;
	
	private boolean quotationReceived
	;
	private MasterStatus clientStatus;
	
	private String  createdBy;
	
	private Date createdTS;
	
	private int quoteCount;
	
	private Date quoteSubmittedDate;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProjectDesc() {
		return projectDesc;
	}

	public void setProjectDesc(String projectDesc) {
		this.projectDesc = projectDesc;
	}

	public String getRfqId() {
		return rfqId;
	}

	public void setRfqId(String rfqId) {
		this.rfqId = rfqId;
	}

	public String getDivision() {
		return division;
	}

	public void setDivision(String division) {
		this.division = division;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public MasterStatus getClientStatus() {
		return clientStatus;
	}

	public void setClientStatus(MasterStatus clientStatus) {
		this.clientStatus = clientStatus;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedTS() {
		return createdTS;
	}

	public void setCreatedTS(Date createdTS) {
		this.createdTS = createdTS;
	}

	public boolean isQuotationReceived() {
		return quotationReceived;
	}

	public void setQuotationReceived(boolean quotationReceived) {
		this.quotationReceived = quotationReceived;
	}

	public int getQuoteCount() {
		return quoteCount;
	}

	public void setQuoteCount(int quoteCount) {
		this.quoteCount = quoteCount;
	}

	public Date getQuoteSubmittedDate() {
		return quoteSubmittedDate;
	}

	public void setQuoteSubmittedDate(Date quoteSubmittedDate) {
		this.quoteSubmittedDate = quoteSubmittedDate;
	}

	@com.fasterxml.jackson.annotation.JsonProperty("displayRfqId")
	public String getDisplayRfqId() {
		return com.portal.procucev.rfq.util.CommonUtil.formatRfqDisplayNumber(this.rfqId);
	}

	@com.fasterxml.jackson.annotation.JsonProperty("displayRfqNumber")
	public String getDisplayRfqNumber() {
		return com.portal.procucev.rfq.util.CommonUtil.formatRfqDisplayNumber(this.rfqId);
	}

	@com.fasterxml.jackson.annotation.JsonProperty("rfqIcon")
	public String getRfqIcon() {
		return com.portal.procucev.rfq.util.CommonUtil.getRfqIcon(this.rfqId);
	}
}
