-- ========================================================
-- DATABASE SCHEMA MYSQL UNTUK SERVER ARMBIAN
-- Aplikasi Rekap Material & Upah Proyek Konstruksi
-- ========================================================

CREATE DATABASE IF NOT EXISTS `db_rekap_konstruksi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `db_rekap_konstruksi`;

-- 1. Tabel Proyek Pembangunan
CREATE TABLE IF NOT EXISTS `projects` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(150) NOT NULL,
  `code_spk` VARCHAR(50) DEFAULT NULL,
  `location` VARCHAR(255) DEFAULT NULL,
  `client_name` VARCHAR(150) DEFAULT NULL,
  `budget_rab` DECIMAL(15, 2) DEFAULT 0.00,
  `start_date` BIGINT DEFAULT NULL,
  `target_end_date` BIGINT DEFAULT NULL,
  `progress_percent` FLOAT DEFAULT 0.0,
  `status` ENUM('Aktif', 'Selesai', 'Ditunda') DEFAULT 'Aktif',
  `foreman_in_charge` VARCHAR(100) DEFAULT NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Tabel Pemakaian Material
CREATE TABLE IF NOT EXISTS `material_usages` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `project_id` BIGINT NOT NULL,
  `date` BIGINT NOT NULL,
  `material_name` VARCHAR(150) NOT NULL,
  `category` VARCHAR(80) DEFAULT 'Semen & Pasir',
  `quantity` DECIMAL(10, 2) NOT NULL,
  `unit` VARCHAR(30) NOT NULL,
  `unit_price` DECIMAL(15, 2) NOT NULL,
  `total_cost` DECIMAL(15, 2) NOT NULL,
  `supplier` VARCHAR(150) DEFAULT NULL,
  `invoice_note` TEXT DEFAULT NULL,
  `source` VARCHAR(50) DEFAULT 'Telegram Bot',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Tabel Upah Tenaga Kerja
CREATE TABLE IF NOT EXISTS `labor_wages` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `project_id` BIGINT NOT NULL,
  `date` BIGINT NOT NULL,
  `worker_role` VARCHAR(100) NOT NULL,
  `worker_count` INT NOT NULL DEFAULT 1,
  `duration_hok` DECIMAL(5, 2) NOT NULL DEFAULT 1.0,
  `wage_rate` DECIMAL(15, 2) NOT NULL,
  `total_wage` DECIMAL(15, 2) NOT NULL,
  `task_description` TEXT DEFAULT NULL,
  `foreman_name` VARCHAR(100) DEFAULT NULL,
  `source` VARCHAR(50) DEFAULT 'Telegram Bot',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Tabel Log Pesan Masuk Bot Telegram
CREATE TABLE IF NOT EXISTS `telegram_logs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `telegram_msg_id` BIGINT DEFAULT NULL,
  `chat_id` VARCHAR(100) DEFAULT NULL,
  `sender_name` VARCHAR(150) DEFAULT NULL,
  `raw_text` TEXT NOT NULL,
  `received_at` BIGINT NOT NULL,
  `parse_status` VARCHAR(50) DEFAULT 'SUCCESS',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_material_project_date ON `material_usages` (`project_id`, `date`);
CREATE INDEX IF NOT EXISTS idx_labor_project_date ON `labor_wages` (`project_id`, `date`);
