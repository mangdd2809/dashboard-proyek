package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AIAnalysisDao
import com.example.data.local.dao.LaborWageDao
import com.example.data.local.dao.MaterialUsageDao
import com.example.data.local.dao.ProgressMilestoneDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TelegramMessageDao
import com.example.data.local.entity.AIAnalysisEntity
import com.example.data.local.entity.LaborWageEntity
import com.example.data.local.entity.MaterialUsageEntity
import com.example.data.local.entity.ProgressMilestoneEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TelegramMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProjectEntity::class,
        MaterialUsageEntity::class,
        LaborWageEntity::class,
        ProgressMilestoneEntity::class,
        TelegramMessageEntity::class,
        AIAnalysisEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun materialDao(): MaterialUsageDao
    abstract fun laborDao(): LaborWageDao
    abstract fun progressDao(): ProgressMilestoneDao
    abstract fun telegramDao(): TelegramMessageDao
    abstract fun aiAnalysisDao(): AIAnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rekap_proyek_ai.db"
                ).addCallback(DatabaseCallback()).build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val projectDao = database.projectDao()
            val materialDao = database.materialDao()
            val laborDao = database.laborDao()
            val progressDao = database.progressDao()
            val telegramDao = database.telegramDao()

            val now = System.currentTimeMillis()
            val oneDay = 24 * 3600 * 1000L

            // 1. Proyek 1: Griya Indah
            val p1Id = projectDao.insertProject(
                ProjectEntity(
                    id = 1,
                    name = "Perumahan Griya Indah Blok C",
                    codeSpk = "SPK-2026-GI01",
                    location = "Jl. Melati No. 12, Sleman",
                    clientName = "PT Griya Sentosa Mandiri",
                    budgetRAB = 450_000_000.0,
                    startDate = now - (30 * oneDay),
                    targetEndDate = now + (60 * oneDay),
                    progressPercent = 68.5f,
                    status = "Aktif",
                    foremanInCharge = "Mandor Budi",
                    updatedAt = now
                )
            )

            // 2. Proyek 2: Ruko Sudirman
            val p2Id = projectDao.insertProject(
                ProjectEntity(
                    id = 2,
                    name = "Renovasi Ruko Sudirman 3 Lt",
                    codeSpk = "SPK-2026-RS03",
                    location = "Kawasan Bisnis Sudirman",
                    clientName = "Bpk. Hendra Wijaya",
                    budgetRAB = 280_000_000.0,
                    startDate = now - (15 * oneDay),
                    targetEndDate = now + (45 * oneDay),
                    progressPercent = 42.0f,
                    status = "Aktif",
                    foremanInCharge = "Mandor Agus",
                    updatedAt = now
                )
            )

            // 3. Proyek 3: Gudang Logistik
            val p3Id = projectDao.insertProject(
                ProjectEntity(
                    id = 3,
                    name = "Gudang Logistik Sentul",
                    codeSpk = "SPK-2026-GL08",
                    location = "Sentul Industrial Park",
                    clientName = "PT Cipta Logistik Nusantara",
                    budgetRAB = 850_000_000.0,
                    startDate = now - (60 * oneDay),
                    targetEndDate = now + (90 * oneDay),
                    progressPercent = 88.0f,
                    status = "Aktif",
                    foremanInCharge = "Mandor Danu",
                    updatedAt = now
                )
            )

            // Materials for Proyek 1
            materialDao.insertMaterials(
                listOf(
                    MaterialUsageEntity(
                        projectId = p1Id,
                        date = now - (1 * oneDay),
                        materialName = "Semen Gresik 40kg",
                        category = "Semen & Pasir",
                        quantity = 50.0,
                        unit = "sak",
                        unitPrice = 68_000.0,
                        totalCost = 3_400_000.0,
                        supplier = "TB Sumber Rejeki",
                        invoiceOrNote = "Nota #8821 - Pengecoran kolom",
                        source = "Telegram Bot"
                    ),
                    MaterialUsageEntity(
                        projectId = p1Id,
                        date = now - (2 * oneDay),
                        materialName = "Besi Beton Ulir 12mm",
                        category = "Besi & Baja",
                        quantity = 40.0,
                        unit = "btg",
                        unitPrice = 115_000.0,
                        totalCost = 4_600_000.0,
                        supplier = "Distributor Baja Mandiri",
                        invoiceOrNote = "Pengiriman batch 2",
                        source = "Telegram Bot"
                    ),
                    MaterialUsageEntity(
                        projectId = p1Id,
                        date = now - (3 * oneDay),
                        materialName = "Pasir Pasang Merapi",
                        category = "Semen & Pasir",
                        quantity = 3.0,
                        unit = "rit",
                        unitPrice = 750_000.0,
                        totalCost = 2_250_000.0,
                        supplier = "CV Pasir Alam",
                        invoiceOrNote = "Pasir pasang plesteran",
                        source = "Manual"
                    ),
                    MaterialUsageEntity(
                        projectId = p1Id,
                        date = now - (4 * oneDay),
                        materialName = "Bata Ringan Hebel 7.5cm",
                        category = "Bata & Dinding",
                        quantity = 12.0,
                        unit = "m3",
                        unitPrice = 620_000.0,
                        totalCost = 7_440_000.0,
                        supplier = "Pabrik Hebel Jaya",
                        invoiceOrNote = "Dinding lantai 2",
                        source = "Telegram Bot"
                    )
                )
            )

            // Materials for Proyek 2
            materialDao.insertMaterials(
                listOf(
                    MaterialUsageEntity(
                        projectId = p2Id,
                        date = now - (1 * oneDay),
                        materialName = "Keramik Granit 60x60 Glazed",
                        category = "Finishing & Cat",
                        quantity = 35.0,
                        unit = "box",
                        unitPrice = 185_000.0,
                        totalCost = 6_475_000.0,
                        supplier = "Mitra 10",
                        invoiceOrNote = "Lantai dasar toko",
                        source = "Telegram Bot"
                    ),
                    MaterialUsageEntity(
                        projectId = p2Id,
                        date = now - (2 * oneDay),
                        materialName = "Cat Dinding WeatherShield 20L",
                        category = "Finishing & Cat",
                        quantity = 4.0,
                        unit = "pail",
                        unitPrice = 850_000.0,
                        totalCost = 3_400_000.0,
                        supplier = "Toko Cat Warna Indah",
                        invoiceOrNote = "Fasad ruko",
                        source = "Manual"
                    )
                )
            )

            // Labor Wages for Proyek 1
            laborDao.insertLaborWages(
                listOf(
                    LaborWageEntity(
                        projectId = p1Id,
                        date = now - (1 * oneDay),
                        workerRole = "Tukang Batu/Cor",
                        workerCount = 4,
                        durationHOK = 1.0,
                        wagePerUnit = 130_000.0,
                        totalWage = 520_000.0,
                        taskDescription = "Pengecoran balok & kolom lantai 2",
                        foremanName = "Mandor Budi",
                        source = "Telegram Bot"
                    ),
                    LaborWageEntity(
                        projectId = p1Id,
                        date = now - (1 * oneDay),
                        workerRole = "Kenek/Pembantu",
                        workerCount = 3,
                        durationHOK = 1.0,
                        wagePerUnit = 95_000.0,
                        totalWage = 285_000.0,
                        taskDescription = "Aduk adukan cor & angkut pasir",
                        foremanName = "Mandor Budi",
                        source = "Telegram Bot"
                    ),
                    LaborWageEntity(
                        projectId = p1Id,
                        date = now - (2 * oneDay),
                        workerRole = "Tukang Besi",
                        workerCount = 3,
                        durationHOK = 1.0,
                        wagePerUnit = 135_000.0,
                        totalWage = 405_000.0,
                        taskDescription = "Perakitan pembesian plat lantai",
                        foremanName = "Mandor Budi",
                        source = "Telegram Bot"
                    ),
                    LaborWageEntity(
                        projectId = p1Id,
                        date = now - (2 * oneDay),
                        workerRole = "Kenek/Pembantu",
                        workerCount = 2,
                        durationHOK = 1.0,
                        wagePerUnit = 95_000.0,
                        totalWage = 190_000.0,
                        taskDescription = "Bantu potong kawat bendrat & besi",
                        foremanName = "Mandor Budi",
                        source = "Telegram Bot"
                    )
                )
            )

            // Labor Wages for Proyek 2
            laborDao.insertLaborWages(
                listOf(
                    LaborWageEntity(
                        projectId = p2Id,
                        date = now - (1 * oneDay),
                        workerRole = "Tukang Keramik",
                        workerCount = 2,
                        durationHOK = 1.0,
                        wagePerUnit = 140_000.0,
                        totalWage = 280_000.0,
                        taskDescription = "Pemasangan granit lantai 1",
                        foremanName = "Mandor Agus",
                        source = "Telegram Bot"
                    ),
                    LaborWageEntity(
                        projectId = p2Id,
                        date = now - (1 * oneDay),
                        workerRole = "Kenek/Pembantu",
                        workerCount = 2,
                        durationHOK = 1.0,
                        wagePerUnit = 95_000.0,
                        totalWage = 190_000.0,
                        taskDescription = "Bantu aduk semen perekat granit",
                        foremanName = "Mandor Agus",
                        source = "Telegram Bot"
                    )
                )
            )

            // Progress Milestones
            progressDao.insertMilestones(
                listOf(
                    ProgressMilestoneEntity(
                        projectId = p1Id,
                        date = now - (1 * oneDay),
                        progressPercent = 68.5f,
                        milestoneTitle = "Pengecoran Balok Lt 2 Selesai",
                        workNotes = "Pengecoran tuntas 100%, siap tunggu masa pengeringan 3 hari",
                        issuesReported = "Semen habis 50 sak pas target",
                        weatherCondition = "Cerah"
                    ),
                    ProgressMilestoneEntity(
                        projectId = p2Id,
                        date = now - (1 * oneDay),
                        progressPercent = 42.0f,
                        milestoneTitle = "Pemasangan Granit Toko",
                        workNotes = "Lantai dasar 60% terpasang dengan nat presisi",
                        issuesReported = "Sedikit kendala debu jalanan",
                        weatherCondition = "Mendung"
                    )
                )
            )

            // Sample Telegram incoming logs
            telegramDao.insertMessage(
                TelegramMessageEntity(
                    telegramUpdateId = 1001,
                    senderName = "Mandor Budi (@budi_proyek)",
                    chatId = "-100234567890",
                    rawText = "Lapor Bos dari Proyek Griya Indah:\n- Hari ini semen masuk 50 sak @ 68rb dari TB Sumber Rejeki\n- Tukang cor 4 org @ 130rb\n- Kenek 3 org @ 95rb\nPengecoran balok lt 2 selesai lancar, cuaca cerah, progress naik ke 68.5%",
                    receivedAt = now - (1 * oneDay),
                    parseStatus = "SUCCESS",
                    detectedProjectId = p1Id,
                    detectedProjectName = "Perumahan Griya Indah Blok C",
                    parsedSummary = "Material: 1 item (Rp 3.400.000), Upah: 2 kelompok (Rp 805.000), Progress: 68.5%",
                    materialCountExtracted = 1,
                    laborCountExtracted = 2
                )
            )
        }
    }
}
