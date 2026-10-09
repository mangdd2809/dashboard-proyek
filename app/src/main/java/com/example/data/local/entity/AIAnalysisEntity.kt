package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_analyses")
data class AIAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val projectName: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val period: String = "Mingguan", // Harian, Mingguan, Bulanan, Keseluruhan
    val analysisType: String = "EVALUASI_BIAYA", // EVALUASI_BIAYA, PEMBOROSAN_MATERIAL, PRODUKTIVITAS_UPAH, PREDIKSI_RAB
    val title: String,
    val contentMarkdown: String,
    val costEfficiencyScore: Int = 85, // 0 - 100
    val riskLevel: String = "Rendah" // Rendah, Sedang, Tinggi
)
