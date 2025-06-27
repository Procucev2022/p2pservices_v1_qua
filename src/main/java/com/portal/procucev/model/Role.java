package com.portal.procucev.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role extends Procucev {
	private static final long serialVersionUID = 1L;

	private String roleName;

	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	private OrgType orgtype;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "role_permission", joinColumns = {
			@JoinColumn(referencedColumnName = "uuid") }, inverseJoinColumns = {
					@JoinColumn(referencedColumnName = "uuid") })
	private List<Permission> permission = new ArrayList<Permission>();

	@Column(name = "is_active")
	private boolean active;
}
