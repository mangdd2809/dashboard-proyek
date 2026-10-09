package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "material_usages",
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
data class MaterialUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val date: Long = System.currentTimeMillis(),
    val materialName: String,
    val category: String = "Semen & Pasir", // Semen & Pasir, Besi & Baja, Bata & Dinding, Kayu & Bekisting, Finishing & Cat, Plumbing & Elektrik, Lainnya
    val quantity: Double,
    val unit: String = "sak", // sak, m3, btg, m2, rit, kg, pail, box, lembar
    val unitPrice: Double,
    val totalCost: Double = quantity * unitPrice,
    val supplier: String = "",
    val invoiceOrNote: String = "",
    val source: String = "Manual", // Manual, Telegram Bot, AI Parse
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
