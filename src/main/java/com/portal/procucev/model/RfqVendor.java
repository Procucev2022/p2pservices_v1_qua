package com.portal.procucev.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The persistent class for the rfq_vendors database table.
 * 
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "rfq_vendors")
public class RfqVendor extends Procucev {
	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	private Organization organization;

	@Column(name = "is_rfq_notified")
	private byte isRfqNotified;
	
	@Column(name="quotation_received")
	private boolean quotationReceived;

	/**
	 * Owning side back-reference. Excluded from toString/equals/hashCode to avoid
	 * infinite recursion with {@link Rfq#getRfqVendor()}.
	 */
	@ManyToOne
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Rfq rfq;
	
	@Transient
	private MasterStatus status;
	
	@Transient
	private String companyName;
	
	@Transient
	private String email;
	
	@Transient
	private String phone;
	
	@Transient
	private String desc;
	
	@Transient
	private String vendorId;
	
	@Transient
	private String companyId;
	
	@Transient
	private String otherEmails;
	
	@Transient
	private boolean newCommentAvailableProcucev;


	@Transient
	private boolean newCommentAvailableVendor;


	@Transient
	private String vendorType;

	@Transient
	private String vendorCode;

	@Transient
	private String city;

	@Transient
	private String sourcingMatchStatus;

	@Transient
	private Date submissionDate;

	@Column(name = "rfq_notified_to")
	private String rfqNotifiedTo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JsonBackReference(value = "user_rfqVendor")
	private User user;

	@Column(name = "rfq_id")
	private String rfqId;
	
	@Transient
	private String rfquuid;

	// @Column(name = "procucev_status")
	@ManyToOne(fetch = FetchType.LAZY)
	private MasterStatus procucevStatus;

	// @Column(name = "vendor_status")
	@ManyToOne(fetch = FetchType.LAZY)
	private MasterStatus vendorStatus;

	@Transient
	private Date rfqClosingDate;
	
	@Column(name="vendor_response_date")
	private Date vendorResponseDate;
	
	@Column(name="cm_user")
	private String cmUser;

	@Transient
	private String rfqStatus;

	@Transient
	private String numberOfItems;
	
	@Column(name="request_type")
	private String requestType;

	public RfqVendor(String companyName, String vendorId, String companyId) {
		super();
		this.companyName = companyName;
		this.vendorId = vendorId;
		this.companyId = companyId;
	}

}