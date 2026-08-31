package com.portal.procucev.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.portal.procucev.dao.BuyerVendorAiProfileDao;
import com.portal.procucev.dao.BuyerVendorDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BuyerVendorServiceImpl implements BuyerVendorService {

    private static final Logger log = LoggerFactory.getLogger(BuyerVendorServiceImpl.class);

    @Autowired
    private BuyerVendorDao buyerVendorDao;

    @Autowired
    private BuyerVendorAiProfileDao buyerVendorAiProfileDao;

    @Autowired
    private com.portal.procucev.dao.OrgDao orgDao;

    @Autowired
    private com.portal.procucev.dao.OrgTypeDao orgTypeDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    private void ensureTableExists() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS buyer_vendor (
                    uuid VARCHAR(255) NOT NULL PRIMARY KEY,
                    created_by VARCHAR(255),
                    last_modified_by VARCHAR(255),
                    created_ts DATETIME,
                    last_modified_ts DATETIME,
                    vendor_code VARCHAR(255) NOT NULL,
                    vendor_name VARCHAR(255) NOT NULL,
                    search_term VARCHAR(255),
                    pan VARCHAR(50),
                    gstin VARCHAR(50),
                    country VARCHAR(100) DEFAULT 'IN',
                    region_code VARCHAR(255),
                    address_line VARCHAR(255),
                    city VARCHAR(255),
                    district VARCHAR(255),
                    postal_code VARCHAR(50),
                    phone_1 VARCHAR(50),
                    phone_2 VARCHAR(50),
                    type_of_business VARCHAR(255),
                    type_of_industry VARCHAR(255),
                    vendor_group VARCHAR(255),
                    status VARCHAR(255) NOT NULL DEFAULT 'Active',
                    sourcing_scope VARCHAR(255) DEFAULT 'Client Only',
                    buyer_org_id VARCHAR(255) NOT NULL,
                    CONSTRAINT uk_buyer_vendor_code UNIQUE (vendor_code, buyer_org_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            try {
                jdbcTemplate.execute("ALTER TABLE buyer_vendor MODIFY COLUMN region_code VARCHAR(255)");
                jdbcTemplate.execute("ALTER TABLE buyer_vendor MODIFY COLUMN country VARCHAR(100)");
                jdbcTemplate.execute("ALTER TABLE buyer_vendor MODIFY COLUMN postal_code VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE buyer_vendor MODIFY COLUMN pan VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE buyer_vendor MODIFY COLUMN gstin VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE buyer_vendor MODIFY COLUMN phone_1 VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE buyer_vendor MODIFY COLUMN phone_2 VARCHAR(50)");
            } catch (Exception alterEx) {
                log.debug("Column alter check note: {}", alterEx.getMessage());
            }

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
    public Page<BuyerVendor> getVendors(String buyerOrgId, String status, String industry, String search, Pageable pageable) {
        ensureTableExists();
        String statusParam = (status != null && !status.isEmpty()) ? status : null;
        String industryParam = (industry != null && !industry.isEmpty()) ? industry : null;
        String searchParam = (search != null && !search.isEmpty()) ? search : null;
        return buyerVendorDao.findByBuyerOrgFiltered(buyerOrgId, statusParam, industryParam, searchParam, pageable);
    }

    @Override
    public BuyerVendor createVendor(BuyerVendor vendor) {
        ensureTableExists();
        String rawCode = vendor.getVendorCode() != null ? vendor.getVendorCode().trim() : "VND-1001";
        String finalCode = rawCode;
        int counter = 1;
        while (buyerVendorDao.existsByVendorCodeAndBuyerOrgId(finalCode, vendor.getBuyerOrgId())) {
            finalCode = rawCode + "_" + counter;
            counter++;
        }
        vendor.setVendorCode(finalCode);
        return buyerVendorDao.save(vendor);
    }

    @Override
    public Optional<BuyerVendor> getVendorById(String idOrCode, String buyerOrgId) {
        ensureTableExists();
        // First try by database UUID primary key
        Optional<BuyerVendor> byId = buyerVendorDao.findByIdAndBuyerOrgId(idOrCode, buyerOrgId);
        if (byId.isPresent()) {
            return byId;
        }
        // Fallback search by vendor code (e.g. VND-001)
        return buyerVendorDao.findByVendorCodeAndBuyerOrgId(idOrCode, buyerOrgId);
    }

    @Override
    public BuyerVendor updateVendor(String idOrCode, BuyerVendor vendor, String buyerOrgId) {
        ensureTableExists();
        Optional<BuyerVendor> existing = getVendorById(idOrCode, buyerOrgId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Vendor not found");
        }
        BuyerVendor entity = existing.get();
        entity.setVendorCode(vendor.getVendorCode());
        entity.setVendorName(vendor.getVendorName());
        entity.setSearchTerm(vendor.getSearchTerm());
        entity.setPan(vendor.getPan());
        entity.setGstin(vendor.getGstin());
        entity.setCountry(vendor.getCountry());
        entity.setRegionCode(vendor.getRegionCode());
        entity.setAddressLine(vendor.getAddressLine());
        entity.setCity(vendor.getCity());
        entity.setDistrict(vendor.getDistrict());
        entity.setPostalCode(vendor.getPostalCode());
        entity.setPhone1(vendor.getPhone1());
        entity.setPhone2(vendor.getPhone2());
        entity.setTypeOfBusiness(vendor.getTypeOfBusiness());
        entity.setTypeOfIndustry(vendor.getTypeOfIndustry());
        entity.setVendorGroup(vendor.getVendorGroup());
        entity.setSourcingScope(vendor.getSourcingScope());
        return buyerVendorDao.save(entity);
    }

    @Override
    public boolean updateStatus(String idOrCode, String buyerOrgId, String status) {
        ensureTableExists();
        Optional<BuyerVendor> vendorOpt = getVendorById(idOrCode, buyerOrgId);
        if (vendorOpt.isPresent()) {
            BuyerVendor v = vendorOpt.get();
            v.setStatus(status);
            buyerVendorDao.save(v);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteVendor(String idOrCode, String buyerOrgId) {
        ensureTableExists();
        if (idOrCode == null || idOrCode.trim().isEmpty()) {
            return false;
        }
        String key = idOrCode.trim();
        boolean deletedAny = false;
        String resolvedVendorCode = null;

        // 1. Try finding AI Profile by primary key UUID or vendorCode
        Optional<BuyerVendorAiProfile> aiById = buyerVendorAiProfileDao.findById(key);
        if (aiById.isPresent() && (aiById.get().getBuyerOrgId() == null || buyerOrgId.equals(aiById.get().getBuyerOrgId()))) {
            resolvedVendorCode = aiById.get().getVendorCode();
            log.info("Deleting AI profile by id: {}, code: {} for buyerOrgId: {}", key, resolvedVendorCode, buyerOrgId);
            buyerVendorAiProfileDao.delete(aiById.get());
            deletedAny = true;
        } else {
            Optional<BuyerVendorAiProfile> aiByCode = buyerVendorAiProfileDao.findByVendorCodeAndBuyerOrgId(key, buyerOrgId);
            if (aiByCode.isPresent()) {
                resolvedVendorCode = aiByCode.get().getVendorCode();
                log.info("Deleting AI profile by vendorCode: {} for buyerOrgId: {}", resolvedVendorCode, buyerOrgId);
                buyerVendorAiProfileDao.delete(aiByCode.get());
                deletedAny = true;
            }
        }

        // 2. Try finding Master BuyerVendor by primary key UUID or resolved vendorCode or key
        Optional<BuyerVendor> masterById = buyerVendorDao.findByIdAndBuyerOrgId(key, buyerOrgId);
        if (masterById.isPresent()) {
            String code = masterById.get().getVendorCode();
            log.info("Deleting master buyer vendor by id: {}, code: {} for buyerOrgId: {}", key, code, buyerOrgId);
            buyerVendorDao.delete(masterById.get());
            deletedAny = true;
            if (resolvedVendorCode == null) {
                resolvedVendorCode = code;
            }
        }

        String codeToLookup = resolvedVendorCode != null ? resolvedVendorCode : key;
        Optional<BuyerVendor> masterByCode = buyerVendorDao.findByVendorCodeAndBuyerOrgId(codeToLookup, buyerOrgId);
        if (masterByCode.isPresent()) {
            log.info("Deleting master buyer vendor by code: {} for buyerOrgId: {}", codeToLookup, buyerOrgId);
            buyerVendorDao.delete(masterByCode.get());
            deletedAny = true;
        }

        // 3. Final safety cleanup of AI profile if resolvedVendorCode was discovered from master record
        if (resolvedVendorCode != null) {
            try {
                buyerVendorAiProfileDao.deleteByVendorCodeAndBuyerOrgId(resolvedVendorCode, buyerOrgId);
            } catch (Exception ignored) {}
        }

        return deletedAny;
    }

    @Override
    @Transactional
    public int bulkDeleteVendors(java.util.List<String> idsOrCodes, String buyerOrgId) {
        ensureTableExists();
        if (idsOrCodes == null || idsOrCodes.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (String idOrCode : idsOrCodes) {
            if (idOrCode != null && !idOrCode.trim().isEmpty()) {
                boolean deleted = deleteVendor(idOrCode.trim(), buyerOrgId);
                if (deleted) {
                    deletedCount++;
                }
            }
        }
        log.info("Bulk deleted {} vendors for buyerOrgId: {}", deletedCount, buyerOrgId);
        return deletedCount;
    }
    public java.util.Map<String, Object> bulkCreateVendors(java.util.List<BuyerVendor> vendors, String buyerOrgId, String createdBy) {
        ensureTableExists();
        java.util.List<BuyerVendor> toSave = new java.util.ArrayList<>();
        java.util.List<String> skippedCodes = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.Set<String> seenCodes = new java.util.HashSet<>();

        if (vendors == null || vendors.isEmpty()) {
            return java.util.Map.of(
                "savedCount", 0,
                "skippedCount", 0,
                "totalCount", 0,
                "skippedCodes", skippedCodes,
                "errors", errors
            );
        }

        for (BuyerVendor v : vendors) {
            if (v.getVendorCode() == null || v.getVendorCode().trim().isEmpty()) {
                errors.add("A row is missing Vendor Code and was skipped.");
                continue;
            }
            String rawCode = v.getVendorCode().trim();
            String normalizedCode = rawCode.toUpperCase(java.util.Locale.ROOT);

            if (v.getVendorName() == null || v.getVendorName().trim().length() < 3) {
                errors.add("Vendor '" + rawCode + "' has invalid name (min 3 chars).");
                continue;
            }
            if (v.getPhone1() == null || !v.getPhone1().matches("\\d{10}")) {
                errors.add("Vendor '" + rawCode + "' has invalid 10-digit phone: " + v.getPhone1());
                continue;
            }

            // Ensure unique vendor code without overwriting existing data
            String finalCode = rawCode;
            int counter = 1;
            while (seenCodes.contains(finalCode.toUpperCase(java.util.Locale.ROOT)) || buyerVendorDao.existsByVendorCodeAndBuyerOrgId(finalCode, buyerOrgId)) {
                finalCode = rawCode + "_" + counter;
                counter++;
            }
            seenCodes.add(finalCode.toUpperCase(java.util.Locale.ROOT));

            v.setVendorCode(finalCode);
            v.setBuyerOrgId(buyerOrgId);
            v.setCreatedBy(createdBy);

            if (v.getStatus() == null || v.getStatus().trim().isEmpty()) {
                v.setStatus("Active");
            }
            if (v.getSourcingScope() == null || v.getSourcingScope().trim().isEmpty()) {
                v.setSourcingScope("Client Only");
            }
            if (v.getCountry() == null || v.getCountry().trim().isEmpty()) {
                v.setCountry("IN");
            }

            toSave.add(v);
        }

        java.util.List<BuyerVendor> saved = new java.util.ArrayList<>();
        if (!toSave.isEmpty()) {
            saved = buyerVendorDao.saveAll(toSave);
        }

        // The saved records are returned so the caller can enrich them using the
        // final vendor codes. Codes are suffixed on collision, so enriching the
        // submitted payload instead would target the wrong master record.
        return java.util.Map.of(
            "savedCount", toSave.size(),
            "skippedCount", skippedCodes.size(),
            "totalCount", vendors.size(),
            "skippedCodes", skippedCodes,
            "errors", errors,
            "vendors", saved
        );
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getProcucevRecommendations(String category, int limit) {
        ensureTableExists();
        int maxLimit = limit > 0 ? limit : 100;
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Set<String> addedKeys = new java.util.HashSet<>();

        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, maxLimit);
            
            // 1. Query verified platform vendors from the Organization table
            com.portal.procucev.model.OrgType vendorType = orgTypeDao.findByTypeName("VENDOR");
            if (vendorType == null) {
                vendorType = orgTypeDao.findByTypeName("Vendor");
            }
            if (vendorType != null) {
                org.springframework.data.domain.Page<com.portal.procucev.Dto.VendorRFQDto> platformVendors = orgDao.getAllVendor(vendorType, pageable);
                if (platformVendors != null && platformVendors.hasContent()) {
                    int scoreBase = 96;
                    double ratingBase = 4.9;
                    for (com.portal.procucev.Dto.VendorRFQDto v : platformVendors.getContent()) {
                        String key = (v.getCompanyName() != null ? v.getCompanyName() : v.getId()).toLowerCase();
                        if (!addedKeys.contains(key)) {
                            addedKeys.add(key);
                            java.util.Map<String, Object> map = new java.util.HashMap<>();
                            map.put("id", v.getId() != null ? v.getId() : ("PRC-" + (1000 + result.size())));
                            map.put("vendorCode", v.getVendorId() != null ? ("PRC-" + v.getVendorId()) : ("PRC-" + (1000 + result.size())));
                            map.put("name", v.getCompanyName());
                            map.put("category", category != null && !category.isEmpty() ? category : "Industrial Supplies");
                            map.put("location", v.getCity() != null ? v.getCity() + ", India" : "India");
                            map.put("rating", Math.round((ratingBase - (result.size() * 0.05)) * 10.0) / 10.0);
                            map.put("matchScore", Math.max(85, scoreBase - (result.size() * 2)));
                            map.put("proximity", v.getCity() != null ? "Local Hub (" + v.getCity() + ")" : "Regional Hub (<500km)");
                            map.put("status", "Active");
                            map.put("sourcingScope", "Procucev Network");
                            map.put("isProcucevVendor", true);
                            map.put("origin", "Procucev Network");
                            result.add(map);
                            if (result.size() >= maxLimit) {
                                break;
                            }
                        }
                    }
                }
            }

            // 2. Query all Category Manager network suppliers from buyer_vendor table
            java.util.List<BuyerVendor> allDbVendors = buyerVendorDao.findAllProcucevNetworkVendors(pageable);
            if (allDbVendors != null && !allDbVendors.isEmpty()) {
                int scoreBase = 96;
                double ratingBase = 4.8;
                for (BuyerVendor v : allDbVendors) {
                    String key = (v.getVendorName() != null ? v.getVendorName() : v.getVendorCode()).toLowerCase();
                    if (!addedKeys.contains(key)) {
                        addedKeys.add(key);
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        String vCode = v.getVendorCode() != null ? (v.getVendorCode().startsWith("PRC-") ? v.getVendorCode() : ("PRC-" + v.getVendorCode())) : ("PRC-" + (2000 + result.size()));
                        map.put("id", v.getId() != null ? v.getId() : vCode);
                        map.put("vendorCode", vCode);
                        map.put("name", v.getVendorName());
                        map.put("category", v.getTypeOfIndustry() != null ? v.getTypeOfIndustry() : (v.getTypeOfBusiness() != null ? v.getTypeOfBusiness() : "Industrial Supplies"));
                        String loc = (v.getCity() != null ? v.getCity() : "") + (v.getRegionCode() != null ? ", " + v.getRegionCode() : "");
                        map.put("location", !loc.trim().isEmpty() ? loc : (v.getCity() != null ? v.getCity() : "India"));
                        map.put("rating", Math.round((ratingBase - (result.size() * 0.05)) * 10.0) / 10.0);
                        map.put("matchScore", Math.max(82, scoreBase - (result.size() * 2)));
                        map.put("proximity", v.getCity() != null ? "Local Hub (" + v.getCity() + ")" : "Regional Hub (<500km)");
                        map.put("status", v.getStatus());
                        map.put("sourcingScope", "Procucev Network");
                        map.put("isProcucevVendor", true);
                        map.put("origin", "Procucev Network");
                        result.add(map);
                        if (result.size() >= maxLimit) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not query DB for Procucev recommendations: {}", ex.getMessage());
        }

        return result;
    }
}
