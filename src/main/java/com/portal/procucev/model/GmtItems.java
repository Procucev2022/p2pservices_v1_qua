package com.portal.procucev.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="gmt_items")
public class GmtItems extends Procucev{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String description;
	private String category;
	private String brand;
	private String remarks;
	private double quantity;
	@Column(name="rfq_item_id")
	private String rfqItemId;
	@Column(name = "unit_of_measures")
	private String unitofMeasures;
	@Column(name="client_sector")
	private String clientSector;
	
}
