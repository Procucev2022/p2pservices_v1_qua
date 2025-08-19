package com.portal.procucev.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="org_division_category")
public class OrgDivisionCategory extends Procucev{/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String division;   // e.g., "Electronics Division"
    private String category;   // e.g., "Mobile Phones"

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
