package com.portal.procucev.Dto;

import java.util.Date;

import com.portal.procucev.model.MasterStatus;

import lombok.Data;

@Data
public class GmtRfqSellerDto {

	    private String id;
	    private String vendorUuid;
	    private String vendorName;
	    private String vendorId;
	    private MasterStatus status;
	    private String query;
	    private String otherEmails;
	    private Date quoteSubmittedDate;
	   
	}