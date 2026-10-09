package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.LaborWageEntity
import com.example.data.local.entity.MaterialUsageEntity
import com.example.data.local.entity.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ArmbianServerConfig(
    val serverUrl: String = "http://192.168.1.100:8000",
    val apiKey: String = "armbian_secret_token_2026",
    val databaseName: String = "db_rekap_konstruksi",
    val isAutoSyncEnabled: Boolean = false
)

data class SyncResult(
    val success: Boolean,
    val message: String,
    val syncedProjects: Int = 0,
    val syncedMaterials: Int = 0,
    val syncedWages: Int = 0
)

class ArmbianServerService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(serverUrl: String, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (serverUrl.isBlank()) {
            return@withContext Result.failure(Exception("Alamat URL server Armbian tidak boleh kosong"))
        }

        val cleanUrl = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl
        val pingUrl = "$cleanUrl/ping"

        try {
            val request = Request.Builder()
                .url(pingUrl)
                .header("X-API-Key", apiKey)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Koneksi ke Server Armbian MySQL Berhasil! (HTTP ${response.code})")
            } else {
                Result.failure(Exception("Server merespon dengan kode: HTTP ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e("ArmbianServer", "Connection test failed", e)
            Result.failure(Exception("Gagal menghubungi server Armbian (${e.localizedMessage ?: "Timeout / Jaringan tidak terjangkau"}). Pastikan perangkat berada dalam jaringan WiFi/LAN yang sama."))
        }
    }

    suspend fun pushDataToArmbian(
        config: ArmbianServerConfig,
        projects: List<ProjectEntity>,
        materials: List<MaterialUsageEntity>,
        wages: List<LaborWageEntity>
    ): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = if (config.serverUrl.endsWith("/")) config.serverUrl.dropLast(1) else config.serverUrl
        val syncUrl = "$cleanUrl/sync"

        val payload = JSONObject().apply {
            put("database", config.databaseName)
            put("timestamp", System.currentTimeMillis())

            val projArr = JSONArray()
            projects.forEach { p ->
                projArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("code_spk", p.codeSpk)
                    put("location", p.location)
                    put("client_name", p.clientName)
                    put("budget_rab", p.budgetRAB)
                    put("progress_percent", p.progressPercent)
                    put("status", p.status)
                    put("foreman", p.foremanInCharge)
                })
            }
            put("projects", projArr)

            val matArr = JSONArray()
            materials.forEach { m ->
                matArr.put(JSONObject().apply {
                    put("id", m.id)
                    put("project_id", m.projectId)
                    put("material_name", m.materialName)
                    put("category", m.category)
                    put("quantity", m.quantity)
                    put("unit", m.unit)
                    put("unit_price", m.unitPrice)
                    put("total_cost", m.totalCost)
                    put("date", m.date)
                    put("source", m.source)
                    put("notes", m.invoiceOrNote)
                })
            }
            put("materials", matArr)

            val wageArr = JSONArray()
            wages.forEach { w ->
                wageArr.put(JSONObject().apply {
                    put("id", w.id)
                    put("project_id", w.projectId)
                    put("worker_role", w.workerRole)
                    put("worker_count", w.workerCount)
                    put("duration_hok", w.durationHOK)
                    put("wage_rate", w.wagePerUnit)
                    put("total_wage", w.totalWage)
                    put("date", w.date)
                    put("task_description", w.taskDescription)
                    put("source", w.source)
                })
            }
            put("wages", wageArr)
        }

        try {
            val request = Request.Builder()
                .url(syncUrl)
                .header("X-API-Key", config.apiKey)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                SyncResult(
                    success = true,
                    message = "Sinkronisasi berhasil! Data terkirim ke MySQL di Server Armbian.",
                    syncedProjects = projects.size,
                    syncedMaterials = materials.size,
                    syncedWages = wages.size
                )
            } else {
                SyncResult(
                    success = false,
                    message = "Server Armbian menolak sinkronisasi (HTTP ${response.code})."
                )
            }
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Gagal koneksi sinkronisasi: ${e.localizedMessage ?: "Koneksi terputus"}. Periksa IP server."
            )
        }
    }

    /**
     * Generates standard MySQL DDL SQL statements to be executed directly
     * in MySQL console on Armbian (e.g. `mysql -u root -p < schema.sql`).
     */
    fun generateMySqlSchema(databaseName: String = "db_rekap_konstruksi"): String {
        return """
-- ========================================================
-- SKRIP DATABASE MYSQL UNTUK SERVER ARMBIAN
-- Aplikasi Rekap Material & Upah Proyek Konstruksi (AI & Telegram)
-- ========================================================

CREATE DATABASE IF NOT EXISTS `$databaseName` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `$databaseName`;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

-- 4. Tabel Log Chat Telegram Masuk
CREATE TABLE IF NOT EXISTS `telegram_logs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `telegram_msg_id` BIGINT DEFAULT NULL,
  `chat_id` VARCHAR(100) DEFAULT NULL,
  `sender_name` VARCHAR(150) DEFAULT NULL,
  `raw_text` TEXT NOT NULL,
  `received_at` BIGINT NOT NULL,
  `parse_status` VARCHAR(50) DEFAULT 'SUCCESS',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Indeks untuk pencarian cepat
CREATE INDEX idx_material_project_date ON `material_usages` (`project_id`, `date`);
CREATE INDEX idx_labor_project_date ON `labor_wages` (`project_id`, `date`);
        """.trimIndent()
    }
}
