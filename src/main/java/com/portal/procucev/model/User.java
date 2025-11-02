package com.portal.procucev.model;



import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name = "user")
public class User extends Procucev {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Column
	private String username;
	@Column
	private String password;

	private String email;
	
	@Column(name = "first_name")
	private String firstName;
	
	@ManyToOne
	private MasterStatus clientStatus;

	@Column(name = "last_name")
	private String lastName;

	private String phone;
	
	@Column(name="self_client")
	private boolean selfClient;

	@Column(name = "full_name")
	private String fullName;

	@ManyToOne(fetch = FetchType.LAZY)
	private Organization org;

	@ManyToOne(fetch = FetchType.EAGER)
	private Role role;

	@Column(name = "dept_name")
	private String deptName;

	@Column(name = "is_active")
	private boolean active;
	
	@Column(name = "reset_password")
	private boolean resetPassword = true;
	
	@Transient
	private boolean emailMatched;
	
	@Transient
	private String subject;
	
	@Transient
	private String message;
	
	@Transient
	private String companyName;
	
	@Transient
	private String rfqId;
	
	@Transient
	private String orgId;
	
	@Column(name="unique_id")
	private String uniqueId;
	
	@Column(name = "activity_ts")
	private Date activityTs;
	
	@Column(name="bfs_group")
	private String bfsGroup;
	
//	@ManyToMany(fetch = FetchType.EAGER)
//	@JoinTable(name = "user_permission", joinColumns = {
//			@JoinColumn(referencedColumnName = "uuid") }, inverseJoinColumns = {
//					@JoinColumn(referencedColumnName = "uuid") })
//	private List<Permission> ownPermissionList = new ArrayList<Permission>();
	
	@Transient
	private List<String> listofPermission = new ArrayList<String>();
	
	@Transient
	private boolean isAuth;
	
	private boolean isWebApp;
	
	private boolean isWhatsApp;
	
	private boolean isBot;
	
	private boolean isApproved;
	
	@Column(name="source_type")
	private String sourceType;
	
	@Transient
	private String zipCode;
	
	@Transient
	private List<String> ownPermissions = new ArrayList<String>();
	
	@JsonIgnore
	public String getPassword() {
	    return password;
	}
	
	@Column(name="verification_status")
	private String verificationStatus;
	

	public User(String id,Date createdTS,String username, MasterStatus clientStatus, String phone, boolean selfClient, String fullName,
			boolean active) {
		super();
		this.id=id;
		this.createdTS=createdTS;
		this.username = username;
		this.clientStatus = clientStatus;
		this.phone = phone;
		this.selfClient = selfClient;
		this.fullName = fullName;
		this.active = active;
	}
	public User(String id,Date createdTS,String username, String phone,  String fullName) {
		super();
		this.id=id;
		this.createdTS=createdTS;
		this.username = username;
		this.phone = phone;
		this.fullName = fullName;
	}


	public User(String id,String username, String phone,String companyName, String fullName, String orgId, String uniqueId, Date activityTs, boolean isWebApp, boolean isWhatsApp,boolean isBot,String zipCode,MasterStatus clientStatus,Date createdTS,String sourceType) {
		super();
		this.id =id;
		this.username = username;
		this.phone = phone;
		this.companyName=companyName;
		this.fullName = fullName;
		this.orgId = orgId;
		this.uniqueId = uniqueId;
		this.activityTs = activityTs;
		this.isWebApp=isWebApp;
		this.isWhatsApp=isWhatsApp;
		this.isBot=isBot;
		this.zipCode=zipCode;
		this.clientStatus=clientStatus;
		this.createdTS=createdTS;
		this.sourceType=sourceType;
	}

	public User(String id,String username, String phone,String companyName, String fullName, String orgId, String uniqueId, Date activityTs, boolean isWebApp, boolean isWhatsApp,boolean isBot,String zipCode,boolean isApproved,boolean selfClient, boolean active,String sourceType,String verificationStatus) {
		super();
		this.id =id;
		this.username = username;
		this.phone = phone;
		this.companyName=companyName;
		this.fullName = fullName;
		this.orgId = orgId;
		this.uniqueId = uniqueId;
		this.activityTs = activityTs;
		this.isWebApp=isWebApp;
		this.isWhatsApp=isWhatsApp;
		this.isBot=isBot;
		this.zipCode=zipCode;
		this.isApproved=isApproved;
		this.selfClient=selfClient;
		this.active=active;
		this.sourceType=sourceType;
		this.verificationStatus=verificationStatus;
	}


	public User(String username, String phone, String companyName,String fullName) {
		super();
		this.username = username;
		this.phone = phone;
		this.companyName = companyName;
		this.fullName = fullName;
	}




	
	
}
