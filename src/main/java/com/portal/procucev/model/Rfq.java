package com.portal.procucev.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * The persistent class for the rfq database table.
 * 
 */

@Data
@Entity
@Table(name = "rfq_header")
public class Rfq extends Procucev {
	private static final long serialVersionUID = 1L;

	@Column(name = "special_instruction")
	private String specialInstruction;

	@Column(name = "rfq_closing_date")
	private Date rfqClosingDate;

	@ManyToOne(fetch = FetchType.LAZY)
	private Organization org;

	@Column(name="by_client")
	private boolean byClient;
	
	private String division;
	
	@Column(name="quotation_received")
	private boolean quotationReceived;
	
	private boolean isWebApp;
	
	private boolean isWhatsApp;
	
	private boolean isBot;
	
	private String user;
	
	private int count;
	
	@Transient
	private boolean fromClient;

	@OrderBy("serialNo ASC")
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "rfq_uuid")
	private List<RfqItem> rfqItem = new ArrayList<RfqItem>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "rfq_uuid")
	@JsonBackReference(value = "rfqVendor")
	private List<RfqVendor> rfqVendor = new ArrayList<RfqVendor>();

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "rfq_uuid")
	private List<RFQDocument> rfqDocument = new ArrayList<RFQDocument>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "rfq_uuid")
	private List<ClientDeliveryLocationRfq> clientdeliverylocationrfq = new ArrayList<ClientDeliveryLocationRfq>();

	@Column(name = "project_desc")
	private String projectDesc;

	private String category;

	@Column(name = "delivery_date")
	private Date deliveryDate;

	@Column(name = "no_pr_flag")
	private boolean noPrFlag;

	@ManyToOne(fetch = FetchType.LAZY)
	private MasterStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	private MasterStatus rfqStatus;
	

	@ManyToOne(fetch = FetchType.LAZY)
	private MasterStatus clientStatus;
	
	@Column(name = "boq_filename")
	private String boqFileName;

	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] boqfile;
	
	@Transient
	private List<Organization> vendors;

	@Transient
	int numberOfItems;

	@Transient
	private String prId;

	@Column(name = "new_comment_available_procucev")
	private boolean newCommentAvailableProcucev;

	@Column(name = "new_comment_available_vendor")
	private boolean newCommentAvailableVendor;

	@Column(name = "rfq_id")
	private String rfqId;

	private String description;

}