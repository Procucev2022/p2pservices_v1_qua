package com.portal.procucev.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="category_division")
public class CategoryDivision extends Procucev{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String division;
	
	private String category;

}
