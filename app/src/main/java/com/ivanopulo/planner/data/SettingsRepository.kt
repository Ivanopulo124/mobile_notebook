package com.ivanopulo.planner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ivanopulo.planner.sync.SyncProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class SyncError {
    AUTH, NETWORK, FORMAT, OTHER
}

data class SyncPrefs(
    val provider: SyncProvider?,
    val account: String?,
    val lastSyncAt: Long?,
    val lastError: SyncError?,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM
    }

    val syncPrefs: Flow<SyncPrefs> = dataStore.data.map { prefs ->
        val provider = prefs[SYNC_PROVIDER]?.let { name -> SyncProvider.entries.firstOrNull { it.name == name } }
        SyncPrefs(
            provider = provider,
            // Accounts saved by the Google Drive version have no provider and count as signed out.
            account = prefs[SYNC_ACCOUNT].takeIf { provider != null },
            lastSyncAt = prefs[SYNC_LAST_AT],
            lastError = prefs[SYNC_ERROR]?.let { name -> SyncError.entries.firstOrNull { it.name == name } },
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setSyncAccount(provider: SyncProvider, account: String) {
        dataStore.edit {
            it[SYNC_PROVIDER] = provider.name
            it[SYNC_ACCOUNT] = account
            it.remove(SYNC_LAST_AT)
            it.remove(SYNC_ERROR)
        }
    }

    suspend fun recordSyncSuccess(at: Long) {
        dataStore.edit {
            it[SYNC_LAST_AT] = at
            it.remove(SYNC_ERROR)
        }
    }

    suspend fun recordSyncError(error: SyncError) {
        dataStore.edit { it[SYNC_ERROR] = error.name }
    }

    suspend fun clearSync() {
        dataStore.edit {
            it.remove(SYNC_PROVIDER)
            it.remove(SYNC_ACCOUNT)
            it.remove(SYNC_LAST_AT)
            it.remove(SYNC_ERROR)
        }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SYNC_PROVIDER = stringPreferencesKey("sync_provider")
        val SYNC_ACCOUNT = stringPreferencesKey("sync_account")
        val SYNC_LAST_AT = longPreferencesKey("sync_last_at")
        val SYNC_ERROR = stringPreferencesKey("sync_error")
    }
}
