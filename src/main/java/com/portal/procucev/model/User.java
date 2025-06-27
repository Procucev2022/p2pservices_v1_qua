package com.portal.procucev.model;



import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name = "user")
public class User extends Procucev {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Column
	private String username;
	@Column
	private String password;

	private String email;
	
	@Column(name = "first_name")
	private String firstName;
	
	@ManyToOne
	private MasterStatus clientStatus;

	@Column(name = "last_name")
	private String lastName;

	private String phone;
	
	@Column(name="self_client")
	private boolean selfClient;

	@Column(name = "full_name")
	private String fullName;

	@ManyToOne(fetch = FetchType.LAZY)
	private Organization org;

	@ManyToOne(fetch = FetchType.LAZY)
	private Role role;

	@Column(name = "dept_name")
	private String deptName;

	@Column(name = "is_active")
	private boolean active;
	
	@Column(name = "reset_password")
	private boolean resetPassword = true;

}
