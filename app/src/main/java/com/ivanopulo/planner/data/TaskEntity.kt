package com.ivanopulo.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasks", indices = [Index("date")])
data class TaskEntity(
    @PrimaryKey val id: String,
    // ISO-8601 date, null for "someday" tasks.
    val date: String?,
    val title: String,
    val done: Boolean,
    val position: Int,
    val color: Int?,
    val updatedAt: Long,
    // Soft delete so that deletions can be propagated by sync.
    val deleted: Boolean,
)
