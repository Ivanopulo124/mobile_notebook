package com.ivanopulo.planner

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ivanopulo.planner.data.PlannerDatabase
import com.ivanopulo.planner.data.SettingsRepository
import com.ivanopulo.planner.data.TaskRepository

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class PlannerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val database =
        Room.databaseBuilder(context, PlannerDatabase::class.java, "planner.db").build()

    val taskRepository = TaskRepository(database.taskDao())
    val settingsRepository = SettingsRepository(context.settingsDataStore)
}
