package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "labor_wages",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["date"])]
)
data class LaborWageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val date: Long = System.currentTimeMillis(),
    val workerRole: String, // Tukang Batu, Tukang Besi, Kenek/Pembantu, Mandor, Tukang Keramik, Tukang Kayu, Lembur Tukang
    val workerCount: Int = 1, // Jumlah orang
    val durationHOK: Double = 1.0, // HOK (Hari Orang Kerja) atau Jam jika lembur
    val wagePerUnit: Double, // Tarif per orang per hari / jam
    val totalWage: Double = workerCount * durationHOK * wagePerUnit,
    val taskDescription: String = "", // e.g. "Pengecoran pelat lantai 2", "Pemasangan dinding bata merah"
    val foremanName: String = "",
    val source: String = "Manual", // Manual, Telegram Bot, AI Parse
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
