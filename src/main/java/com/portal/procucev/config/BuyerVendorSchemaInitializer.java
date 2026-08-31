package com.portal.procucev.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class BuyerVendorSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(BuyerVendorSchemaInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeSchema() {
        log.info("Ensuring Buyer Vendor and AI Profile tables exist...");
        try {
            // 1. Buyer Vendor Master Table
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

            // Migration / Alter checks in case table already exists with small varchar
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

            // 2. Buyer Vendor AI Profile Table
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
                    ai_raw_response TEXT,
                    CONSTRAINT uk_buyer_vendor_ai_code UNIQUE (vendor_code, buyer_org_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            try {
                jdbcTemplate.execute("ALTER TABLE buyer_vendor_ai_profile ADD CONSTRAINT uk_buyer_vendor_ai_code UNIQUE (vendor_code, buyer_org_id)");
            } catch (Exception alterEx) {
                log.debug("AI profile unique constraint check note: {}", alterEx.getMessage());
            }

            log.info("Buyer Vendor tables verified/created successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Buyer Vendor tables: {}", e.getMessage(), e);
        }
    }
}
