package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class AppUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val role: String, // "Admin", "Project Manager", "Estimator/QS", "Mandor/Pengawas", "Logistik"
    val email: String = "",
    val phone: String = "",
    val assignedProject: String = "Semua Proyek",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
