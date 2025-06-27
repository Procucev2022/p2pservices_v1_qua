package com.portal.procucev.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "master_status")
public class MasterStatus extends Procucev {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JsonBackReference(value = "orgType_masterStatus")
	private OrgType orgType;

	@Column(name = "description")
	private String description;

	@Column(name = "ui_display")
	private String uiDisplay;

	private String status;
}
