package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MaterialUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialUsageDao {
    @Query("SELECT * FROM material_usages ORDER BY date DESC")
    fun getAllMaterialsFlow(): Flow<List<MaterialUsageEntity>>

    @Query("SELECT * FROM material_usages WHERE projectId = :projectId ORDER BY date DESC")
    fun getMaterialsByProjectFlow(projectId: Long): Flow<List<MaterialUsageEntity>>

    @Query("SELECT * FROM material_usages WHERE projectId = :projectId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getMaterialsByProjectAndDateRangeFlow(projectId: Long, startDate: Long, endDate: Long): Flow<List<MaterialUsageEntity>>

    @Query("SELECT * FROM material_usages WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAllMaterialsByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<MaterialUsageEntity>>

    @Query("SELECT SUM(totalCost) FROM material_usages WHERE projectId = :projectId")
    fun getTotalMaterialCostByProjectFlow(projectId: Long): Flow<Double?>

    @Query("SELECT SUM(totalCost) FROM material_usages")
    fun getTotalMaterialCostAllFlow(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<MaterialUsageEntity>)

    @Update
    suspend fun updateMaterial(material: MaterialUsageEntity)

    @Delete
    suspend fun deleteMaterial(material: MaterialUsageEntity)
}
