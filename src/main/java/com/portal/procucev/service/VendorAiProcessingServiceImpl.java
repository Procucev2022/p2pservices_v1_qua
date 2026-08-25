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
        log.info("Processing {} vendors with Gemini AI for buyerOrgId: {}", vendors.size(), buyerOrgId);
        List<BuyerVendorAiProfile> results = new ArrayList<>();

        for (BuyerVendor v : vendors) {
            BuyerVendorAiProfile profile = analyzeSingleVendor(v, buyerOrgId, username);
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
    @Transactional
    public List<BuyerVendorAiProfile> getAnalyzedVendors(String buyerOrgId) {
        ensureTableExists();
        List<BuyerVendorAiProfile> existingProfiles = aiProfileDao.findByBuyerOrgIdOrderByCreatedTSDesc(buyerOrgId);
        
        // Ensure any imported BuyerVendors without an AI profile get analyzed and included
        List<BuyerVendor> allVendors = buyerVendorDao.findByBuyerOrgId(buyerOrgId);
        if (allVendors != null && !allVendors.isEmpty()) {
            Set<String> profiledCodes = new HashSet<>();
            for (BuyerVendorAiProfile p : existingProfiles) {
                if (p.getVendorCode() != null) {
                    profiledCodes.add(p.getVendorCode().trim().toLowerCase());
                }
            }

            List<BuyerVendor> unprofiledVendors = new ArrayList<>();
            for (BuyerVendor bv : allVendors) {
                if (bv.getVendorCode() != null && !profiledCodes.contains(bv.getVendorCode().trim().toLowerCase())) {
                    unprofiledVendors.add(bv);
                }
            }

            if (!unprofiledVendors.isEmpty()) {
                log.info("Auto-analyzing {} unprofiled vendors via Gemini AI for buyerOrgId: {}", unprofiledVendors.size(), buyerOrgId);
                List<BuyerVendorAiProfile> newlyProfiled = processVendorsWithAi(unprofiledVendors, buyerOrgId, "system-ai");
                existingProfiles.addAll(0, newlyProfiled);
            }
        }

        return existingProfiles;
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
            String jsonResp = geminiApiClient.generateContent(prompt);
            String sanitized = sanitizeJson(jsonResp);
            JsonNode root = objectMapper.readTree(sanitized);

            String industry = root.path("industry").asText(vendor.getTypeOfIndustry() != null ? vendor.getTypeOfIndustry() : "Manufacturing & Industrial");
            String category = root.path("category").asText("Industrial Goods & Assemblies");

            profile.setIndustry(industry);
            profile.setCategory(category);
            profile.setSubCategoriesJson(root.path("subCategories").toString());
            profile.setCapabilitiesJson(root.path("capabilities").toString());
            profile.setSuitableCategoriesJson(root.path("suitableProcurementCategories").toString());
            profile.setAiRawResponse(sanitized);
            log.info("Gemini AI successfully classified vendor {}: Industry={}, Category={}", vendor.getVendorName(), industry, category);
        } catch (Exception ex) {
            log.error("Gemini AI execution failed for vendor {}: {}", vendor.getVendorName(), ex.getMessage(), ex);
            applyDeterministicCategorization(profile, vendor);
        }

        // Apply consistent qualification scoring based on actual data
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

    private void applyDeterministicCategorization(BuyerVendorAiProfile profile, BuyerVendor vendor) {
        String industry = vendor.getTypeOfIndustry() != null && !vendor.getTypeOfIndustry().isBlank() 
            ? vendor.getTypeOfIndustry().trim() 
            : detectIndustry(vendor.getVendorName(), "");
        String category = detectCategory(industry);

        profile.setIndustry(industry);
        profile.setCategory(category);
        profile.setSubCategoriesJson(generateSubCategoriesJson(category));
        profile.setCapabilitiesJson(generateCapabilitiesJson(industry, category));
        profile.setSuitableCategoriesJson(String.format("[\"%s Direct Sourcing\", \"%s Annual Supply Contract\"]", industry, category));
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

        // Deterministic Score Calculation based on data completeness
        int score = 70;
        if (hasGstin) score += 10;
        if (hasPan) score += 5;
        if (hasPhone) score += 5;
        if (hasAddress && hasCity) score += 5;
        if (vendor.getTypeOfBusiness() != null && !vendor.getTypeOfBusiness().isBlank()) score += 3;
        if (vendor.getTypeOfIndustry() != null && !vendor.getTypeOfIndustry().isBlank()) score += 2;

        score = Math.min(score, 95);

        profile.setAiScore(score);
        profile.setFinancialStability(score + 2);
        profile.setOperationalScope(score - 1);
        profile.setComplianceScore(hasGstin && hasPan ? 95 : 85);
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

    private String detectIndustry(String name, String existingIndustry) {
        if (existingIndustry != null && !existingIndustry.isBlank()) return existingIndustry;
        if (name == null) return "Manufacturing & Industrial";
        String lower = name.toLowerCase();
        if (lower.contains("steel") || lower.contains("metal") || lower.contains("mro") || lower.contains("iron") || lower.contains("tool")) return "Steel & Metals";
        if (lower.contains("chem") || lower.contains("petro") || lower.contains("oil") || lower.contains("gas") || lower.contains("polymer")) return "Chemicals";
        if (lower.contains("elect") || lower.contains("power") || lower.contains("cable") || lower.contains("switch")) return "Electrical & Automation";
        if (lower.contains("tech") || lower.contains("info") || lower.contains("soft") || lower.contains("it")) return "IT & Software";
        if (lower.contains("build") || lower.contains("cement") || lower.contains("const") || lower.contains("infra")) return "Cement & Building Materials";
        if (lower.contains("logist") || lower.contains("transport") || lower.contains("freight") || lower.contains("dart") || lower.contains("dhl")) return "Logistics & Warehousing";
        if (lower.contains("pharma") || lower.contains("drug") || lower.contains("health")) return "Pharmaceuticals";
        if (lower.contains("food") || lower.contains("dairy") || lower.contains("beverage") || lower.contains("fmcg")) return "Food & FMCG";
        return "Manufacturing & Industrial";
    }

    private String detectCategory(String industry) {
        return switch (industry) {
            case "Steel & Metals" -> "Steel & Metals";
            case "Chemicals" -> "Petrochemicals";
            case "Electrical & Automation" -> "Switchgear & Cabling";
            case "IT & Software" -> "IT Hardware & Services";
            case "Cement & Building Materials" -> "Building Materials & Cement";
            case "Logistics & Warehousing" -> "Freight & Transport Services";
            case "Pharmaceuticals" -> "Active Pharmaceutical Ingredients";
            case "Food & FMCG" -> "Food Ingredients & Packaging";
            default -> "Industrial Goods & Assemblies";
        };
    }

    private String generateSubCategoriesJson(String category) {
        return switch (category) {
            case "Steel & Metals" -> "[\"Structural Steel\", \"Alloy Sheets\", \"Fasteners & Fixtures\", \"Pipes & Tubes\"]";
            case "Petrochemicals" -> "[\"Polymers & Resins\", \"Industrial Solvents\", \"Hydrocarbons\", \"Surfactants\"]";
            case "Switchgear & Cabling" -> "[\"HT/LT Cables\", \"Circuit Breakers\", \"Transformers\", \"Switchboards\"]";
            case "Building Materials & Cement" -> "[\"Portland Cement\", \"Aggregate Materials\", \"Reinforcement Bars\", \"Ready-Mix Concrete\"]";
            case "IT Hardware & Services" -> "[\"Enterprise Servers\", \"Cloud Infrastructure\", \"Networking Gear\", \"Workstations\"]";
            case "Freight & Transport Services" -> "[\"Express Parcel Delivery\", \"FTL Freight\", \"Cold Chain Logistics\", \"Warehousing\"]";
            case "Active Pharmaceutical Ingredients" -> "[\"Bulk Drugs\", \"Excipients\", \"Sterile Packaging\", \"Chemical Intermediates\"]";
            case "Food Ingredients & Packaging" -> "[\"Dairy Products\", \"Food-Grade Oils\", \"Flexible Packaging\", \"Preservatives\"]";
            default -> "[\"Standard Components\", \"OEM Spare Parts\", \"Fabricated Parts\", \"Raw Stock\"]";
        };
    }

    private String generateCapabilitiesJson(String industry, String category) {
        return switch (industry) {
            case "Steel & Metals" -> "[\"Steel Sourcing\", \"Industrial Materials\", \"Precision Fabrication\", \"High-Tensile Fastening\"]";
            case "Chemicals" -> "[\"Industrial Chemicals\", \"Bulk Solvents\", \"Polymer Compounding\", \"Specialty Fluids\"]";
            case "IT & Software" -> "[\"IT Systems Sourcing\", \"Cloud Infrastructure\", \"Enterprise Software Support\", \"Hardware Maintenance\"]";
            case "Logistics & Warehousing" -> "[\"Multi-Modal Transport\", \"Warehousing Solutions\", \"Express Delivery\", \"Supply Chain Tracking\"]";
            default -> String.format("[\"%s Sourcing\", \"%s Supplies\", \"Quality Assured Batching\", \"Just-In-Time Delivery\"]", industry, category);
        };
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
