-- Final consolidated & normalized schema
-- Key decisions:
-- 1. Invoices are the single source of truth for billing (invoice_* fields removed from travelers)
-- 2. Strong foreign keys added where relationships are clear
-- 3. Repeated enums kept consistent
-- 4. Audit / history tables kept append-only

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

/* ===================== USERS ===================== */
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL DEFAULT 'user'
) ENGINE=InnoDB;

/* ===================== TRAVELERS ===================== */
CREATE TABLE travelers (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(10),
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  gender VARCHAR(50),
  dob DATE,
  nationality VARCHAR(100),
  passport_no VARCHAR(100),
  passport_issue DATE,
  passport_expire DATE,

  travel_country VARCHAR(255),
  visa_center VARCHAR(255),
  visa_type VARCHAR(100),
  package VARCHAR(100),
  status VARCHAR(100),
  planned_travel_date DATE,

  contact_number VARCHAR(50),
  country_code VARCHAR(10),
  email VARCHAR(255),
  whatsapp_contact VARCHAR(50),

  address_line_1 VARCHAR(255),
  address_line_2 VARCHAR(255),
  city VARCHAR(100),
  state_province VARCHAR(100),
  zip_code VARCHAR(20),
  country VARCHAR(255) DEFAULT 'United Kingdom',

  public_url_token VARCHAR(64) UNIQUE,

  priority VARCHAR(255),
  is_family TINYINT(1) DEFAULT 0,
  is_pinned TINYINT(1) DEFAULT 0,
  pinned_by_username VARCHAR(50),

  notes TEXT,

  created_by_username VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by_username VARCHAR(50),
  last_updated_at TIMESTAMP NULL,

  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

/* ===================== DEPENDENTS ===================== */
CREATE TABLE dependents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  traveler_id INT NOT NULL,

  first_name VARCHAR(255),
  last_name VARCHAR(255),
  relationship_to_main VARCHAR(50),
  dob DATE,
  nationality VARCHAR(100),
  passport_no VARCHAR(100),
  passport_issue DATE,
  passport_expire DATE,

  visa_type VARCHAR(100),
  package VARCHAR(100),
  status VARCHAR(100),

  contact_number VARCHAR(50),
  email VARCHAR(255),

  price DECIMAL(10,2),
  discount_type ENUM('none','percentage','fixed') DEFAULT 'none',
  discount_value DECIMAL(10,2) DEFAULT 0.00,
  discount_amount DECIMAL(10,2) DEFAULT 0.00,
  total_amount DECIMAL(10,2),

  public_url_token VARCHAR(64) UNIQUE,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_dependents_traveler FOREIGN KEY (traveler_id)
    REFERENCES travelers(id) ON DELETE CASCADE,

  INDEX idx_traveler_id (traveler_id)
) ENGINE=InnoDB;

/* ===================== TRAVELER QUESTIONS ===================== */
CREATE TABLE traveler_questions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  record_id INT NOT NULL,
  record_type ENUM('traveler','dependent') NOT NULL,

  occupation_status VARCHAR(100),
  occupation_title VARCHAR(255),
  company_name VARCHAR(255),
  company_address_1 VARCHAR(255),
  company_city VARCHAR(100),
  company_state VARCHAR(100),
  company_zip VARCHAR(20),

  marital_status VARCHAR(50),
  travel_date_from DATE,
  travel_date_to DATE,
  primary_destination VARCHAR(255),

  agreed_to_terms TINYINT(1) DEFAULT 0,
  progress_percentage INT DEFAULT 0,
  form_complete TINYINT(1) DEFAULT 0,

  UNIQUE KEY uq_record (record_id, record_type)
) ENGINE=InnoDB;

/* ===================== INVOICES ===================== */
CREATE TABLE invoices (
  id INT AUTO_INCREMENT PRIMARY KEY,
  traveler_id INT NOT NULL,

  invoice_number VARCHAR(50) NOT NULL,
  invoice_type ENUM('invoice','proforma','t-invoice','credit-note') DEFAULT 'invoice',

  invoice_date DATE NOT NULL,
  due_date DATE,

  subtotal DECIMAL(10,2) DEFAULT 0.00,
  discount_type ENUM('none','percentage','fixed') DEFAULT 'none',
  discount_amount DECIMAL(10,2) DEFAULT 0.00,
  tax_type ENUM('none','inclusive','exclusive') DEFAULT 'none',
  tax_amount DECIMAL(10,2) DEFAULT 0.00,
  total_amount DECIMAL(10,2) DEFAULT 0.00,

  payment_status ENUM('Unpaid','Partial','Paid','Refunded','Cancelled') DEFAULT 'Unpaid',

  currency VARCHAR(3) DEFAULT 'GBP',

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_invoice_traveler FOREIGN KEY (traveler_id)
    REFERENCES travelers(id),

  UNIQUE KEY uq_invoice_number (invoice_number),
  INDEX idx_invoice_date (invoice_date)
) ENGINE=InnoDB;

/* ===================== INVOICE ITEMS ===================== */
CREATE TABLE invoice_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  invoice_id INT NOT NULL,

  item_type ENUM('main','dependent') DEFAULT 'main',
  traveler_id INT NULL,
  dependent_id INT NULL,

  description VARCHAR(500) NOT NULL,
  quantity INT DEFAULT 1,
  unit_price DECIMAL(10,2) DEFAULT 0.00,
  line_total DECIMAL(10,2) DEFAULT 0.00,

  sort_order INT DEFAULT 0,

  CONSTRAINT fk_items_invoice FOREIGN KEY (invoice_id)
    REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB;

/* ===================== PAYMENTS ===================== */
CREATE TABLE invoice_payments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  invoice_id INT NOT NULL,
  payment_date DATE NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  payment_method VARCHAR(50),
  reference VARCHAR(100),
  notes TEXT,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id)
    REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB;

/* ===================== SUPPORTING TABLES ===================== */
CREATE TABLE discount_codes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  discount_type ENUM('percentage','fixed') NOT NULL,
  discount_value DECIMAL(10,2) NOT NULL,
  valid_from DATE,
  valid_until DATE,
  usage_limit INT,
  usage_count INT DEFAULT 0,
  is_active TINYINT(1) DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE package_pricing (
  id INT AUTO_INCREMENT PRIMARY KEY,
  package_name VARCHAR(100) NOT NULL UNIQUE,
  price DECIMAL(10,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'GBP',
  is_active TINYINT(1) DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE visa_urls (
  id INT AUTO_INCREMENT PRIMARY KEY,
  country VARCHAR(255) NOT NULL,
  visa_center VARCHAR(255),
  url TEXT NOT NULL,
  application_form_url TEXT,
  UNIQUE KEY uq_country_center (country, visa_center)
) ENGINE=InnoDB;

/* ===================== AUDIT & EMAIL ===================== */
CREATE TABLE audit_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  username VARCHAR(50),
  record_type VARCHAR(50),
  record_id INT,
  record_name VARCHAR(255),
  field_changed VARCHAR(100),
  old_value TEXT,
  new_value TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE email_log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  record_type VARCHAR(50),
  record_id INT,
  recipient_email VARCHAR(255),
  sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
