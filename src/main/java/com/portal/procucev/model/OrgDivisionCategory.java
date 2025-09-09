package com.portal.procucev.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
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

    @JsonBackReference(value = "org-division-org")
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
   
    @Column(name = "user_id")
    private String userId; 
}
