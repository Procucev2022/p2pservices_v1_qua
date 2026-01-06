package com.portal.procucev.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="bfs_users")
public class BFSUsers extends Procucev {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Column(name = "buy_price")
	private double buyPrice;

	private double discount;
	
	private int quantity;

	@Column(name = "ask_price")
	private double askPrice;
	
	@ManyToOne(fetch = FetchType.LAZY)
	private Organization org;

	@ManyToOne
	private MasterStatus status;
	
	@ManyToOne
	private BFSItems items;
	
	@Transient
	private String companyName;
	
	@Transient
	private String email;
	
	@Transient
	private String city;
	
	@Transient
	private String userId;
	
	@Column(name="cm_remarks")
	private String cmRemarks;
	
	@Column(name="seller_remarks")
	private String sellerRemarks;
	
	@Column(name="buyer_remarks")
	private String buyerRemarks;
	
	@Transient
	private boolean approval;
	
	@Column(name="unique_id")
	private String uniqueId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	private User user;
	
	@Column(name="buyer_phone")
	private String buyerPhone;
	
	@Column(name="seller_phone")
	private String sellerPhone;


	public BFSUsers(String id,Date createdTS,double buyPrice, double discount, int quantity, double askPrice, MasterStatus status,
			String companyName, String email, String city,String cmRemarks,String sellerRemarks,String buyerRemarks,String userId) {
		super();
		this.id=id;
		this.createdTS=createdTS;
		this.buyPrice = buyPrice;
		this.discount = discount;
		this.quantity = quantity;
		this.askPrice = askPrice;
		this.status = status;
		this.companyName = companyName;
		this.email = email;
		this.city = city;
		this.cmRemarks=cmRemarks;
		this.sellerRemarks=sellerRemarks;
		this.buyerRemarks=buyerRemarks;
		this.userId=userId;
	}
	
	public BFSUsers(String id,Date createdTS,double buyPrice, double discount, int quantity, double askPrice, MasterStatus status,
			String cmRemarks,String sellerRemarks,String buyerRemarks) {
		super();
		this.id=id;
		this.createdTS=createdTS;
		this.buyPrice = buyPrice;
		this.discount = discount;
		this.quantity = quantity;
		this.askPrice = askPrice;
		this.status = status;
		this.cmRemarks=cmRemarks;
		this.sellerRemarks=sellerRemarks;
		this.buyerRemarks=buyerRemarks;
	}
}
