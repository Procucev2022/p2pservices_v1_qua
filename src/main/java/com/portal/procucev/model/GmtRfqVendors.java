package com.portal.procucev.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="gmt_rfq_vendors")
public class GmtRfqVendors extends Procucev {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@ManyToOne
	private Organization vendor;
	
	@ManyToOne
	private Rfq rfq;
	
	@Column(name="quotation_received")
	private boolean quotationReceived;
	
	@Transient
	private String vendorUuid;
	
	@Transient
	private String vendorName;
	
	@Transient
	private String otherEmails;
	
	@Transient
	private String vendorId;
	
	@ManyToOne
	private MasterStatus status;
	
	@Column(name="requested_date")
	private Date requestedDate;
	
	@Column(name="accepted_date")
	private Date acceptedDate;
	
	private String query;
	
	public GmtRfqVendors(String id,String vendorUuid, String vendorName, String vendorId, MasterStatus status,String query,String otherEmails) {
		super();
		this.id=id;
		this.vendorUuid = vendorUuid;
		this.vendorName = vendorName;
		this.vendorId = vendorId;
		this.status = status;
		this.query=query;
		this.otherEmails=otherEmails;
	}
	
	
	
}
