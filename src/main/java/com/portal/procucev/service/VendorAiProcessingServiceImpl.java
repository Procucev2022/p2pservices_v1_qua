package com.portal.procucev.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.procucev.dao.BuyerVendorAiProfileDao;
import com.portal.procucev.dao.BuyerVendorDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;
import com.portal.procucev.rfq.client.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorAiProcessingServiceImpl implements VendorAiProcessingService {

    private final BuyerVendorAiProfileDao aiProfileDao;
    private final BuyerVendorDao buyerVendorDao;
    private final GeminiApiClient geminiApiClient;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void ensureTableExists() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS buyer_vendor_ai_profile (
                    uuid VARCHAR(255) NOT NULL PRIMARY KEY,
                    created_by VARCHAR(255),
                    last_modified_by VARCHAR(255),
                    created_ts DATETIME,
                    last_modified_ts DATETIME,
                    vendor_code VARCHAR(255) NOT NULL,
                    vendor_name VARCHAR(255) NOT NULL,
                    buyer_org_id VARCHAR(255) NOT NULL,
                    industry VARCHAR(255),
                    category VARCHAR(255),
                    sub_categories_json TEXT,
                    capabilities_json TEXT,
                    suitable_categories_json TEXT,
                    gstin VARCHAR(255),
                    pan VARCHAR(255),
                    gstin_verified TINYINT(1) DEFAULT 1,
                    pan_verified TINYINT(1) DEFAULT 1,
                    company_info_verified TINYINT(1) DEFAULT 1,
                    contact_info_verified TINYINT(1) DEFAULT 1,
                    qualification VARCHAR(50) DEFAULT 'Qualified',
                    ai_score INT DEFAULT 90,
                    financial_stability INT DEFAULT 90,
                    operational_scope INT DEFAULT 90,
                    compliance_score INT DEFAULT 95,
                    supply_reliability INT DEFAULT 88,
                    verification_status VARCHAR(100) DEFAULT '100% Verified',
                    compliance_status VARCHAR(100) DEFAULT 'Fully Compliant',
                    phone_1 VARCHAR(50),
                    phone_2 VARCHAR(50),
                    email VARCHAR(255),
                    address_line VARCHAR(255),
                    city VARCHAR(100),
                    state VARCHAR(100),
                    postal_code VARCHAR(50),
                    country VARCHAR(100) DEFAULT 'India',
                    type_of_business VARCHAR(255),
                    vendor_group VARCHAR(255),
                    sourcing_scope VARCHAR(255) DEFAULT 'Client Only',
                    ai_raw_response TEXT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        } catch (Exception e) {
            log.warn("Table verification note: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<BuyerVendorAiProfile> processVendorsWithAi(List<BuyerVendor> vendors, String buyerOrgId, String username) {
        ensureTableExists();
        if (vendors == null || vendors.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("Processing {} vendors concurrently with Gemini AI for buyerOrgId: {}", vendors.size(), buyerOrgId);
        
        // Execute Gemini AI analysis in parallel across all vendors
        List<BuyerVendorAiProfile> analyzedProfiles = vendors.parallelStream()
                .map(v -> analyzeSingleVendor(v, buyerOrgId, username))
                .toList();

        List<BuyerVendorAiProfile> results = new ArrayList<>();
        for (BuyerVendorAiProfile profile : analyzedProfiles) {
            Optional<BuyerVendorAiProfile> existing = aiProfileDao.findByVendorCodeAndBuyerOrgId(profile.getVendorCode(), buyerOrgId);
            if (existing.isPresent()) {
                BuyerVendorAiProfile toUpdate = existing.get();
                copyFields(profile, toUpdate);
                toUpdate.setLastModifiedBy(username);
                results.add(aiProfileDao.save(toUpdate));
            } else {
                profile.setCreatedBy(username);
                results.add(aiProfileDao.save(profile));
            }
        }
        return results;
    }

    @Override
    public List<BuyerVendorAiProfile> getAnalyzedVendors(String buyerOrgId) {
        ensureTableExists();
        return aiProfileDao.findByBuyerOrgIdOrderByCreatedTSDesc(buyerOrgId);
    }

    @Override
    public Optional<BuyerVendorAiProfile> getVendorAiProfile(String vendorCode, String buyerOrgId) {
        ensureTableExists();
        Optional<BuyerVendorAiProfile> opt = aiProfileDao.findByVendorCodeAndBuyerOrgId(vendorCode, buyerOrgId);
        if (opt.isPresent()) {
            return opt;
        }

        // If not in AI profile table yet, find in BuyerVendor master and analyze via Gemini
        Optional<BuyerVendor> vendorOpt = buyerVendorDao.findByVendorCodeAndBuyerOrgId(vendorCode, buyerOrgId);
        if (vendorOpt.isPresent()) {
            BuyerVendorAiProfile profile = analyzeSingleVendor(vendorOpt.get(), buyerOrgId, "system-ai");
            return Optional.of(aiProfileDao.save(profile));
        }

        return Optional.empty();
    }

    private BuyerVendorAiProfile analyzeSingleVendor(BuyerVendor vendor, String buyerOrgId, String username) {
        BuyerVendorAiProfile profile = createBaseProfile(vendor, buyerOrgId, username);

        // Prompt for Gemini AI LLM
        String prompt = String.format("""
            You are an enterprise procurement AI assistant analyzing a supplier for vendor onboarding.
            Vendor Information:
            - Vendor Name: %s
            - Industry Provided: %s
            - Business Type: %s
            - Search Term: %s
            - City/Location: %s

            Analyze this supplier and return a STRICT JSON object with:
            {
              "industry": "Industry classification e.g. MRO, Chemicals, Electrical, IT & Software, Construction, Manufacturing, Logistics & Warehousing",
              "category": "Primary Procurement Category e.g. Steel & Metals, Petrochemicals, Fasteners, Cables, IT Hardware & Services",
              "subCategories": ["Subcategory 1", "Subcategory 2", "Subcategory 3", "Subcategory 4"],
              "capabilities": ["Capability 1", "Capability 2", "Capability 3", "Capability 4"],
              "suitableProcurementCategories": ["Procurement Scope 1", "Procurement Scope 2"]
            }
            CRITICAL: Return STRICT JSON ONLY. Do not wrap in markdown or backticks.
            """,
            vendor.getVendorName(),
            vendor.getTypeOfIndustry() != null ? vendor.getTypeOfIndustry() : "Industrial",
            vendor.getTypeOfBusiness() != null ? vendor.getTypeOfBusiness() : "Supplier",
            vendor.getSearchTerm() != null ? vendor.getSearchTerm() : "",
            vendor.getCity() != null ? vendor.getCity() : ""
        );

        try {
            log.info("Calling Gemini AI LLM for vendor: {}", vendor.getVendorName());
            String jsonResp = geminiApiClient.generateContent(prompt, List.of(), GeminiApiClient.getVendorCategorizationSchema());
            String sanitized = sanitizeJson(jsonResp);
            JsonNode root = objectMapper.readTree(sanitized);

            String industry = root.path("industry").asText(vendor.getTypeOfIndustry() != null ? vendor.getTypeOfIndustry() : "");
            String category = root.path("category").asText("");

            profile.setIndustry(industry);
            profile.setCategory(category);
            profile.setSubCategoriesJson(root.path("subCategories").toString());
            profile.setCapabilitiesJson(root.path("capabilities").toString());
            profile.setSuitableCategoriesJson(root.path("suitableProcurementCategories").toString());
            profile.setAiRawResponse(sanitized);
            log.info("Gemini AI successfully classified vendor {}: Industry={}, Category={}", vendor.getVendorName(), industry, category);
        } catch (Exception ex) {
            log.error("Gemini AI execution failed for vendor {}: {}", vendor.getVendorName(), ex.getMessage(), ex);
            throw new RuntimeException("Gemini AI execution failed for vendor " + vendor.getVendorName() + ": " + ex.getMessage(), ex);
        }

        // Apply qualification verification based on actual vendor fields
        applyDeterministicQualification(profile, vendor);
        return profile;
    }

    private BuyerVendorAiProfile createBaseProfile(BuyerVendor vendor, String buyerOrgId, String username) {
        BuyerVendorAiProfile profile = new BuyerVendorAiProfile();
        profile.setVendorCode(vendor.getVendorCode());
        profile.setVendorName(vendor.getVendorName());
        profile.setBuyerOrgId(buyerOrgId);
        profile.setPan(vendor.getPan());
        profile.setGstin(vendor.getGstin());
        profile.setPhone1(vendor.getPhone1());
        profile.setPhone2(vendor.getPhone2());
        profile.setEmail((vendor.getVendorCode() != null ? vendor.getVendorCode().toLowerCase() : "vendor") + "@vendor-hub.com");
        profile.setAddressLine(vendor.getAddressLine());
        profile.setCity(vendor.getCity() != null ? vendor.getCity() : "Mumbai");
        profile.setState(vendor.getRegionCode() != null ? vendor.getRegionCode() : "Maharashtra");
        profile.setPostalCode(vendor.getPostalCode() != null ? vendor.getPostalCode() : "400001");
        profile.setCountry(vendor.getCountry() != null ? vendor.getCountry() : "India");
        profile.setTypeOfBusiness(vendor.getTypeOfBusiness() != null ? vendor.getTypeOfBusiness() : "Authorized Enterprise");
        profile.setVendorGroup(vendor.getVendorGroup() != null ? vendor.getVendorGroup() : "Approved Vendor");
        profile.setSourcingScope(vendor.getSourcingScope() != null ? vendor.getSourcingScope() : "Client Only");
        profile.setCreatedBy(username);
        return profile;
    }

    private void applyDeterministicQualification(BuyerVendorAiProfile profile, BuyerVendor vendor) {
        boolean hasGstin = vendor.getGstin() != null && !vendor.getGstin().isBlank();
        boolean hasPan = vendor.getPan() != null && !vendor.getPan().isBlank();
        boolean hasPhone = vendor.getPhone1() != null && !vendor.getPhone1().isBlank();
        boolean hasAddress = vendor.getAddressLine() != null && !vendor.getAddressLine().isBlank();
        boolean hasCity = vendor.getCity() != null && !vendor.getCity().isBlank();

        profile.setGstinVerified(hasGstin);
        profile.setPanVerified(hasPan);
        profile.setCompanyInfoVerified(hasAddress && hasCity);
        profile.setContactInfoVerified(hasPhone);

        // Score Calculation based on actual data completeness (baseline 50, max 95)
        int score = 50;
        if (hasGstin) score += 20;
        if (hasPan) score += 10;
        if (hasPhone) score += 5;
        if (hasAddress && hasCity) score += 5;
        if (vendor.getTypeOfBusiness() != null && !vendor.getTypeOfBusiness().isBlank()) score += 3;
        if (vendor.getTypeOfIndustry() != null && !vendor.getTypeOfIndustry().isBlank()) score += 2;

        score = Math.min(score, 95);

        profile.setAiScore(score);
        profile.setFinancialStability(Math.min(score + 2, 95));
        profile.setOperationalScope(Math.max(score - 1, 40));
        profile.setComplianceScore(hasGstin && hasPan ? 95 : (hasGstin || hasPan ? 75 : 60));
        profile.setSupplyReliability(score);

        if (score >= 85) {
            profile.setQualification("Qualified");
            profile.setVerificationStatus(hasGstin && hasPan ? "100% Provided" : "80% Provided");
            profile.setComplianceStatus("Compliant");
        } else if (score >= 70) {
            profile.setQualification("Pending");
            profile.setVerificationStatus("Partial Information");
            profile.setComplianceStatus("Pending Review");
        } else {
            profile.setQualification("Unqualified");
            profile.setVerificationStatus("Incomplete");
            profile.setComplianceStatus("Non-Compliant");
        }
    }

    private void copyFields(BuyerVendorAiProfile src, BuyerVendorAiProfile dest) {
        dest.setVendorName(src.getVendorName());
        dest.setIndustry(src.getIndustry());
        dest.setCategory(src.getCategory());
        dest.setSubCategoriesJson(src.getSubCategoriesJson());
        dest.setCapabilitiesJson(src.getCapabilitiesJson());
        dest.setSuitableCategoriesJson(src.getSuitableCategoriesJson());
        dest.setGstin(src.getGstin());
        dest.setPan(src.getPan());
        dest.setGstinVerified(src.isGstinVerified());
        dest.setPanVerified(src.isPanVerified());
        dest.setCompanyInfoVerified(src.isCompanyInfoVerified());
        dest.setContactInfoVerified(src.isContactInfoVerified());
        dest.setQualification(src.getQualification());
        dest.setAiScore(src.getAiScore());
        dest.setFinancialStability(src.getFinancialStability());
        dest.setOperationalScope(src.getOperationalScope());
        dest.setComplianceScore(src.getComplianceScore());
        dest.setSupplyReliability(src.getSupplyReliability());
        dest.setVerificationStatus(src.getVerificationStatus());
        dest.setComplianceStatus(src.getComplianceStatus());
        dest.setPhone1(src.getPhone1());
        dest.setPhone2(src.getPhone2());
        dest.setAddressLine(src.getAddressLine());
        dest.setCity(src.getCity());
        dest.setState(src.getState());
        dest.setPostalCode(src.getPostalCode());
        dest.setTypeOfBusiness(src.getTypeOfBusiness());
        dest.setVendorGroup(src.getVendorGroup());
        dest.setSourcingScope(src.getSourcingScope());
        dest.setAiRawResponse(src.getAiRawResponse());
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return "{}";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1).trim();
        }
        return cleaned;
    }
}
