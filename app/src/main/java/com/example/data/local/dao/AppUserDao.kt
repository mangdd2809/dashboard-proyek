package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AppUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUserDao {
    @Query("SELECT * FROM app_users ORDER BY id ASC")
    fun getAllUsersFlow(): Flow<List<AppUserEntity>>

    @Query("SELECT * FROM app_users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): AppUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AppUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<AppUserEntity>)

    @Update
    suspend fun updateUser(user: AppUserEntity)

    @Delete
    suspend fun deleteUser(user: AppUserEntity)

    @Query("DELETE FROM app_users WHERE id = :id")
    suspend fun deleteUserById(id: Long)
}
