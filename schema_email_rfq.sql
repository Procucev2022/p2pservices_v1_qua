-- SQL DDL for Email RFQ Automation tables in MySQL (development_gmtbfs)

CREATE TABLE IF NOT EXISTS rfq_buyers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    company_name VARCHAR(255),
    contact_person VARCHAR(255),
    phone VARCHAR(255),
    verified BOOLEAN DEFAULT TRUE,
    org_id VARCHAR(255),
    user_id VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    pincode VARCHAR(255),
    address TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS rfq_email_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    subject VARCHAR(255),
    sender_email VARCHAR(255),
    status VARCHAR(255),
    error_message VARCHAR(1000),
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS rfq_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_number VARCHAR(255) NOT NULL UNIQUE,
    buyer_email VARCHAR(255) NOT NULL,
    status VARCHAR(255),
    raw_subject VARCHAR(255),
    items_json TEXT,
    delivery_location VARCHAR(255),
    delivery_date VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS rfq_item_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_email VARCHAR(255) NOT NULL,
    item_description VARCHAR(500) NOT NULL,
    delivery_date VARCHAR(255),
    rfq_number VARCHAR(255),
    category VARCHAR(255),
    division VARCHAR(255),
    category_confidence DOUBLE,
    classification_status VARCHAR(255),
    created_at DATETIME NOT NULL,
    INDEX idx_rfq_item_lookup (buyer_email, item_description, delivery_date)
);
