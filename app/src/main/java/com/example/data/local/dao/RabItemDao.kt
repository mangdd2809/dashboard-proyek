package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.RabItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RabItemDao {
    @Query("SELECT * FROM rab_items ORDER BY id ASC")
    fun getAllRabItemsFlow(): Flow<List<RabItemEntity>>

    @Query("SELECT * FROM rab_items WHERE projectId = :projectId ORDER BY id ASC")
    fun getRabItemsByProjectFlow(projectId: Long): Flow<List<RabItemEntity>>

    @Query("SELECT * FROM rab_items WHERE projectId = :projectId AND floorLevel = :floor ORDER BY id ASC")
    fun getRabItemsByProjectAndFloorFlow(projectId: Long, floor: String): Flow<List<RabItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRabItem(item: RabItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRabItems(items: List<RabItemEntity>)

    @Update
    suspend fun updateRabItem(item: RabItemEntity)

    @Delete
    suspend fun deleteRabItem(item: RabItemEntity)

    @Query("DELETE FROM rab_items WHERE id = :id")
    suspend fun deleteRabItemById(id: Long)

    @Query("DELETE FROM rab_items WHERE projectId = :projectId")
    suspend fun deleteRabItemsByProject(projectId: Long)
}
