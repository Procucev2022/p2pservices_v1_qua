package com.portal.procucev.model;

import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Entity;

@NoArgsConstructor
@Data
@Entity
@Table(name = "pincode_data")
public class PincodeData extends Procucev {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String pincode;
	private String city;
	private String state;
	
	public PincodeData(String pincode, String city, String state) {
		super();
		this.pincode = pincode;
		this.city = city;
		this.state = state;
	}

	public PincodeData(String city, String state) {
		super();
		this.city = city;
		this.state = state;
	}
	
	
	
}
