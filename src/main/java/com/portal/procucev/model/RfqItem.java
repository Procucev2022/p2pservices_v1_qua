package com.portal.procucev.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The persistent class for the rfq_items database table.
 * 
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "rfq_items")
public class RfqItem extends Procucev {
	private static final long serialVersionUID = 1L;

	@ManyToOne
	@JsonBackReference(value = "rfq")
	private Rfq rfq;
	private String category;
	private String division;
	private String itemcode;
	private String description;
	private String brand;
	private double quantity;
	@OrderBy("serialNo ASC")
	@Column(name = "serial_no")
	private double serialNo;
	@Column(name = "unit_of_measures")
	private String unitofMeasures;
	private long unitprice;
	private long totalamount;
	private String remarks;	
	@Column(name="pritem_id")
	private String pritemId;
	

}