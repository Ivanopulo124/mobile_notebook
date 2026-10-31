package com.ivanopulo.planner.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivanopulo.planner.PlannerApp

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val syncManager = (applicationContext as PlannerApp).container.syncManager
        return when (syncManager.performSync()) {
            SyncOutcome.SUCCESS, SyncOutcome.SKIPPED -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
            SyncOutcome.FAILED -> Result.failure()
        }
    }
}
