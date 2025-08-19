package com.portal.procucev.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="org_branches")
public class OrgBranches extends Procucev{/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Column(name="branch_name")
	private String branchName;
	@Column(name="contact_person")
    private String contactPerson;
    private String email;
    private String address;
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

}
