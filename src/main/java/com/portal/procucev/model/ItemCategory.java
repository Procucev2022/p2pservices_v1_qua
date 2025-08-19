package com.portal.procucev.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="item_category")
public class ItemCategory extends Procucev{/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Column(name="serial_no")
	private int serialNo;
	
	private String item;
	
	private String category;
	
	private String division;

}
