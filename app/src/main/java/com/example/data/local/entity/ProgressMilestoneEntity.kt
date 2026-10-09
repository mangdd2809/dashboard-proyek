package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress_milestones",
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
data class ProgressMilestoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val date: Long = System.currentTimeMillis(),
    val progressPercent: Float,
    val milestoneTitle: String,
    val workNotes: String = "",
    val issuesReported: String = "",
    val weatherCondition: String = "Cerah", // Cerah, Hujan Lebat, Gerimis, Mendung
    val recordedBy: String = "Mandor",
    val isSynced: Boolean = false
)
