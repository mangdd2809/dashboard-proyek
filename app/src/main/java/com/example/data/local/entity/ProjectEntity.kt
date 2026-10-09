package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val codeSpk: String = "",
    val location: String = "",
    val clientName: String = "",
    val budgetRAB: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val targetEndDate: Long = System.currentTimeMillis() + (90L * 24 * 3600 * 1000), // Default 90 days
    val progressPercent: Float = 0f,
    val status: String = "Aktif", // Aktif, Selesai, Ditunda
    val foremanInCharge: String = "",
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
