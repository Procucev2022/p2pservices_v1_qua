package com.portal.procucev.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "vendor_catalogue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorCatalogue extends Procucev{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name="material_description")
    private String materialDescription;

    private String uom;

    @Column(name="min_order_quantity")
    private Integer minOrderQuantity;

    @Column(name="price_per_uom")
    private BigDecimal pricePerUom;

    @Column(name="gst_percentage")
    private BigDecimal gstPercentage;

    @Column(name="lead_time_for_moq")
    private Integer leadTimeForMoq; // in days

    @Column(name="available_quantity")
    private Integer availableQuantity;

    // Terms & Conditions
    @Column(name="packing_and_forwarding")
    private String packingAndForwarding;

    private String freight;

    @Column(name="payment_terms")
    private String paymentTerms;
    
    @Column(name="other_terms")
    private String otherTerms;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@ManyToOne(fetch = FetchType.LAZY)
	private Organization org;
	
	private String user;
	
	@Column(name="category")
	private String category;
	
	@Column(name="division")
	private String division;
}
