package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ProgressMilestoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressMilestoneDao {
    @Query("SELECT * FROM progress_milestones WHERE projectId = :projectId ORDER BY date DESC")
    fun getMilestonesByProjectFlow(projectId: Long): Flow<List<ProgressMilestoneEntity>>

    @Query("SELECT * FROM progress_milestones ORDER BY date DESC LIMIT 20")
    fun getAllMilestonesFlow(): Flow<List<ProgressMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: ProgressMilestoneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<ProgressMilestoneEntity>)
}
