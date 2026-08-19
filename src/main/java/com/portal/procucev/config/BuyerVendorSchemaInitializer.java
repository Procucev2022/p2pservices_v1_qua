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
        log.info("Ensuring Buyer Vendor table exists...");
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
                    pan VARCHAR(10),
                    gstin VARCHAR(15),
                    country VARCHAR(10) DEFAULT 'IN',
                    region_code VARCHAR(10),
                    address_line VARCHAR(255),
                    city VARCHAR(255),
                    district VARCHAR(255),
                    postal_code VARCHAR(6),
                    phone_1 VARCHAR(15),
                    phone_2 VARCHAR(15),
                    type_of_business VARCHAR(255),
                    type_of_industry VARCHAR(255),
                    vendor_group VARCHAR(255),
                    status VARCHAR(255) NOT NULL DEFAULT 'Active',
                    sourcing_scope VARCHAR(255) DEFAULT 'Client Only',
                    buyer_org_id VARCHAR(255) NOT NULL,
                    CONSTRAINT uk_buyer_vendor_code UNIQUE (vendor_code, buyer_org_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            log.info("Buyer Vendor table verified/created successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Buyer Vendor table: {}", e.getMessage(), e);
        }
    }
}
