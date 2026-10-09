package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TelegramMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelegramMessageDao {
    @Query("SELECT * FROM telegram_messages ORDER BY receivedAt DESC")
    fun getAllMessagesFlow(): Flow<List<TelegramMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: TelegramMessageEntity): Long

    @Update
    suspend fun updateMessage(message: TelegramMessageEntity)

    @Query("UPDATE telegram_messages SET parseStatus = :status, parsedSummary = :summary, materialCountExtracted = :mats, laborCountExtracted = :labors WHERE id = :id")
    suspend fun updateParseResult(id: Long, status: String, summary: String?, mats: Int, labors: Int)
}
