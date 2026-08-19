package com.portal.procucev.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Buyer's own vendor master list.
 * Each record belongs to one buyer organization (data isolation via buyerOrgId).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "buyer_vendor", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"vendor_code", "buyer_org_id"})
})
public class BuyerVendor extends Procucev {

    private static final long serialVersionUID = 1L;

    @Column(name = "vendor_code", nullable = false)
    @NotBlank(message = "Vendor code is required")
    private String vendorCode;

    @Column(name = "vendor_name", nullable = false)
    @NotBlank(message = "Vendor name is required")
    @Size(min = 3, message = "Vendor name must be at least 3 characters")
    private String vendorName;

    @Column(name = "search_term")
    private String searchTerm;

    @Column(name = "pan", length = 10)
    @Pattern(regexp = "^$|[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format")
    private String pan;

    @Column(name = "gstin", length = 15)
    @Pattern(regexp = "^$|\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[A-Z\\d]{1}[Z]{1}[A-Z\\d]{1}", message = "Invalid GSTIN format")
    private String gstin;

    @Column(name = "country", length = 10)
    private String country = "IN";

    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "postal_code", length = 6)
    @Pattern(regexp = "^$|\\d{6}", message = "Postal code must be 6 digits")
    private String postalCode;

    @Column(name = "phone_1", length = 15)
    @NotBlank(message = "Phone 1 is required")
    @Pattern(regexp = "\\d{10}", message = "Phone must be 10 digits")
    private String phone1;

    @Column(name = "phone_2", length = 15)
    private String phone2;

    @Column(name = "type_of_business")
    private String typeOfBusiness;

    @Column(name = "type_of_industry")
    private String typeOfIndustry;

    @Column(name = "vendor_group")
    private String vendorGroup;

    @Column(name = "status", nullable = false)
    @NotBlank
    private String status = "Active"; // Active, Inactive, Pending

    @Column(name = "sourcing_scope")
    private String sourcingScope = "Client Only"; // Client Only, Client+Procucev

    @Column(name = "buyer_org_id", nullable = false)
    private String buyerOrgId;
}
