package com.ivanopulo.planner.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deleted = 0 AND date BETWEEN :from AND :to ORDER BY date, position")
    fun observeRange(from: String, to: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND date IS NULL ORDER BY position")
    fun observeUndated(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE deleted = 0 AND date IS :date")
    suspend fun nextPosition(date: String?): Int

    @Upsert
    suspend fun upsert(task: TaskEntity)
}
