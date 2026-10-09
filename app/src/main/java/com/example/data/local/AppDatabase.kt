package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
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
        AIAnalysisEntity::class,
        AppUserEntity::class,
        RabItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun materialDao(): MaterialUsageDao
    abstract fun laborDao(): LaborWageDao
    abstract fun progressDao(): ProgressMilestoneDao
    abstract fun telegramDao(): TelegramMessageDao
    abstract fun aiAnalysisDao(): AIAnalysisDao
    abstract fun appUserDao(): AppUserDao
    abstract fun rabItemDao(): RabItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rekap_proyek_ai.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
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
            val appUserDao = database.appUserDao()
            val rabItemDao = database.rabItemDao()

            val now = System.currentTimeMillis()
            val oneDay = 24 * 3600 * 1000L

            // 0. Proyek Utama dari Dokumen RAB: Universitas As-Syifa Subang
            val p0Id = projectDao.insertProject(
                ProjectEntity(
                    id = 1,
                    name = "Pembangunan Universitas As-Syifa Subang",
                    codeSpk = "SPK-2026-ASSYIFA-01",
                    location = "Subang, Jawa Barat",
                    clientName = "Yayasan As-Syifa Al Qoeriyyah",
                    budgetRAB = 14_930_200_000.0, // Rp 14.930.200.000 (Dibulatkan sesuai Dokumen EE)
                    startDate = now - (45 * oneDay),
                    targetEndDate = now + (240 * oneDay),
                    progressPercent = 54.2f,
                    status = "Aktif",
                    foremanInCharge = "Ir. H. Fardan & Mandor Budi",
                    updatedAt = now
                )
            )

            // 1. Proyek 2: Griya Indah
            val p1Id = projectDao.insertProject(
                ProjectEntity(
                    id = 2,
                    name = "Perumahan Griya Indah Blok C",
                    codeSpk = "SPK-2026-GI01",
                    location = "Jl. Melati No. 12, Sleman",
                    clientName = "PT Griya Sentosa Mandiri",
                    budgetRAB = 450_000_000.0,
                    startDate = now - (30 * oneDay),
                    targetEndDate = now + (60 * oneDay),
                    progressPercent = 68.5f,
                    status = "Aktif",
                    foremanInCharge = "Mandor Agus",
                    updatedAt = now
                )
            )

            // 2. Proyek 3: Ruko Sudirman
            val p2Id = projectDao.insertProject(
                ProjectEntity(
                    id = 3,
                    name = "Renovasi Ruko Sudirman 3 Lt",
                    codeSpk = "SPK-2026-RS03",
                    location = "Kawasan Bisnis Sudirman",
                    clientName = "Bpk. Hendra Wijaya",
                    budgetRAB = 280_000_000.0,
                    startDate = now - (15 * oneDay),
                    targetEndDate = now + (45 * oneDay),
                    progressPercent = 42.0f,
                    status = "Aktif",
                    foremanInCharge = "Mandor Danu",
                    updatedAt = now
                )
            )

            // Seed Users (CRUD User Data)
            appUserDao.insertUsers(
                listOf(
                    AppUserEntity(
                        username = "admin",
                        fullName = "Ir. H. Fardan (Project Director)",
                        role = "Admin",
                        email = "fardan@assyifa-project.id",
                        phone = "081234567890",
                        assignedProject = "Semua Proyek"
                    ),
                    AppUserEntity(
                        username = "pm_syifa",
                        fullName = "Ahmad Syakir, S.T. (Project Manager)",
                        role = "Project Manager",
                        email = "syakir@assyifa-project.id",
                        phone = "081398765432",
                        assignedProject = "Pembangunan Universitas As-Syifa Subang"
                    ),
                    AppUserEntity(
                        username = "qs_estimator",
                        fullName = "Fajar Pratama, S.T. (Estimator / QS)",
                        role = "Estimator/QS",
                        email = "fajar.qs@assyifa-project.id",
                        phone = "085712345678",
                        assignedProject = "Semua Proyek"
                    ),
                    AppUserEntity(
                        username = "mandor_budi",
                        fullName = "Budi Santoso (Mandor Lapangan)",
                        role = "Mandor/Pengawas",
                        email = "budi.mandor@gmail.com",
                        phone = "082199887766",
                        assignedProject = "Pembangunan Universitas As-Syifa Subang"
                    ),
                    AppUserEntity(
                        username = "logistik_hendra",
                        fullName = "Hendra Setiawan (Logistik Proyek)",
                        role = "Logistik",
                        email = "logistik@assyifa-project.id",
                        phone = "081987654321",
                        assignedProject = "Pembangunan Universitas As-Syifa Subang"
                    )
                )
            )

            // Seed Master RAB Items dari Dokumen Rekap EE Universitas As-Syifa
            rabItemDao.insertRabItems(
                listOf(
                    // --- LANTAI 1 (Sub Total Rp 5.812.207.567,99) ---
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "A",
                        categoryName = "PEKERJAAN PERSIAPAN",
                        workDescription = "Pembersihan Lapangan, Pengukuran & Bowplank",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 42_704_001.27,
                        totalPrice = 42_704_001.27
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "B",
                        categoryName = "PEKERJAAN TANAH DAN PONDASI",
                        workDescription = "Galian Tanah Pilecap, Pasir Alas & Lantai Kerja",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 28_168_486.64,
                        totalPrice = 28_168_486.64
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "C",
                        categoryName = "PEKERJAAN BETON BERTULANG",
                        workDescription = "Tiang Pancang 30x30, Pilecap PC1, Pedestal, Sloof & Plat Lantai",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 2_983_554_152.05,
                        totalPrice = 2_983_554_152.05
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "D",
                        categoryName = "PEKERJAAN TANGGA BETON BERTULANG",
                        workDescription = "Tangga Lt 1 ke Lt 2 & Tangga Darurat",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 121_969_731.98,
                        totalPrice = 121_969_731.98
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "E",
                        categoryName = "PEKERJAAN PASANGAN DINDING DAN PLESTERAN",
                        workDescription = "Bata Merah 1PC:5PS, Plesteran & Acian",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 802_822_737.29,
                        totalPrice = 802_822_737.29
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "F",
                        categoryName = "PEKERJAAN LANTAI DAN KERAMIK",
                        workDescription = "Granite Tile 80x80, Keramik Kasar & Dinding KM/WC",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 689_155_008.59,
                        totalPrice = 689_155_008.59
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "G",
                        categoryName = "PEKERJAAN PINTU, JENDELA",
                        workDescription = "Kusen Alumunium 4\", Daun Pintu, Jendela & Kaca Clear 6mm",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 172_394_341.21,
                        totalPrice = 172_394_341.21
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "H",
                        categoryName = "PEKERJAAN RANGKA & PLAFOND",
                        workDescription = "Rangka Hollow 4x4, Gypsumboard 9mm, List Gypsum",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 279_428_647.92,
                        totalPrice = 279_428_647.92
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "I",
                        categoryName = "PEKERJAAN PENGECATAN",
                        workDescription = "Cat Dinding Eksterior, Interior & Plafond",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 184_332_811.77,
                        totalPrice = 184_332_811.77
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "J",
                        categoryName = "PEKERJAAN INSTALASI LISTRIK",
                        workDescription = "Kabel NYM, Titik Lampu, Downlight 14W, Saklar & Stop Kontak",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 83_579_410.03,
                        totalPrice = 83_579_410.03
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "K",
                        categoryName = "PEKERJAAN INSTALASI AC",
                        workDescription = "AC Daikin 2 PK (22 Unit), Drain & Pipa Refrigrant",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 265_366_445.00,
                        totalPrice = 265_366_445.00
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "L",
                        categoryName = "PEKERJAAN INSTALASI PLUMBING",
                        workDescription = "Kloset Duduk, Pipa Air Tinja 4\", Sumur Bor, Septictank",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 102_107_143.29,
                        totalPrice = 102_107_143.29
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 1",
                        categoryCode = "M",
                        categoryName = "PEKERJAAN LAIN-LAIN",
                        workDescription = "Dinding Krawangan, WPC Motif Kayu, Tulisan Akrilik As-Syifa",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 56_624_650.95,
                        totalPrice = 56_624_650.95
                    ),

                    // --- LANTAI 2 (Sub Total Rp 3.951.819.039,35) ---
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "A",
                        categoryName = "PEKERJAAN BETON BERTULANG",
                        workDescription = "Kolom K1/K2/K3, Balok B1/B2/B3/B4, Plat Dak 15cm",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 1_563_915_394.77,
                        totalPrice = 1_563_915_394.77
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "B",
                        categoryName = "PEKERJAAN TANGGA BETON BERTULANG",
                        workDescription = "Tangga Lt 2 ke Lt 3 & Tangga Darurat",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 121_969_731.98,
                        totalPrice = 121_969_731.98
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "C",
                        categoryName = "PEKERJAAN PASANGAN DINDING DAN PLESTERAN",
                        workDescription = "Pasangan Bata Merah, Plesteran & Acian Lt 2",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 667_330_208.20,
                        totalPrice = 667_330_208.20
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "D",
                        categoryName = "PEKERJAAN LANTAI",
                        workDescription = "Pasir Bawah Lantai, Granite Tile 80x80 & Keramik KM/WC",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 516_122_916.50,
                        totalPrice = 516_122_916.50
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "E",
                        categoryName = "PEKERJAAN PINTU, JENDELA",
                        workDescription = "Pintu Alumunium Single P1/P2/P3, Jendela J1/J2/J3",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 204_503_460.74,
                        totalPrice = 204_503_460.74
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "F",
                        categoryName = "PEKERJAAN RANGKA & PLAFOND",
                        workDescription = "Rangka Hollow 4x4 & Gypsumboard 9mm Lt 2",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 279_641_949.46,
                        totalPrice = 279_641_949.46
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "G",
                        categoryName = "PEKERJAAN PENGECATAN",
                        workDescription = "Cat Dinding Eksterior, Interior & Plafond Lt 2",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 163_038_051.14,
                        totalPrice = 163_038_051.14
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "H",
                        categoryName = "PEKERJAAN INSTALASI LISTRIK",
                        workDescription = "Titik Lampu, Downlight, Saklar & Stop Kontak AC Lt 2",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 66_253_430.70,
                        totalPrice = 66_253_430.70
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "I",
                        categoryName = "PEKERJAAN INSTALASI AC",
                        workDescription = "AC Daikin 2 PK (22 Unit), Drain & Pipa Refrigrant Lt 2",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 265_366_445.00,
                        totalPrice = 265_366_445.00
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "J",
                        categoryName = "PEKERJAAN INSTALASI PLUMBING",
                        workDescription = "Plumbing Air Bersih/Kotor, Kloset & Wastafel Lt 2",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 53_317_223.44,
                        totalPrice = 53_317_223.44
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 2",
                        categoryCode = "K",
                        categoryName = "PEKERJAAN LAIN-LAIN",
                        workDescription = "Dinding Krawangan Lt 2 & Wall Cladding WPC",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 50_360_227.43,
                        totalPrice = 50_360_227.43
                    ),

                    // --- LANTAI 3 (Sub Total Rp 5.166.173.880,67) ---
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "A",
                        categoryName = "PEKERJAAN BETON & BAJA",
                        workDescription = "Kolom Baja IWF 300, Rafter IWF, Baseplate, Gording Atap CNP",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 2_589_258_755.82,
                        totalPrice = 2_589_258_755.82
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "B",
                        categoryName = "PEKERJAAN PASANGAN DINDING DAN PLESTERAN",
                        workDescription = "Bata Merah, Plesteran & Acian Lt 3",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 847_997_476.25,
                        totalPrice = 847_997_476.25
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "C",
                        categoryName = "PEKERJAAN LANTAI",
                        workDescription = "Granite Tile, Keramik KM/WC & Lantai Beton Ekspose Coating",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 330_744_141.22,
                        totalPrice = 330_744_141.22
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "D",
                        categoryName = "PEKERJAAN PINTU, JENDELA",
                        workDescription = "Pintu Alumunium Single P1/P2 & Pintu PVC KM/WC (19 unit)",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 49_131_308.95,
                        totalPrice = 49_131_308.95
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "E",
                        categoryName = "PEKERJAAN ATAP",
                        workDescription = "Penutup Atap Spandek (1191 m2) & Listplank GRC",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 174_119_368.50,
                        totalPrice = 174_119_368.50
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "F",
                        categoryName = "PEKERJAAN RANGKA & PLAFOND",
                        workDescription = "Rangka Hollow 4x4 & Gypsumboard 9mm Lt 3",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 308_117_862.22,
                        totalPrice = 308_117_862.22
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "G",
                        categoryName = "PEKERJAAN PENGECATAN",
                        workDescription = "Cat Dinding Eksterior, Interior & Plafond Lt 3",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 204_140_018.63,
                        totalPrice = 204_140_018.63
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "H",
                        categoryName = "PEKERJAAN INSTALASI LISTRIK",
                        workDescription = "Titik Lampu (227 Titik), Downlight 14W, Saklar & Stop Kontak",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 90_792_659.68,
                        totalPrice = 90_792_659.68
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "I",
                        categoryName = "PEKERJAAN INSTALASI AC",
                        workDescription = "AC Cassette Indoor 5PK (14 Unit), Drain & Pipa Refrigrant",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 391_806_705.36,
                        totalPrice = 391_806_705.36
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "J",
                        categoryName = "PEKERJAAN INSTALASI PLUMBING",
                        workDescription = "Kloset (19 Unit), Shower (19 Unit), Wastafel, Tandon Air",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 129_705_356.63,
                        totalPrice = 129_705_356.63
                    ),
                    RabItemEntity(
                        projectId = p0Id,
                        floorLevel = "LANTAI 3",
                        categoryCode = "K",
                        categoryName = "PEKERJAAN LAIN - LAIN",
                        workDescription = "Dinding Krawangan Lt 3 & Wall Cladding WPC",
                        volume = 1.0,
                        unit = "ls",
                        unitPrice = 50_360_227.43,
                        totalPrice = 50_360_227.43
                    )
                )
            )

            // Materials for Proyek Universitas As-Syifa
            materialDao.insertMaterials(
                listOf(
                    MaterialUsageEntity(
                        projectId = p0Id,
                        date = now - (1 * oneDay),
                        materialName = "Tiang Pancang Beton 30x30 Tinggi 12m",
                        category = "Besi & Baja",
                        quantity = 40.0,
                        unit = "m1",
                        unitPrice = 465_850.0,
                        totalCost = 18_634_000.0,
                        supplier = "PT Wika Beton Fabrikasi",
                        invoiceOrNote = "Pondasi Gedung Rektorat As-Syifa",
                        source = "Telegram Bot"
                    ),
                    MaterialUsageEntity(
                        projectId = p0Id,
                        date = now - (2 * oneDay),
                        materialName = "Ready Mix Beton K-300 Slump 12",
                        category = "Semen & Pasir",
                        quantity = 35.0,
                        unit = "m3",
                        unitPrice = 1_796_904.0,
                        totalCost = 62_891_640.0,
                        supplier = "Pioneer Beton Subang",
                        invoiceOrNote = "Pengecoran Pilecap PC1 & Kolom Pedestal",
                        source = "Telegram Bot"
                    ),
                    MaterialUsageEntity(
                        projectId = p0Id,
                        date = now - (3 * oneDay),
                        materialName = "Besi Ulir D16-150 KS Asli",
                        category = "Besi & Baja",
                        quantity = 1500.0,
                        unit = "kg",
                        unitPrice = 19_428.0,
                        totalCost = 29_142_000.0,
                        supplier = "Krakatau Steel Distributor",
                        invoiceOrNote = "Pembesian Sloof SL1 & Kolom K1",
                        source = "Manual Mandor"
                    ),
                    MaterialUsageEntity(
                        projectId = p0Id,
                        date = now - (4 * oneDay),
                        materialName = "Bata Merah Standar Subang",
                        category = "Bata & Dinding",
                        quantity = 5000.0,
                        unit = "buah",
                        unitPrice = 900.0,
                        totalCost = 4_500_000.0,
                        supplier = "Lio Bata Merah Pagaden Subang",
                        invoiceOrNote = "Pasangan dinding lantai 1",
                        source = "Telegram Bot"
                    )
                )
            )

            // Labor Wages for Proyek Universitas As-Syifa
            laborDao.insertLaborWages(
                listOf(
                    LaborWageEntity(
                        projectId = p0Id,
                        date = now - (1 * oneDay),
                        workerRole = "Tukang Cor & Pembesian",
                        workerCount = 12,
                        durationHOK = 1.0,
                        wagePerUnit = 145_000.0,
                        totalWage = 1_740_000.0,
                        taskDescription = "Pengecoran tiang pancang & pilecap PC1",
                        foremanName = "Mandor Budi",
                        source = "Telegram Bot"
                    ),
                    LaborWageEntity(
                        projectId = p0Id,
                        date = now - (1 * oneDay),
                        workerRole = "Kenek/Pembantu Lapangan",
                        workerCount = 8,
                        durationHOK = 1.0,
                        wagePerUnit = 100_000.0,
                        totalWage = 800_000.0,
                        taskDescription = "Persiapan adukan & pemadatan vibrator cor",
                        foremanName = "Mandor Budi",
                        source = "Telegram Bot"
                    ),
                    LaborWageEntity(
                        projectId = p0Id,
                        date = now - (2 * oneDay),
                        workerRole = "Tukang Pasang Bata & Plesteran",
                        workerCount = 6,
                        durationHOK = 1.0,
                        wagePerUnit = 140_000.0,
                        totalWage = 840_000.0,
                        taskDescription = "Pasangan bata merah ruang kelas lantai 1",
                        foremanName = "Mandor Budi",
                        source = "Telegram Bot"
                    )
                )
            )

            // Progress Milestones for Universitas As-Syifa
            progressDao.insertMilestones(
                listOf(
                    ProgressMilestoneEntity(
                        projectId = p0Id,
                        date = now - (1 * oneDay),
                        progressPercent = 54.2f,
                        milestoneTitle = "Struktur Lt 1 & Pondasi Pilecap 100%",
                        workNotes = "Pondasi tiang pancang dan sloof selesai, berlanjut ke rangka kolom Lt 2",
                        issuesReported = "Pengiriman baja IWF 300 dari pabrik tepat waktu",
                        weatherCondition = "Cerah Berawan"
                    )
                )
            )

            // Sample Telegram incoming log for Universitas As-Syifa
            telegramDao.insertMessage(
                TelegramMessageEntity(
                    telegramUpdateId = 1002,
                    senderName = "Mandor Budi (@budi_assyifa)",
                    chatId = "-100889977665",
                    rawText = "Lapor Pak Direktur dari Proyek Universitas As-Syifa Subang:\n- Hari ini tiang pancang masuk 40 m1 @ 465.850\n- Ready mix cor K-300 masuk 35 m3 @ 1.796.904\n- Tukang cor 12 org @ 145rb\n- Kenek 8 org @ 100rb\nProgress pondasi pilecap tuntas 100%, total progress proyek naik ke 54.2%",
                    receivedAt = now - (1 * oneDay),
                    parseStatus = "SUCCESS",
                    detectedProjectId = p0Id,
                    detectedProjectName = "Pembangunan Universitas As-Syifa Subang",
                    parsedSummary = "Material: 2 item (Rp 81.525.640), Upah: 20 orang (Rp 2.540.000), Progress: 54.2%",
                    materialCountExtracted = 2,
                    laborCountExtracted = 2
                )
            )
        }
    }
}
