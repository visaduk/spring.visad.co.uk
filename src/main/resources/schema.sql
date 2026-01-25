-- Generated Schema based on JPA Entities
-- Compatible with existing database (INT IDs)

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users (Entity: User.java)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Travelers (Entity: Traveler.java)
CREATE TABLE IF NOT EXISTS travelers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    
    -- Personal
    name VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    title VARCHAR(10),
    gender VARCHAR(10),
    dob DATE,
    place_of_birth VARCHAR(100),
    country_of_birth VARCHAR(100),
    nationality VARCHAR(100),
    
    -- Contact
    email VARCHAR(150),
    contact_number VARCHAR(30),
    whatsapp_contact VARCHAR(30),
    
    -- Address
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Passport
    passport_no VARCHAR(50),
    passport_issue DATE,
    passport_expire DATE,
    
    -- Visa
    travel_country VARCHAR(100),
    visa_center VARCHAR(150),
    visa_type VARCHAR(100),
    visa_link VARCHAR(500),
    application_form_link VARCHAR(500),
    application_form_username VARCHAR(100),
    application_form_password VARCHAR(100),
    package VARCHAR(100),
    
    -- Status
    status VARCHAR(50) DEFAULT 'Wait App',
    priority VARCHAR(20) DEFAULT 'Normal',
    payment_status VARCHAR(50),
    
    -- Dates
    planned_travel_date DATE,
    doc_date DATE,
    
    -- Notes
    note TEXT,
    notes TEXT,
    appointment_remarks TEXT,
    
    -- Account
    username VARCHAR(100),
    logins TEXT,
    
    -- Family
    is_family BOOLEAN DEFAULT FALSE,
    relationship_to_main VARCHAR(50),
    
    -- Public URL
    public_url_token VARCHAR(20),
    
    -- Pricing / Invoice
    price DECIMAL(10, 2),
    discount_type VARCHAR(20),
    discount_value DECIMAL(10, 2),
    refund_amount DECIMAL(10, 2),
    
    -- Invoice Fields
    invoice_subtotal DECIMAL(10, 2),
    invoice_discount_type VARCHAR(20),
    invoice_discount_value DECIMAL(10, 2),
    invoice_discount_amount DECIMAL(10, 2),
    invoice_total DECIMAL(10, 2),
    invoice_items_json TEXT,
    invoice_generated BOOLEAN DEFAULT FALSE,
    invoice_generated_at TIMESTAMP NULL,
    
    -- Audit
    created_by_username VARCHAR(50),
    last_updated_by_username VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_travelers_passport (passport_no),
    INDEX idx_travelers_email (email),
    INDEX idx_travelers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Dependents (Entity: Dependent.java)
CREATE TABLE IF NOT EXISTS dependents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    traveler_id INT NOT NULL,
    
    -- Personal
    name VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    title VARCHAR(10),
    gender VARCHAR(10),
    dob DATE,
    place_of_birth VARCHAR(100),
    country_of_birth VARCHAR(100),
    nationality VARCHAR(100),
    relationship_to_main VARCHAR(50),
    
    -- Contact
    email VARCHAR(150),
    contact_number VARCHAR(30),
    whatsapp_contact VARCHAR(30),
    
    -- Address
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Passport
    passport_no VARCHAR(50),
    passport_issue DATE,
    passport_expire DATE,
    
    -- Visa
    travel_country VARCHAR(100),
    visa_center VARCHAR(150),
    visa_type VARCHAR(100),
    visa_link VARCHAR(500),
    application_form_link VARCHAR(500),
    application_form_username VARCHAR(100),
    application_form_password VARCHAR(100),
    package VARCHAR(100),
    
    -- Status
    status VARCHAR(50) DEFAULT 'Wait App',
    priority VARCHAR(20) DEFAULT 'Normal',
    payment_status VARCHAR(50),
    
    -- Dates
    planned_travel_date DATE,
    doc_date DATE,
    
    -- Notes
    note TEXT,
    notes TEXT,
    appointment_remarks TEXT,
    
    -- Account
    username VARCHAR(100),
    logins TEXT,
    
    -- Public URL
    public_url_token VARCHAR(20),
    
    -- Pricing
    price DECIMAL(10, 2),
    
    -- Audit
    created_by_username VARCHAR(50),
    last_updated_by_username VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_dependents_traveler FOREIGN KEY (traveler_id) REFERENCES travelers(id) ON DELETE CASCADE,
    INDEX idx_dependents_traveler_id (traveler_id),
    INDEX idx_dependents_passport (passport_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. TravelerQuestions (Entity: TravelerQuestions.java)
CREATE TABLE IF NOT EXISTS traveler_questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    record_id INT NOT NULL,
    record_type VARCHAR(20) NOT NULL,
    
    -- Travel Dates
    travel_date_from DATE,
    travel_date_to DATE,
    
    -- Progress
    progress_percentage INT,
    form_complete BOOLEAN DEFAULT FALSE,
    
    -- eVisa
    evisa_issue_date DATE,
    evisa_expiry_date DATE,
    evisa_document VARCHAR(255),
    
    -- Share Code
    share_code VARCHAR(50),
    share_code_expiry_date DATE,
    share_code_document VARCHAR(255),
    
    -- Documents
    booking_document VARCHAR(255),
    passport_front VARCHAR(255),
    passport_back VARCHAR(255),
    
    -- Notes
    additional_notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_tq_record (record_id, record_type),
    INDEX idx_tq_record (record_id, record_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Documents (Entity: Document.java)
-- This table is missing from your current database!
CREATE TABLE IF NOT EXISTS documents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    record_id INT NOT NULL,
    record_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    uploaded_by INT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    deleted_by INT,
    
    INDEX idx_documents_record (record_id, record_type, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. AuditLogs (Entity: AuditLog.java)
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    username VARCHAR(50),
    record_type VARCHAR(20) NOT NULL,
    record_id INT NOT NULL,
    record_name VARCHAR(255),
    field_changed VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_audit_logs_record (record_id, record_type),
    INDEX idx_audit_logs_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. VisaUrls (Entity: VisaUrl.java)
CREATE TABLE IF NOT EXISTS visa_urls (
    id INT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(100) NOT NULL,
    visa_center VARCHAR(150),
    url VARCHAR(500) NOT NULL,
    application_form_url VARCHAR(500),
    is_uploaded_file BOOLEAN DEFAULT FALSE,
    
    UNIQUE KEY uk_visa_urls (country, visa_center)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. InvoiceHistory (Entity: InvoiceHistory.java)
CREATE TABLE IF NOT EXISTS invoice_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    record_id INT NOT NULL,
    record_type VARCHAR(20) NOT NULL,
    invoice_type VARCHAR(20),
    invoice_number VARCHAR(50),
    sent_to_email VARCHAR(150),
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_invoice_history_record (record_id, record_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. EmailLog (Entity: EmailLog.java)
CREATE TABLE IF NOT EXISTS email_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    record_id INT NOT NULL,
    record_type VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    sent_by INT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subject VARCHAR(255),
    status VARCHAR(20) DEFAULT 'sent',
    
    INDEX idx_email_log_record (record_id, record_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
