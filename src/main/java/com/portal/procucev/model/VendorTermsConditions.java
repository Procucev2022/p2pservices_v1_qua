package com.portal.procucev.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity(name = "vendor_terms_conditions")
public class VendorTermsConditions extends Procucev {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	@Column(name = "packing_and_forwarding")
	private String packingAndForwarding;

	private String freight;

	@Column(name = "payment_terms")
	private String paymentTerms;

	@Column(name = "other_terms")
	private String otherTerms;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@ManyToOne(fetch = FetchType.LAZY)
	private Organization org;

	private String user;
}
