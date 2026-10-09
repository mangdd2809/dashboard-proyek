package com.example.data.firebase

import com.google.firebase.Timestamp

data class CloudProject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val codeSpk: String = "",
    val location: String = "",
    val clientName: String = "",
    val budgetRAB: Double = 0.0,
    val progressPercent: Double = 0.0,
    val status: String = "Aktif",
    val foremanInCharge: String = "",
    val updatedAt: Timestamp? = null
)

data class CloudRabItem(
    val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val floorLevel: String = "",
    val categoryCode: String = "",
    val categoryName: String = "",
    val itemNumber: String = "",
    val workDescription: String = "",
    val volume: Double = 0.0,
    val unit: String = "",
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val updatedAt: Timestamp? = null
)

data class CloudUserProfile(
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "Project Manager",
    val phone: String = "",
    val assignedProject: String = "Semua Proyek",
    val isActive: Boolean = true,
    val updatedAt: Timestamp? = null
)
