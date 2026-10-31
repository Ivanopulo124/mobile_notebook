package com.ivanopulo.planner

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.ivanopulo.planner.data.PlannerDatabase
import com.ivanopulo.planner.data.SettingsRepository
import com.ivanopulo.planner.data.TaskRepository
import com.ivanopulo.planner.sync.GoogleAuth
import com.ivanopulo.planner.sync.SyncManager
import com.ivanopulo.planner.sync.SyncStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration.Companion.seconds

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class PlannerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.syncManager.schedulePeriodicSync()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = container.syncManager.requestSync()
        })
    }
}

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database =
        Room.databaseBuilder(context, PlannerDatabase::class.java, "planner.db").build()

    val settingsRepository = SettingsRepository(context.settingsDataStore)

    val syncManager = SyncManager(
        auth = GoogleAuth(context),
        store = SyncStore(database.taskDao()) { block -> database.withTransaction { block() } },
        settings = settingsRepository,
        workManager = WorkManager.getInstance(context),
        appScope = appScope,
    )

    val taskRepository = TaskRepository(
        dao = database.taskDao(),
        onLocalChange = { syncManager.requestSync(delay = 5.seconds) },
    )
}
