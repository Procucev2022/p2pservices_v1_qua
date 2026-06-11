package com.portal.procucev.Dto;

import java.util.Date;

import com.portal.procucev.model.MasterStatus;

import lombok.Data;

@Data
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

//	private MasterStatus clientStatus;
	 private String clientStatusId;
     private String clientStatusName;
	
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
	
	public RfqDTO() {
		
	}
	
	
	public RfqDTO(String id, String rfqId, String projectDesc, String category, String createdBy, String companyName,
			String companyId, String phoneNumber, boolean quotationReceived, int count, String clientStatusId,
			String clientStatusName, String division, Date createdTs, boolean byClient, Date deliveryDate,
			int noOfQuotes, long noOfVendors, Date quoteSubmittedDate, boolean newCommentAvailableVendor,
			String sourceType) {
		super();
		this.id = id;
		this.rfqId = rfqId;
		this.projectDesc = projectDesc;
		this.category = category;
		this.createdBy = createdBy;
		this.companyName = companyName;
		this.companyId = companyId;
		this.phoneNumber = phoneNumber;
		this.quotationReceived = quotationReceived;
		this.count = count;
		this.clientStatusId = clientStatusId;
		this.clientStatusName = clientStatusName;
		this.division = division;
		this.createdTs = createdTs;
		this.byClient = byClient;
		this.deliveryDate = deliveryDate;
		this.noOfQuotes = noOfQuotes;
		this.noOfVendors = noOfVendors;
		this.quoteSubmittedDate = quoteSubmittedDate;
		this.newCommentAvailableVendor = newCommentAvailableVendor;
		this.sourceType = sourceType;
	}
	
	
	
	
	
}
