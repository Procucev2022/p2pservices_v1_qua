package com.portal.procucev.model;


import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

/**
 * The persistent class for the organization database table.
 * 
 */
@Data
@Entity
@Table(name = "organization")
public class Organization extends Procucev {

	private static final long serialVersionUID = 1L;

	@Transient
	String masterStatus = new String();
	
	@Transient
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] boqfile;

	@Transient
	private MultipartFile file;

	@Column(name = "organization_name")
	private String companyName;

	@Column(name = "PAN")
	private String pan;

	private boolean tempapproval;
	
	@Column(name="client_refference")
	private boolean clientRefference;
	
	@Column(name = "GSTIN")
	private String gstin;
	
	private String refference;

	@Column(name = "organization_phonenumber")
	private String organizationPhonenumber;

	private String organizationcol;

	@Column(name = "address1")
	private String address1;
	
	private Date validdate;

	@Column(name = "address2")
	private String address2;

	@Column(name = "country")
	private String country;

	@Column(name = "state")
	private String state;

	@Column(name = "city")
	private String city;

	@Column(name = "zip_code")
	private String zipCode;

	@Column(name = "type")
	private String type;

	@Column(name = "TAN")
	private String tan;

	@Column(name = "MSME")
	private boolean msme;
	
	@Column(name="contact_person")
	private String contactPerson;
	
	@Column(name="sub_category")
	private String subCategory;
	
	private String website;

	@Column(name = "client_category")
	private String clientCategory;

	@Column(name = "client_vertical")
	private String clientVertical;

	@Column(name = "vendor_rank")
	private String vendorRank;
	
	@Column(name="client_sector")
	private String clientSector;

	private String others;

	@ManyToOne
	private MasterStatus vendorStatus;

	@ManyToOne
	private MasterStatus procucevStatus;
	
	@ManyToOne
	private MasterStatus clientStatus;

	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] files;

	@Column(name = "accepted_terms")
	private boolean acceptedTerms;
	
	private boolean emailsent;

	@Transient
	private String vendorEmail;
	@Transient
	private String vendorPhone;
	
	@Transient
	private String name;
	
	@Transient
	private String userOtp;

	private String vendorcategory;
	
	@Column(name="self_client")
	private boolean selfClient;
	
	@Column(name="new_client_status")
	private boolean newClientStatus;
	
	private String hsncode;
	
	@Column(name="vendor_class")
	private String vendorClass;
	
	
	private String email;
	
	@Column(name="upgrade_vendor")
	private boolean upgradeVendor;
	
	@Column(name="upgrade_start_date")
	private Date upgradeStartDate;

	@Column(name="upgrade_end_date")
	private Date upgradeEndDate;
	
	@Column(name="upgrade_days")
	private int upgradeDays;
	
	@Column(name="digi_pin")
	private String digiPin;

	
	
	/*
	 * @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	 * 
	 * @JoinColumn(name = "organization_uuid")
	 * 
	 * @JsonBackReference(value = "clientlogo") private List<clientLogo> clientLogo
	 * = new ArrayList<clientLogo>();
	 */
	/*
	 * @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	 * 
	 * @JoinColumn(name = "organization_uuid")
	 * 
	 * @JsonBackReference(value = "verticals") private List<ClientVerticals>
	 * clientVerticals = new ArrayList<ClientVerticals>();
	 */
	/*
	 * @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	 * 
	 * @JoinColumn(name = "organization_uuid")
	 * 
	 * @JsonBackReference(value = "categories") private List<ClientCategories>
	 * clientCategories = new ArrayList<ClientCategories>();
	 */
	

	// @ManyToOne(fetch = FetchType.LAZY)
	// private MasterStatus orgStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	private OrgType orgType;

	@Column(name = "license_valid_date")
	private Date licenseValidDate;

	@Column(name = "vendor_type")
	private String vendorType;

	private String eagerness;

	@Column(name = "is_authorized_distributor")
	private boolean authorizedDistributor;
	
	@Column(name="item_edit")
	private boolean itemEdit;

	@Column(name = "company_id")
	private String companyId;
	
	@Column(name="details")
	private String details;

	@Column(name="item_desc")
	private String itemDesc;
	
	@Column(name="client_vendor")
	private boolean clientVendor;
	
	@Column(name="other_emails")
	private String otherEmails;
	
	@ManyToOne
	private MasterStatus status;
	
	@Column(name="DPS_Name")
	private String dpsName;
	
	@Column(name="GMT_Name")
	private String gmtName;
	
	@Column(name="BFS_Name")
	private String bfsName;
	
	@Column(name="CAPEX_Name")
	private String capexName;
	
	@Column(name="is_india")
	private boolean india;
	
	private String crn;
	
	@Transient
	private List<String> regions;
	
	@Transient
	private List<String> subcategories;

}