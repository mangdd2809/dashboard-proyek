package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_messages")
data class TelegramMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val telegramUpdateId: Long = 0,
    val senderName: String = "Mandor Lapangan",
    val chatId: String = "",
    val rawText: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val parseStatus: String = "PENDING", // PENDING, SUCCESS, PARTIAL, FAILED
    val detectedProjectId: Long? = null,
    val detectedProjectName: String? = null,
    val parsedSummary: String? = null,
    val materialCountExtracted: Int = 0,
    val laborCountExtracted: Int = 0
)
