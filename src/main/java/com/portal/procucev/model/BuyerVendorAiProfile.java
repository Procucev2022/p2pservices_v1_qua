package com.portal.procucev.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "buyer_vendor_ai_profile",
    uniqueConstraints = @UniqueConstraint(name = "uk_buyer_vendor_ai_code", columnNames = {"vendor_code", "buyer_org_id"}),
    indexes = {
        @Index(name = "idx_ai_buyer_vendor", columnList = "vendor_code, buyer_org_id")
    }
)
public class BuyerVendorAiProfile extends Procucev {

    private static final long serialVersionUID = 1L;

    @Column(name = "vendor_code", nullable = false)
    private String vendorCode;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "buyer_org_id", nullable = false)
    private String buyerOrgId;

    @Column(name = "industry")
    private String industry;

    @Column(name = "category")
    private String category;

    @Lob
    @Column(name = "sub_categories_json", columnDefinition = "TEXT")
    private String subCategoriesJson;

    @Lob
    @Column(name = "capabilities_json", columnDefinition = "TEXT")
    private String capabilitiesJson;

    @Lob
    @Column(name = "suitable_categories_json", columnDefinition = "TEXT")
    private String suitableCategoriesJson;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "pan")
    private String pan;

    @Column(name = "gstin_verified")
    private boolean gstinVerified = true;

    @Column(name = "pan_verified")
    private boolean panVerified = true;

    @Column(name = "company_info_verified")
    private boolean companyInfoVerified = true;

    @Column(name = "contact_info_verified")
    private boolean contactInfoVerified = true;

    @Column(name = "qualification")
    private String qualification = "Qualified";

    @Column(name = "ai_score")
    private int aiScore = 90;

    @Column(name = "financial_stability")
    private int financialStability = 92;

    @Column(name = "operational_scope")
    private int operationalScope = 90;

    @Column(name = "compliance_score")
    private int complianceScore = 95;

    @Column(name = "supply_reliability")
    private int supplyReliability = 88;

    @Column(name = "verification_status")
    private String verificationStatus = "100% Verified";

    @Column(name = "compliance_status")
    private String complianceStatus = "Fully Compliant";

    @Column(name = "phone_1")
    private String phone1;

    @Column(name = "phone_2")
    private String phone2;

    @Column(name = "email")
    private String email;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country = "India";

    @Column(name = "type_of_business")
    private String typeOfBusiness;

    @Column(name = "vendor_group")
    private String vendorGroup;

    @Column(name = "sourcing_scope")
    private String sourcingScope = "Client Only";

    @Lob
    @Column(name = "ai_raw_response", columnDefinition = "TEXT")
    private String aiRawResponse;
}
