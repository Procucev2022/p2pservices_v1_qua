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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BuyerVendorServiceImpl implements BuyerVendorService {

    private static final Logger log = LoggerFactory.getLogger(BuyerVendorServiceImpl.class);

    @Autowired
    private BuyerVendorDao buyerVendorDao;

    @Autowired
    private BuyerVendorAiProfileDao buyerVendorAiProfileDao;

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
        if (buyerVendorDao.existsByVendorCodeAndBuyerOrgId(vendor.getVendorCode(), vendor.getBuyerOrgId())) {
            throw new IllegalArgumentException("Vendor code '" + vendor.getVendorCode() + "' already exists for this organization");
        }
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
        Optional<BuyerVendor> vendorOpt = getVendorById(idOrCode, buyerOrgId);
        if (vendorOpt.isPresent()) {
            BuyerVendor v = vendorOpt.get();
            String vendorCode = v.getVendorCode();
            log.info("Deleting vendor id: {}, code: {} for buyerOrgId: {}", v.getId(), vendorCode, buyerOrgId);
            
            // Delete AI profile if exists
            try {
                if (vendorCode != null && !vendorCode.trim().isEmpty()) {
                    buyerVendorAiProfileDao.deleteByVendorCodeAndBuyerOrgId(vendorCode.trim(), buyerOrgId);
                }
            } catch (Exception ex) {
                log.warn("Error deleting AI profile for vendor {}: {}", vendorCode, ex.getMessage());
            }

            // Delete master buyer vendor
            buyerVendorDao.delete(v);
            return true;
        }
        return false;
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

    @Override

    public java.util.Map<String, Object> bulkCreateVendors(java.util.List<BuyerVendor> vendors, String buyerOrgId, String createdBy) {
        ensureTableExists();
        java.util.List<BuyerVendor> toSave = new java.util.ArrayList<>();
        java.util.List<String> skippedCodes = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

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
            if (v.getVendorName() == null || v.getVendorName().trim().length() < 3) {
                errors.add("Vendor '" + v.getVendorCode() + "' has invalid name (min 3 chars).");
                continue;
            }
            if (v.getPhone1() == null || !v.getPhone1().matches("\\d{10}")) {
                errors.add("Vendor '" + v.getVendorCode() + "' has invalid 10-digit phone: " + v.getPhone1());
                continue;
            }

            v.setVendorCode(v.getVendorCode().trim());
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

            try {
                if (buyerVendorDao.existsByVendorCodeAndBuyerOrgId(v.getVendorCode(), buyerOrgId)) {
                    skippedCodes.add(v.getVendorCode());
                } else {
                    toSave.add(v);
                }
            } catch (Exception ex) {
                log.warn("Check exists failed: {}, attempting save", ex.getMessage());
                toSave.add(v);
            }
        }

        if (!toSave.isEmpty()) {
            buyerVendorDao.saveAll(toSave);
        }

        return java.util.Map.of(
            "savedCount", toSave.size(),
            "skippedCount", skippedCodes.size(),
            "totalCount", vendors.size(),
            "skippedCodes", skippedCodes,
            "errors", errors
        );
    }
}
