package com.ivanopulo.planner.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
