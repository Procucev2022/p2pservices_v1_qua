package com.portal.procucev.Dto;

import java.util.Date;

import jakarta.persistence.Column;

import com.portal.procucev.model.MasterStatus;

public class BfsDTO {

	private String id;

	private Date createdTS;

	private String description;

	private String specification;

	private String unitofMeasures;

	private double sellPrice;

	private double discount;

	private double sellFinalPrice;

	private String sellerEmail;

	private String sellerName;

	private String sellerCompanyName;

	private double buyPrice;

	private double buyerDiscount;

	private double buyerFinalPrice;

	private double totalQuantity;

	private double availableQuantity;

	private int quantity;

	private String buyerEmail;

	private String buyerName;

	private String buyerCompanyName;

	private String buyerRemarks;

	private String buyerId;

	private String sellerId;

	private MasterStatus status;

	private boolean buyPriceDisclosure;

	private String disclosedBuypriceValue;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(String specification) {
		this.specification = specification;
	}

	public String getUnitofMeasures() {
		return unitofMeasures;
	}

	public void setUnitofMeasures(String unitofMeasures) {
		this.unitofMeasures = unitofMeasures;
	}

	public double getSellPrice() {
		return sellPrice;
	}

	public void setSellPrice(double sellPrice) {
		this.sellPrice = sellPrice;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public double getSellFinalPrice() {
		return sellFinalPrice;
	}

	public void setSellFinalPrice(double sellFinalPrice) {
		this.sellFinalPrice = sellFinalPrice;
	}

	public String getSellerEmail() {
		return sellerEmail;
	}

	public void setSellerEmail(String sellerEmail) {
		this.sellerEmail = sellerEmail;
	}

	public String getSellerName() {
		return sellerName;
	}

	public void setSellerName(String sellerName) {
		this.sellerName = sellerName;
	}

	public String getSellerCompanyName() {
		return sellerCompanyName;
	}

	public void setSellerCompanyName(String sellerCompanyName) {
		this.sellerCompanyName = sellerCompanyName;
	}

	public double getBuyPrice() {
		return buyPrice;
	}

	public void setBuyPrice(double buyPrice) {
		this.buyPrice = buyPrice;
	}

	public double getBuyerDiscount() {
		return buyerDiscount;
	}

	public void setBuyerDiscount(double buyerDiscount) {
		this.buyerDiscount = buyerDiscount;
	}

	public double getBuyerFinalPrice() {
		return buyerFinalPrice;
	}

	public void setBuyerFinalPrice(double buyerFinalPrice) {
		this.buyerFinalPrice = buyerFinalPrice;
	}

	public String getBuyerEmail() {
		return buyerEmail;
	}

	public void setBuyerEmail(String buyerEmail) {
		this.buyerEmail = buyerEmail;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public void setBuyerName(String buyerName) {
		this.buyerName = buyerName;
	}

	public String getBuyerCompanyName() {
		return buyerCompanyName;
	}

	public void setBuyerCompanyName(String buyerCompanyName) {
		this.buyerCompanyName = buyerCompanyName;
	}

	public Date getCreatedTS() {
		return createdTS;
	}

	public void setCreatedTS(Date createdTS) {
		this.createdTS = createdTS;
	}

	public MasterStatus getStatus() {
		return status;
	}

	public void setStatus(MasterStatus status) {
		this.status = status;
	}

	public double getTotalQuantity() {
		return totalQuantity;
	}

	public void setTotalQuantity(double totalQuantity) {
		this.totalQuantity = totalQuantity;
	}

	public double getAvailableQuantity() {
		return availableQuantity;
	}

	public void setAvailableQuantity(double availableQuantity) {
		this.availableQuantity = availableQuantity;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getBuyerRemarks() {
		return buyerRemarks;
	}

	public void setBuyerRemarks(String buyerRemarks) {
		this.buyerRemarks = buyerRemarks;
	}

	public String getBuyerId() {
		return buyerId;
	}

	public void setBuyerId(String buyerId) {
		this.buyerId = buyerId;
	}

	public String getSellerId() {
		return sellerId;
	}

	public void setSellerId(String sellerId) {
		this.sellerId = sellerId;
	}

	public boolean isBuyPriceDisclosure() {
		return buyPriceDisclosure;
	}

	public void setBuyPriceDisclosure(boolean buyPriceDisclosure) {
		this.buyPriceDisclosure = buyPriceDisclosure;
	}

	public String getDisclosedBuypriceValue() {
		return disclosedBuypriceValue;
	}

	public void setDisclosedBuypriceValue(String disclosedBuypriceValue) {
		this.disclosedBuypriceValue = disclosedBuypriceValue;
	}
	
	

}
