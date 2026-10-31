package com.ivanopulo.planner.sync

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.common.api.ApiException
import com.ivanopulo.planner.data.SettingsRepository
import com.ivanopulo.planner.data.SyncError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

data class SyncStatus(
    val account: String?,
    val lastSyncAt: Long?,
    val lastError: SyncError?,
    val running: Boolean,
)

enum class SyncOutcome { SUCCESS, SKIPPED, RETRY, FAILED }

class SyncManager(
    private val auth: GoogleAuth,
    private val store: SyncStore,
    private val settings: SettingsRepository,
    private val workManager: WorkManager,
    private val appScope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    val status: Flow<SyncStatus> = combine(
        settings.syncPrefs,
        workManager.getWorkInfosForUniqueWorkFlow(WORK_ONE_TIME),
    ) { prefs, works ->
        SyncStatus(
            account = prefs.account,
            lastSyncAt = prefs.lastSyncAt,
            lastError = prefs.lastError,
            running = works.any { it.state == WorkInfo.State.RUNNING },
        )
    }

    /** Schedules a sync if signed in. A newer request replaces a pending one, which debounces edits. */
    fun requestSync(delay: Duration = Duration.ZERO) {
        appScope.launch {
            if (settings.syncPrefs.first().account == null) return@launch
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .setInitialDelay(delay.toJavaDuration())
                .build()
            workManager.enqueueUniqueWork(WORK_ONE_TIME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun schedulePeriodicSync() {
        appScope.launch {
            if (settings.syncPrefs.first().account == null) return@launch
            val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL.inWholeHours, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    /** Returns the consent screen to launch, or null when access was already granted. */
    suspend fun beginSignIn(): PendingIntent? {
        val result = auth.authorize()
        if (result.hasResolution()) return result.pendingIntent
        completeSignIn(result.accessToken)
        return null
    }

    suspend fun finishSignIn(data: Intent?) {
        completeSignIn(auth.resultFromIntent(data).accessToken)
    }

    suspend fun signOut() {
        val account = settings.syncPrefs.first().account
        workManager.cancelUniqueWork(WORK_ONE_TIME)
        workManager.cancelUniqueWork(WORK_PERIODIC)
        settings.clearSync()
        if (account != null) {
            try {
                auth.revoke(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Revoking Drive access failed", e)
            }
        }
    }

    suspend fun performSync(): SyncOutcome = mutex.withLock {
        val account = settings.syncPrefs.first().account ?: return SyncOutcome.SKIPPED
        try {
            syncWithFreshToken(account)
            settings.recordSyncSuccess(clock())
            SyncOutcome.SUCCESS
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed", e)
            val (error, outcome) = classify(e)
            settings.recordSyncError(error)
            outcome
        }
    }

    private suspend fun completeSignIn(accessToken: String?) {
        val token = accessToken ?: throw NotAuthorizedException()
        settings.setSyncAccount(DriveRemoteStore(token).accountEmail())
        requestSync()
        schedulePeriodicSync()
    }

    private suspend fun syncWithFreshToken(account: String) {
        val token = auth.accessToken(account)
        try {
            SyncEngine(store, DriveRemoteStore(token)).sync()
        } catch (e: DriveAuthException) {
            // The cached token was revoked or expired early: drop it and try once with a new one.
            auth.clearToken(token)
            SyncEngine(store, DriveRemoteStore(auth.accessToken(account))).sync()
        }
    }

    private fun classify(e: Exception): Pair<SyncError, SyncOutcome> = when (e) {
        is NotAuthorizedException, is DriveAuthException, is ApiException -> SyncError.AUTH to SyncOutcome.FAILED
        is UnsupportedSyncFormatException, is SerializationException -> SyncError.FORMAT to SyncOutcome.FAILED
        is DriveHttpException ->
            if (e.code == 429 || e.code >= 500) SyncError.NETWORK to SyncOutcome.RETRY
            else SyncError.OTHER to SyncOutcome.FAILED
        is IOException -> SyncError.NETWORK to SyncOutcome.RETRY
        else -> SyncError.OTHER to SyncOutcome.FAILED
    }

    private companion object {
        const val TAG = "PlannerSync"
        const val WORK_ONE_TIME = "sync"
        const val WORK_PERIODIC = "sync-periodic"
        val PERIODIC_INTERVAL = 12.hours
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
