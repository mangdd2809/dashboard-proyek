package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rab_items")
data class RabItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val floorLevel: String, // "LANTAI 1", "LANTAI 2", "LANTAI 3"
    val categoryCode: String, // "A", "B", "C", dst
    val categoryName: String, // "PEKERJAAN BETON BERTULANG", dll
    val itemNumber: String = "",
    val workDescription: String,
    val volume: Double = 0.0,
    val unit: String = "",
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)
