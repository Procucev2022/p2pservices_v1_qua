package com.portal.procucev.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="item_uom")
public class ItemUOM extends Procucev{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String description;
}
