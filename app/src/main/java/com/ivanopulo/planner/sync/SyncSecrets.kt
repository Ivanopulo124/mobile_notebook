package com.ivanopulo.planner.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

data class PendingAuth(val verifier: String, val state: String)

/** Tokens live in their own file so that backup rules can leave it out. */
class SyncSecrets(private val dataStore: DataStore<Preferences>) {
    suspend fun dropboxRefreshToken(): String? = dataStore.data.first()[DROPBOX_REFRESH_TOKEN]

    suspend fun setDropboxRefreshToken(token: String) {
        dataStore.edit { it[DROPBOX_REFRESH_TOKEN] = token }
    }

    suspend fun yandexToken(): String? = dataStore.data.first()[YANDEX_TOKEN]

    suspend fun setYandexToken(token: String) {
        dataStore.edit { it[YANDEX_TOKEN] = token }
    }

    // Kept on disk because the process may die while the browser shows the consent page.
    suspend fun savePendingDropbox(pending: PendingAuth) {
        dataStore.edit {
            it[DROPBOX_VERIFIER] = pending.verifier
            it[DROPBOX_STATE] = pending.state
        }
    }

    suspend fun takePendingDropbox(): PendingAuth? {
        var pending: PendingAuth? = null
        dataStore.edit {
            val verifier = it[DROPBOX_VERIFIER]
            val state = it[DROPBOX_STATE]
            if (verifier != null && state != null) pending = PendingAuth(verifier, state)
            it.remove(DROPBOX_VERIFIER)
            it.remove(DROPBOX_STATE)
        }
        return pending
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val DROPBOX_REFRESH_TOKEN = stringPreferencesKey("dropbox_refresh_token")
        val DROPBOX_VERIFIER = stringPreferencesKey("dropbox_pkce_verifier")
        val DROPBOX_STATE = stringPreferencesKey("dropbox_pkce_state")
        val YANDEX_TOKEN = stringPreferencesKey("yandex_token")
    }
}
