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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bfs_items")
public class BFSItems extends Procucev {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String description;

	private String specification;

	@Column(name = "total_quantity")
	private double totalQuantity;

	@Column(name = "available_quantity")
	private double availableQuantity;

	private String category;

	@Column(name = "item_number")
	private String itemNumber;

	private String location;

	@Column(name = "age_of_asset")
	private String ageOfAsset;
	
	@Column(name="unit_of_measure")
	private String unitofMeasures;

	@Column(name = "sell_price")
	private double sellPrice;

	private double discount;

	@Column(name = "ask_price")
	private double askPrice;

	@Column(name = "bfs_group")
	private String bfsGroup;
	
	@Column(name="buy_price_disclosure")
	private boolean buyPriceDisclosure;
	
	@Column(name="disclosed_buyprice_value")
	private String disclosedBuypriceValue;
	
	@Transient
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] boqfile;
	
	@Column(name="user_id")
	private String userId;
	
	@Column(name="proxy_id")
	private String proxyId;

	@ManyToOne(fetch = FetchType.LAZY)
	private Organization org;

	@ManyToOne
	private MasterStatus status;
	
	private String remarks;
	
	@Column(name="requested_flag")
	private boolean requestedFlag;
	
	@Column(name="comments_flag")
	private boolean commentsFlag;
	
	@Column(name="images_flag")
	private boolean imagesFlag;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "bfs_uuid")
	private List<BFSDocuments> bfsDocuments = new ArrayList<BFSDocuments>();
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "bfs_uuid")
	private List<BFSImages> bfsImages = new ArrayList<BFSImages>();
	
	@Column(name="latest_bid_date")
	private Date latestBidDate;

	

	public BFSItems(String id,Date createdTS,String description, String specification, double totalQuantity, double availableQuantity,
			String category, String itemNumber, String location, String ageOfAsset, double sellPrice, double discount,
			double askPrice, String bfsGroup,String proxyId,String unitofMeasures,String remarks,MasterStatus status,boolean commentsFlag
			,boolean buyPriceDisclosure,String disclosedBuypriceValue,Date latestBidDate) {
		super();
		this.id = id;
		this.createdTS = createdTS;
		this.description = description;
		this.specification = specification;
		this.totalQuantity = totalQuantity;
		this.availableQuantity = availableQuantity;
		this.category = category;
		this.itemNumber = itemNumber;
		this.location = location;
		this.ageOfAsset = ageOfAsset;
		this.sellPrice = sellPrice;
		this.discount = discount;
		this.askPrice = askPrice;
		this.bfsGroup = bfsGroup;
		this.proxyId=proxyId;
		this.unitofMeasures=unitofMeasures;
		this.remarks=remarks;
		this.status=status;
		this.commentsFlag=commentsFlag;
		this.buyPriceDisclosure=buyPriceDisclosure;
		this.disclosedBuypriceValue=disclosedBuypriceValue;
		this.latestBidDate=latestBidDate;
	}
}
