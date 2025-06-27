package com.portal.procucev.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "org_types")
@Data
public class OrgType extends Procucev {

	private static final long serialVersionUID = 1L;

	private String description;

	@Column(name = "type_name")
	private String typeName;
}
