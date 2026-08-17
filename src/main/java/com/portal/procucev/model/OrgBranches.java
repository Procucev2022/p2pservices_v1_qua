package com.portal.procucev.model;


import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
    /**
     * Owning side back-reference. Excluded from toString/equals/hashCode to avoid
     * infinite recursion with {@link Organization#getBranches()}.
     */
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "organization_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Organization organization;

}
