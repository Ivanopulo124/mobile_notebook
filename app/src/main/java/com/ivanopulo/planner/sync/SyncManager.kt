package com.ivanopulo.planner.sync

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
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
    val provider: SyncProvider?,
    val account: String?,
    val lastSyncAt: Long?,
    val lastError: SyncError?,
    val running: Boolean,
)

enum class SyncOutcome { SUCCESS, SKIPPED, RETRY, FAILED }

class SyncManager(
    private val dropbox: DropboxAuth,
    private val yandex: YandexAuth,
    private val secrets: SyncSecrets,
    private val store: SyncStore,
    private val settings: SettingsRepository,
    private val workManager: WorkManager,
    private val appScope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    val availableProviders: List<SyncProvider> = SyncProvider.entries.filter { auth(it).isConfigured }

    val status: Flow<SyncStatus> = combine(
        settings.syncPrefs,
        workManager.getWorkInfosForUniqueWorkFlow(WORK_ONE_TIME),
    ) { prefs, works ->
        SyncStatus(
            provider = prefs.provider,
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

    /** The consent page to open in the browser; it redirects back to [finishDropboxSignIn]. */
    suspend fun dropboxAuthorizationUrl(): String = dropbox.authorizationUrl()

    suspend fun finishDropboxSignIn(code: String?, state: String?) {
        dropbox.finishSignIn(code, state)?.let { completeSignIn(SyncProvider.DROPBOX, it) }
    }

    suspend fun finishYandexSignIn(token: String) {
        completeSignIn(SyncProvider.YANDEX, yandex.finishSignIn(token))
    }

    suspend fun signOut() {
        val provider = settings.syncPrefs.first().provider
        workManager.cancelUniqueWork(WORK_ONE_TIME)
        workManager.cancelUniqueWork(WORK_PERIODIC)
        settings.clearSync()
        if (provider != null) {
            try {
                auth(provider).signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Revoking cloud access failed", e)
            }
        }
        secrets.clear()
    }

    suspend fun performSync(): SyncOutcome = mutex.withLock {
        val provider = settings.syncPrefs.first().provider ?: return SyncOutcome.SKIPPED
        try {
            syncWith(auth(provider))
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

    private fun auth(provider: SyncProvider): CloudAuth = when (provider) {
        SyncProvider.DROPBOX -> dropbox
        SyncProvider.YANDEX -> yandex
    }

    private suspend fun completeSignIn(provider: SyncProvider, account: String) {
        settings.setSyncAccount(provider, account)
        requestSync()
        schedulePeriodicSync()
    }

    private suspend fun syncWith(auth: CloudAuth) {
        try {
            SyncEngine(store, auth.remoteStore(auth.accessToken())).sync()
        } catch (e: RemoteAuthException) {
            // The cached token was revoked or expired early: drop it and try once with a new one.
            if (!auth.invalidateAccessToken()) throw e
            SyncEngine(store, auth.remoteStore(auth.accessToken())).sync()
        }
    }

    private fun classify(e: Exception): Pair<SyncError, SyncOutcome> = when (e) {
        is NotAuthorizedException, is RemoteAuthException -> SyncError.AUTH to SyncOutcome.FAILED
        is UnsupportedSyncFormatException, is SerializationException -> SyncError.FORMAT to SyncOutcome.FAILED
        is RemoteHttpException ->
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
