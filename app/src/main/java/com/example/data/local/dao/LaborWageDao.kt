package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LaborWageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LaborWageDao {
    @Query("SELECT * FROM labor_wages ORDER BY date DESC")
    fun getAllLaborWagesFlow(): Flow<List<LaborWageEntity>>

    @Query("SELECT * FROM labor_wages WHERE projectId = :projectId ORDER BY date DESC")
    fun getLaborWagesByProjectFlow(projectId: Long): Flow<List<LaborWageEntity>>

    @Query("SELECT * FROM labor_wages WHERE projectId = :projectId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getLaborWagesByProjectAndDateRangeFlow(projectId: Long, startDate: Long, endDate: Long): Flow<List<LaborWageEntity>>

    @Query("SELECT * FROM labor_wages WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAllLaborWagesByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<LaborWageEntity>>

    @Query("SELECT SUM(totalWage) FROM labor_wages WHERE projectId = :projectId")
    fun getTotalLaborWageByProjectFlow(projectId: Long): Flow<Double?>

    @Query("SELECT SUM(totalWage) FROM labor_wages")
    fun getTotalLaborWageAllFlow(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaborWage(wage: LaborWageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaborWages(wages: List<LaborWageEntity>)

    @Update
    suspend fun updateLaborWage(wage: LaborWageEntity)

    @Delete
    suspend fun deleteLaborWage(wage: LaborWageEntity)
}
