-- phpMyAdmin SQL Dump
-- version 5.1.1
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Jan 25, 2026 at 04:13 PM
-- Server version: 10.11.14-MariaDB
-- PHP Version: 8.1.34

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `visadcouk_dataf`
--

-- --------------------------------------------------------

--
-- Table structure for table `audit_logs`
--

CREATE TABLE `audit_logs` (
  `id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `record_type` varchar(50) DEFAULT NULL,
  `record_id` int(11) DEFAULT NULL,
  `record_name` varchar(255) DEFAULT NULL,
  `field_changed` varchar(100) DEFAULT NULL,
  `old_value` text DEFAULT NULL,
  `new_value` text DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `dependents`
--

CREATE TABLE `dependents` (
  `id` int(11) NOT NULL,
  `traveler_id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT 'New Co-Traveler',
  `title` varchar(10) DEFAULT NULL,
  `travel_country` varchar(255) DEFAULT NULL,
  `visa_center` varchar(255) DEFAULT NULL,
  `package` varchar(100) DEFAULT NULL,
  `visa_type` varchar(100) DEFAULT NULL,
  `status` varchar(100) DEFAULT NULL,
  `doc_date` date DEFAULT NULL,
  `whatsapp_contact` varchar(50) DEFAULT NULL,
  `appointment_remarks` text DEFAULT NULL,
  `visa_link` text DEFAULT NULL,
  `note` text DEFAULT NULL,
  `priority` varchar(255) DEFAULT NULL,
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0 = not pinned, 1 = pinned',
  `pinned_by_username` varchar(50) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `planned_travel_date` date DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `gender` varchar(50) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `place_of_birth` varchar(255) DEFAULT NULL,
  `country_of_birth` varchar(255) DEFAULT NULL,
  `relationship_to_main` varchar(50) DEFAULT NULL,
  `nationality` varchar(100) DEFAULT NULL,
  `passport_no` varchar(100) DEFAULT NULL,
  `passport_issue` date DEFAULT NULL,
  `passport_expire` date DEFAULT NULL,
  `contact_number` varchar(50) DEFAULT NULL,
  `country_code` varchar(10) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `logins` text DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `payment_status` varchar(50) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL COMMENT 'Service price',
  `discount_type` enum('none','percentage','fixed') DEFAULT 'none',
  `discount_value` decimal(10,2) DEFAULT 0.00 COMMENT 'Discount percentage or fixed amount',
  `discount_amount` decimal(10,2) DEFAULT 0.00 COMMENT 'Calculated discount',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT 'Final price after discount',
  `address_line_1` varchar(255) DEFAULT NULL,
  `address_line_2` varchar(255) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `state_province` varchar(100) DEFAULT NULL,
  `zip_code` varchar(20) DEFAULT NULL,
  `country` varchar(255) DEFAULT 'United Kingdom',
  `public_url_token` varchar(64) DEFAULT NULL,
  `application_form_link` text DEFAULT NULL,
  `application_form_username` varchar(255) DEFAULT NULL,
  `application_form_password` varchar(255) DEFAULT NULL,
  `created_by_username` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_updated_by_username` varchar(50) DEFAULT NULL,
  `last_updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `discount_codes`
--

CREATE TABLE `discount_codes` (
  `id` int(11) NOT NULL,
  `code` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `discount_type` enum('percentage','fixed') NOT NULL,
  `discount_value` decimal(10,2) NOT NULL COMMENT 'Percentage or fixed amount',
  `min_order_amount` decimal(10,2) DEFAULT 0.00,
  `max_discount` decimal(10,2) DEFAULT NULL COMMENT 'Maximum discount cap',
  `usage_limit` int(11) DEFAULT NULL COMMENT 'Total times code can be used',
  `usage_count` int(11) DEFAULT 0 COMMENT 'Times code has been used',
  `valid_from` date DEFAULT NULL,
  `valid_until` date DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `documents`
--

CREATE TABLE `documents` (
  `id` int(11) NOT NULL,
  `category` varchar(50) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` int(11) DEFAULT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_size` bigint(20) DEFAULT NULL,
  `file_type` varchar(100) DEFAULT NULL,
  `filename` varchar(255) NOT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `record_id` int(11) NOT NULL,
  `record_type` varchar(20) NOT NULL,
  `uploaded_at` datetime(6) DEFAULT NULL,
  `uploaded_by` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `email_log`
--

CREATE TABLE `email_log` (
  `id` int(11) NOT NULL,
  `record_id` int(11) NOT NULL,
  `record_type` varchar(50) NOT NULL,
  `recipient_email` varchar(255) NOT NULL,
  `sent_by` int(11) NOT NULL,
  `sent_at` datetime NOT NULL DEFAULT current_timestamp(),
  `status` varchar(20) DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `invoices`
--

CREATE TABLE `invoices` (
  `id` int(11) NOT NULL,
  `traveler_id` int(11) NOT NULL COMMENT 'Links to travelers.id',
  `invoice_number` varchar(50) NOT NULL,
  `invoice_type` varchar(20) DEFAULT 'invoice' COMMENT 'invoice, proforma, t-invoice, credit-note',
  `customer_name` varchar(255) NOT NULL,
  `customer_email` varchar(255) DEFAULT NULL,
  `customer_phone` varchar(50) DEFAULT NULL,
  `customer_address` text DEFAULT NULL,
  `invoice_date` date NOT NULL,
  `due_date` date DEFAULT NULL,
  `subtotal` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Total before discount',
  `discount_type` enum('none','percentage','fixed') DEFAULT 'none',
  `discount_percentage` decimal(5,2) DEFAULT 0.00 COMMENT 'Discount percentage',
  `discount_amount` decimal(10,2) DEFAULT 0.00 COMMENT 'Calculated discount in £',
  `discount_calculated` decimal(10,2) DEFAULT 0.00 COMMENT 'Actual discount value applied',
  `discount_reason` varchar(255) DEFAULT NULL COMMENT 'Reason for discount',
  `tax_type` enum('none','inclusive','exclusive') DEFAULT 'none',
  `tax_percentage` decimal(5,2) DEFAULT 0.00 COMMENT 'Tax percentage',
  `tax_amount` decimal(10,2) DEFAULT 0.00 COMMENT 'Calculated tax amount',
  `total_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Final amount after discount and tax',
  `currency` varchar(3) DEFAULT 'GBP',
  `payment_status` enum('Unpaid','Partial','Paid','Refunded','Cancelled') DEFAULT 'Unpaid',
  `payment_method` varchar(50) DEFAULT NULL,
  `payment_date` date DEFAULT NULL,
  `payment_reference` varchar(100) DEFAULT NULL,
  `amount_paid` decimal(10,2) DEFAULT 0.00,
  `amount_outstanding` decimal(10,2) DEFAULT 0.00,
  `visa_country` varchar(255) DEFAULT NULL,
  `visa_type` varchar(100) DEFAULT NULL,
  `package` varchar(100) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `terms` text DEFAULT NULL COMMENT 'Payment terms shown on invoice',
  `footer_message` text DEFAULT NULL COMMENT 'Custom footer message',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE current_timestamp(),
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL COMMENT 'When invoice was emailed',
  `sent_to` varchar(255) DEFAULT NULL COMMENT 'Email address sent to',
  `sent_count` int(11) DEFAULT 0 COMMENT 'Number of times sent'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `invoice_history`
--

CREATE TABLE `invoice_history` (
  `id` int(11) NOT NULL,
  `record_id` int(11) NOT NULL,
  `record_type` varchar(20) NOT NULL DEFAULT 'traveler',
  `invoice_type` varchar(20) NOT NULL,
  `invoice_number` varchar(50) NOT NULL,
  `sent_to_email` varchar(255) NOT NULL,
  `sent_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `invoice_items`
--

CREATE TABLE `invoice_items` (
  `id` int(11) NOT NULL,
  `invoice_id` int(11) NOT NULL COMMENT 'Links to invoices.id',
  `item_type` enum('main','co-traveler') DEFAULT 'main',
  `traveler_id` int(11) DEFAULT NULL COMMENT 'Links to travelers.id for main',
  `dependent_id` int(11) DEFAULT NULL COMMENT 'Links to dependents.id for co-travelers',
  `description` varchar(500) NOT NULL,
  `traveler_name` varchar(255) DEFAULT NULL,
  `package` varchar(100) DEFAULT NULL,
  `visa_type` varchar(100) DEFAULT NULL,
  `visa_country` varchar(255) DEFAULT NULL,
  `quantity` int(11) DEFAULT 1,
  `unit_price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `line_total` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT 'quantity * unit_price',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `item_discount_type` enum('none','percentage','fixed') DEFAULT 'none',
  `item_discount_value` decimal(10,2) DEFAULT 0.00,
  `item_discount_amount` decimal(10,2) DEFAULT 0.00,
  `sort_order` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `invoice_payments`
--

CREATE TABLE `invoice_payments` (
  `id` int(11) NOT NULL,
  `invoice_id` int(11) NOT NULL,
  `payment_date` date NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `payment_method` varchar(50) DEFAULT NULL,
  `reference` varchar(100) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `recorded_by` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `login_profiles`
--

CREATE TABLE `login_profiles` (
  `id` int(11) NOT NULL,
  `profile_name` varchar(255) NOT NULL,
  `url` text DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` text DEFAULT NULL,
  `priority` varchar(50) DEFAULT 'Normal',
  `remark` text DEFAULT NULL,
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0,
  `pinned_by_username` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `package_pricing`
--

CREATE TABLE `package_pricing` (
  `id` int(11) NOT NULL,
  `package_name` varchar(100) NOT NULL,
  `package_code` varchar(50) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `currency` varchar(3) DEFAULT 'GBP',
  `description` text DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `sort_order` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `travelers`
--

CREATE TABLE `travelers` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT 'New Traveler',
  `title` varchar(10) DEFAULT NULL,
  `travel_country` varchar(255) DEFAULT NULL,
  `visa_center` varchar(255) DEFAULT NULL,
  `package` varchar(100) DEFAULT NULL,
  `visa_type` varchar(100) DEFAULT NULL,
  `status` varchar(100) DEFAULT NULL,
  `doc_date` date DEFAULT NULL,
  `whatsapp_contact` varchar(50) DEFAULT NULL,
  `appointment_remarks` text DEFAULT NULL,
  `visa_link` text DEFAULT NULL,
  `note` text DEFAULT NULL,
  `priority` varchar(255) DEFAULT NULL,
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0 = not pinned, 1 = pinned',
  `pinned_by_username` varchar(50) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `planned_travel_date` date DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `gender` varchar(50) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `place_of_birth` varchar(255) DEFAULT NULL,
  `country_of_birth` varchar(255) DEFAULT NULL,
  `relationship_to_main` varchar(50) DEFAULT NULL,
  `nationality` varchar(100) DEFAULT NULL,
  `passport_no` varchar(100) DEFAULT NULL,
  `passport_issue` date DEFAULT NULL,
  `passport_expire` date DEFAULT NULL,
  `contact_number` varchar(50) DEFAULT NULL,
  `country_code` varchar(10) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `logins` text DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `payment_status` varchar(50) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL COMMENT 'Service price',
  `discount_type` varchar(20) DEFAULT NULL,
  `discount_value` decimal(10,2) DEFAULT 0.00 COMMENT 'Discount percentage or fixed amount',
  `discount_amount` decimal(10,2) DEFAULT 0.00 COMMENT 'Calculated discount',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT 'Final price after discount',
  `invoice_number` varchar(50) DEFAULT NULL,
  `address_line_1` varchar(255) DEFAULT NULL,
  `address_line_2` varchar(255) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `state_province` varchar(100) DEFAULT NULL,
  `zip_code` varchar(20) DEFAULT NULL,
  `country` varchar(255) DEFAULT 'United Kingdom',
  `public_url_token` varchar(64) DEFAULT NULL,
  `application_form_link` text DEFAULT NULL,
  `application_form_username` varchar(255) DEFAULT NULL,
  `application_form_password` varchar(255) DEFAULT NULL,
  `is_family` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0 = not family, 1 = is family',
  `created_by_username` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_updated_by_username` varchar(50) DEFAULT NULL,
  `last_updated_at` timestamp NULL DEFAULT NULL,
  `invoice_generated` tinyint(1) DEFAULT 0,
  `invoice_generated_at` datetime DEFAULT NULL,
  `invoice_subtotal` decimal(10,2) DEFAULT NULL,
  `invoice_discount_type` varchar(20) DEFAULT NULL,
  `invoice_discount_value` decimal(10,2) DEFAULT NULL,
  `invoice_discount_amount` decimal(10,2) DEFAULT NULL,
  `invoice_total` decimal(10,2) DEFAULT NULL,
  `invoice_items_json` text DEFAULT NULL,
  `refund_amount` decimal(10,2) DEFAULT 0.00,
  `last_invoice_sent_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `traveler_questions`
--

CREATE TABLE `traveler_questions` (
  `id` int(11) NOT NULL,
  `record_id` int(11) NOT NULL,
  `record_type` enum('traveler','dependent') NOT NULL,
  `occupation_status` varchar(100) DEFAULT NULL COMMENT 'Stores the occupation status like Employee, Student, etc.',
  `occupation_title` varchar(255) DEFAULT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `company_address_1` varchar(255) DEFAULT NULL,
  `company_address_2` varchar(255) DEFAULT NULL,
  `company_city` varchar(100) DEFAULT NULL,
  `company_state` varchar(100) DEFAULT NULL,
  `company_zip` varchar(20) DEFAULT NULL,
  `company_phone` varchar(50) DEFAULT NULL,
  `company_email` varchar(255) DEFAULT NULL,
  `marital_status` varchar(50) DEFAULT NULL,
  `has_credit_card` varchar(10) DEFAULT NULL,
  `travel_covered_by` varchar(50) DEFAULT NULL,
  `fingerprints_taken` varchar(10) DEFAULT NULL,
  `schengen_visa_image` text DEFAULT NULL,
  `travel_date_from` date DEFAULT NULL,
  `travel_date_to` date DEFAULT NULL,
  `primary_destination` varchar(255) DEFAULT NULL,
  `has_stay_booking` varchar(10) DEFAULT NULL,
  `stay_type` varchar(50) DEFAULT NULL COMMENT 'e.g., Tourism, Business',
  `hotel_name` varchar(255) DEFAULT NULL,
  `hotel_address_1` varchar(255) DEFAULT NULL,
  `hotel_address_2` varchar(255) DEFAULT NULL,
  `hotel_city` varchar(100) DEFAULT NULL,
  `hotel_state` varchar(100) DEFAULT NULL,
  `hotel_zip` varchar(20) DEFAULT NULL,
  `hotel_booking_reference` varchar(255) DEFAULT NULL,
  `hotel_contact_number` varchar(50) DEFAULT NULL,
  `inviting_person_first_name` varchar(255) DEFAULT NULL,
  `inviting_person_surname` varchar(255) DEFAULT NULL,
  `inviting_person_phone` varchar(50) DEFAULT NULL,
  `inviting_person_email` varchar(255) DEFAULT NULL,
  `inviting_person_phone_code` varchar(10) DEFAULT NULL,
  `inviting_person_relationship` varchar(100) DEFAULT NULL,
  `inviting_person_address_1` varchar(255) DEFAULT NULL,
  `inviting_person_address_2` varchar(255) DEFAULT NULL,
  `inviting_person_city` varchar(100) DEFAULT NULL,
  `inviting_person_state` varchar(100) DEFAULT NULL,
  `inviting_person_zip` varchar(20) DEFAULT NULL,
  `inviting_company_name` varchar(255) DEFAULT NULL,
  `inviting_company_contact_person` varchar(255) DEFAULT NULL,
  `inviting_company_address_1` varchar(255) DEFAULT NULL,
  `inviting_company_address_2` varchar(255) DEFAULT NULL,
  `inviting_company_city` varchar(100) DEFAULT NULL,
  `inviting_company_state` varchar(100) DEFAULT NULL,
  `inviting_company_zip` varchar(20) DEFAULT NULL,
  `inviting_company_phone` varchar(50) DEFAULT NULL,
  `agreed_to_terms` tinyint(1) DEFAULT 0 COMMENT '1 if user agreed to terms',
  `sponsor_relation` varchar(255) DEFAULT NULL,
  `sponsor_full_name` varchar(255) DEFAULT NULL,
  `sponsor_address_1` varchar(255) DEFAULT NULL,
  `sponsor_address_2` varchar(255) DEFAULT NULL,
  `sponsor_city` varchar(100) DEFAULT NULL,
  `sponsor_state` varchar(100) DEFAULT NULL,
  `sponsor_zip` varchar(20) DEFAULT NULL,
  `sponsor_email` varchar(255) DEFAULT NULL,
  `sponsor_phone` varchar(50) DEFAULT NULL,
  `host_name` varchar(255) DEFAULT NULL,
  `host_phone` varchar(50) DEFAULT NULL,
  `host_company_name` varchar(255) DEFAULT NULL,
  `host_address_1` varchar(255) DEFAULT NULL,
  `host_address_2` varchar(255) DEFAULT NULL,
  `host_city` varchar(100) DEFAULT NULL,
  `host_state` varchar(100) DEFAULT NULL,
  `host_zip` varchar(20) DEFAULT NULL,
  `host_email` varchar(255) DEFAULT NULL,
  `host_company_phone` varchar(50) DEFAULT NULL,
  `has_bookings` varchar(10) DEFAULT NULL,
  `booking_documents_path` text DEFAULT NULL,
  `progress_percentage` int(3) DEFAULT 0,
  `evisa_issue_date` date DEFAULT NULL,
  `evisa_expiry_date` date DEFAULT NULL,
  `evisa_no_date_settled` varchar(10) DEFAULT NULL COMMENT 'Stores ''Yes'' or ''No''',
  `evisa_document_path` text DEFAULT NULL,
  `share_code` varchar(255) DEFAULT NULL,
  `share_code_expiry_date` date DEFAULT NULL,
  `share_code_document_path` text DEFAULT NULL,
  `form_complete` tinyint(1) DEFAULT 0,
  `last_question_index` int(11) DEFAULT 0,
  `additional_notes` text DEFAULT NULL,
  `booking_document` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `evisa_document` varchar(255) DEFAULT NULL,
  `passport_back` varchar(255) DEFAULT NULL,
  `passport_front` varchar(255) DEFAULT NULL,
  `share_code_document` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL DEFAULT 'user',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `visa_urls`
--

CREATE TABLE `visa_urls` (
  `id` int(11) NOT NULL,
  `country` varchar(255) NOT NULL,
  `visa_center` varchar(255) DEFAULT NULL,
  `url` text NOT NULL,
  `application_form_url` text DEFAULT NULL,
  `is_uploaded_file` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_audit_logs_record` (`record_id`,`record_type`),
  ADD KEY `idx_audit_logs_timestamp` (`timestamp`);

--
-- Indexes for table `dependents`
--
ALTER TABLE `dependents`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `public_url_token` (`public_url_token`),
  ADD KEY `traveler_id` (`traveler_id`),
  ADD KEY `idx_relationship` (`relationship_to_main`),
  ADD KEY `idx_traveler_id` (`traveler_id`),
  ADD KEY `idx_dependents_traveler_id` (`traveler_id`),
  ADD KEY `idx_dependents_passport` (`passport_no`);

--
-- Indexes for table `discount_codes`
--
ALTER TABLE `discount_codes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `code` (`code`);

--
-- Indexes for table `documents`
--
ALTER TABLE `documents`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_documents_record` (`record_id`,`record_type`,`category`);

--
-- Indexes for table `email_log`
--
ALTER TABLE `email_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_email_log_record` (`record_id`,`record_type`);

--
-- Indexes for table `invoices`
--
ALTER TABLE `invoices`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_traveler_id` (`traveler_id`),
  ADD KEY `idx_invoice_number` (`invoice_number`),
  ADD KEY `idx_invoice_date` (`invoice_date`),
  ADD KEY `idx_payment_status` (`payment_status`);

--
-- Indexes for table `invoice_history`
--
ALTER TABLE `invoice_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `record_lookup` (`record_id`,`record_type`),
  ADD KEY `idx_invoice_history_record` (`record_id`,`record_type`);

--
-- Indexes for table `invoice_items`
--
ALTER TABLE `invoice_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_invoice_id` (`invoice_id`);

--
-- Indexes for table `invoice_payments`
--
ALTER TABLE `invoice_payments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_invoice` (`invoice_id`);

--
-- Indexes for table `login_profiles`
--
ALTER TABLE `login_profiles`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `package_pricing`
--
ALTER TABLE `package_pricing`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `package_name` (`package_name`);

--
-- Indexes for table `travelers`
--
ALTER TABLE `travelers`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `public_url_token` (`public_url_token`),
  ADD KEY `idx_relationship` (`relationship_to_main`),
  ADD KEY `idx_created_at` (`created_at`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_status_created` (`status`,`DESC`),
  ADD KEY `idx_travelers_passport` (`passport_no`),
  ADD KEY `idx_travelers_email` (`email`),
  ADD KEY `idx_travelers_status` (`status`);

--
-- Indexes for table `traveler_questions`
--
ALTER TABLE `traveler_questions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `record_identifier` (`record_id`,`record_type`),
  ADD KEY `idx_tq_record` (`record_id`,`record_type`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- Indexes for table `visa_urls`
--
ALTER TABLE `visa_urls`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `country_center_unique` (`country`,`visa_center`),
  ADD UNIQUE KEY `UKk6q2fikpgyp8ebt8ge4n5gah4` (`country`,`visa_center`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `audit_logs`
--
ALTER TABLE `audit_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `dependents`
--
ALTER TABLE `dependents`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `discount_codes`
--
ALTER TABLE `discount_codes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `documents`
--
ALTER TABLE `documents`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `email_log`
--
ALTER TABLE `email_log`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `invoices`
--
ALTER TABLE `invoices`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `invoice_history`
--
ALTER TABLE `invoice_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `invoice_items`
--
ALTER TABLE `invoice_items`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `invoice_payments`
--
ALTER TABLE `invoice_payments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `login_profiles`
--
ALTER TABLE `login_profiles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `package_pricing`
--
ALTER TABLE `package_pricing`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `travelers`
--
ALTER TABLE `travelers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `traveler_questions`
--
ALTER TABLE `traveler_questions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `visa_urls`
--
ALTER TABLE `visa_urls`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `dependents`
--
ALTER TABLE `dependents`
  ADD CONSTRAINT `dependents_ibfk_1` FOREIGN KEY (`traveler_id`) REFERENCES `travelers` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `invoice_items`
--
ALTER TABLE `invoice_items`
  ADD CONSTRAINT `fk_invoice_items_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `invoice_payments`
--
ALTER TABLE `invoice_payments`
  ADD CONSTRAINT `fk_payments_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
