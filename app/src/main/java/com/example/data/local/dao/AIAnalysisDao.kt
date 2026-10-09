package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AIAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIAnalysisDao {
    @Query("SELECT * FROM ai_analyses WHERE projectId = :projectId ORDER BY generatedAt DESC")
    fun getAnalysesByProjectFlow(projectId: Long): Flow<List<AIAnalysisEntity>>

    @Query("SELECT * FROM ai_analyses ORDER BY generatedAt DESC")
    fun getAllAnalysesFlow(): Flow<List<AIAnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AIAnalysisEntity): Long
}
