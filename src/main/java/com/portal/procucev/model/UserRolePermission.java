package com.portal.procucev.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="user_role_permission")
public class UserRolePermission extends Procucev{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String email;
	
	private String user;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_uuid")
	private List<ClientPermission> clientPermission = new ArrayList<ClientPermission>();

}
