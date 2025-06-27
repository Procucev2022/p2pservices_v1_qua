package com.portal.procucev.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Data
@NoArgsConstructor
@Entity
@Table(name = "client_delivery_location_rfq")
public class ClientDeliveryLocationRfq extends Procucev {
	private static final long serialVersionUID = 1L;

	private String address;

	private String city;

	private String state;
	
	private String pincode;

	@ManyToOne
	@JsonBackReference
	private Rfq rfq;
}
