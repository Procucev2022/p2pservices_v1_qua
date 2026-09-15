package com.portal.procucev.rfq.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class RfqSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeSchema() {
        log.info("Ensuring Email RFQ module database tables exist...");
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rfq_buyers (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "company_name VARCHAR(255), " +
                    "contact_person VARCHAR(255), " +
                    "phone VARCHAR(255), " +
                    "verified BOOLEAN DEFAULT TRUE, " +
                    "org_id VARCHAR(255), " +
                    "user_id VARCHAR(255), " +
                    "city VARCHAR(255), " +
                    "state VARCHAR(255), " +
                    "pincode VARCHAR(255), " +
                    "address TEXT, " +
                    "created_at DATETIME NOT NULL, " +
                    "updated_at DATETIME" +
                    ")");

            try {
                jdbcTemplate.execute("ALTER TABLE rfq_buyers ADD COLUMN address TEXT");
                log.info("Successfully added missing 'address' column to 'rfq_buyers' table.");
            } catch (Exception ignored) {
                // Column already exists or table was newly created
            }

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rfq_email_transactions (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "message_id VARCHAR(255) NOT NULL UNIQUE, " +
                    "subject VARCHAR(255), " +
                    "sender_email VARCHAR(255), " +
                    "status VARCHAR(255), " +
                    "error_message VARCHAR(1000), " +
                    "extraction_json TEXT, " +
                    "created_at DATETIME NOT NULL, " +
                    "updated_at DATETIME" +
                    ")");
            try {
                jdbcTemplate.execute("ALTER TABLE rfq_email_transactions ADD COLUMN extraction_json TEXT");
            } catch (Exception ignored) {
                // Column already exists or table was newly created
            }
            try {
                jdbcTemplate.execute("ALTER TABLE rfq_email_transactions ADD COLUMN email_body LONGTEXT");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE rfq_email_transactions ADD COLUMN attachment_text LONGTEXT");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE rfq_email_transactions ADD COLUMN attachment_paths VARCHAR(2000)");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE rfq_email_transactions ADD COLUMN received_date DATETIME");
            } catch (Exception ignored) {}

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rfq_records (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "rfq_number VARCHAR(255) NOT NULL UNIQUE, " +
                    "buyer_email VARCHAR(255) NOT NULL, " +
                    "status VARCHAR(255), " +
                    "raw_subject VARCHAR(255), " +
                    "items_json TEXT, " +
                    "delivery_location VARCHAR(255), " +
                    "delivery_date VARCHAR(255), " +
                    "created_at DATETIME NOT NULL, " +
                    "updated_at DATETIME" +
                    ")");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rfq_id_reservations (" +
                    "rfq_number VARCHAR(255) PRIMARY KEY, " +
                    "reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rfq_item_records (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "buyer_email VARCHAR(255) NOT NULL, " +
                    "item_description VARCHAR(500) NOT NULL, " +
                    "delivery_date VARCHAR(255), " +
                    "rfq_number VARCHAR(255), " +
                    "category VARCHAR(255), " +
                    "division VARCHAR(255), " +
                    "category_confidence DOUBLE, " +
                    "classification_status VARCHAR(255), " +
                    "created_at DATETIME NOT NULL, " +
                    "INDEX idx_rfq_item_lookup (buyer_email, item_description(150), delivery_date)" +
                    ")");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rfq_ai_token_usage (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "rfq_number VARCHAR(255) NOT NULL, " +
                    "message_id VARCHAR(255), " +
                    "model_name VARCHAR(255), " +
                    "prompt_tokens INT DEFAULT 0, " +
                    "candidate_tokens INT DEFAULT 0, " +
                    "total_tokens INT DEFAULT 0, " +
                    "attempts_count INT DEFAULT 1, " +
                    "estimated_cost_usd DOUBLE DEFAULT 0.0, " +
                    "created_at DATETIME NOT NULL, " +
                    "INDEX idx_rfq_ai_token_rfq_number (rfq_number)" +
                    ")");

            log.info("Email RFQ module database tables verified/created successfully.");

            // ── Ensure CLIENT_RFQ_IDLE exists in master_status ──
            try {
                Integer idleCount = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM master_status WHERE status = 'CLIENT_RFQ_IDLE'", Integer.class);
                if (idleCount == null || idleCount == 0) {
                    String idleUuid = java.util.UUID.randomUUID().toString();
                    jdbcTemplate.update(
                            "INSERT INTO master_status (uuid, status, ui_display, description, created_ts) VALUES (?, ?, ?, ?, ?)",
                            idleUuid, "CLIENT_RFQ_IDLE", "Idle", "Idle state for unverified demo buyer RFQs", new java.util.Date());
                    log.info("Inserted missing CLIENT_RFQ_IDLE into master_status table.");
                }
            } catch (Exception e) {
                log.warn("Could not check/insert CLIENT_RFQ_IDLE in master_status: {}", e.getMessage());
            }

            // ── Migrate any unverified demo buyer RFQs currently showing as New/other to CLIENT_RFQ_IDLE ──
            try {
                int updated = jdbcTemplate.update(
                        "UPDATE rfq_header SET client_status = (SELECT uuid FROM master_status WHERE status = 'CLIENT_RFQ_IDLE' LIMIT 1) " +
                        "WHERE user IN (SELECT uuid FROM user_details WHERE verification_status = 'DEMO_BUYER' OR source_type = 'EMAIL') " +
                        "AND (client_status IS NULL OR client_status IN (SELECT uuid FROM master_status WHERE status <> 'CLIENT_RFQ_IDLE'))"
                );
                if (updated > 0) {
                    log.info("Migrated {} unverified demo buyer RFQs to CLIENT_RFQ_IDLE status.", updated);
                }
            } catch (Exception e) {
                try {
                    int updated = jdbcTemplate.update(
                            "UPDATE rfq_header SET client_status = (SELECT uuid FROM master_status WHERE status = 'CLIENT_RFQ_IDLE' LIMIT 1) " +
                            "WHERE user IN (SELECT id FROM user_details WHERE verification_status = 'DEMO_BUYER' OR source_type = 'EMAIL') " +
                            "AND (client_status IS NULL OR client_status IN (SELECT uuid FROM master_status WHERE status <> 'CLIENT_RFQ_IDLE'))"
                    );
                    if (updated > 0) {
                        log.info("Migrated {} unverified demo buyer RFQs to CLIENT_RFQ_IDLE status (using user id).", updated);
                    }
                } catch (Exception ex) {
                    log.debug("Demo buyer RFQ status sync skipped: {}", ex.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to initialize Email RFQ database tables: {}", e.getMessage(), e);
        }
    }
}
