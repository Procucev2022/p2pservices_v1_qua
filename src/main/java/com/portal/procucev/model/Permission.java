package com.portal.procucev.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission extends Procucev {
	private static final long serialVersionUID = 1L;

	@Column(name = "MODULE_NAME")
	private String moduleName;

	@Column(name = "IS_ROOT")
	private boolean root;

	@Column(name = "PARENT_PERMISSION_ID")
	private String parentPermissionID;

	@Column(name = "PERMISSION_NAME")
	private String permissionName;

	@Column(name = "IS_MODULE_NAME")
	private boolean moduleCheck;

}
