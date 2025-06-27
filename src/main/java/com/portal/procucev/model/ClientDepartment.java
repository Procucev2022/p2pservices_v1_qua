package com.portal.procucev.model;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "client_departments")
public class ClientDepartment extends Procucev {
	

	private static final long serialVersionUID = 1L;
	
	private String department;
	
	@ManyToOne
	private Organization organization;

	
	public ClientDepartment(String id,Date createdTS,String createdBy,String department) {
		super();
		this.id=id;
		this.createdTS=createdTS;
		this.createdBy=createdBy;
		this.department = department;
	}

}
