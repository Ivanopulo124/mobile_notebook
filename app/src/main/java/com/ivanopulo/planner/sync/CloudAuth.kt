package com.ivanopulo.planner.sync

enum class SyncProvider { DROPBOX, YANDEX }

interface CloudAuth {
    val isConfigured: Boolean

    suspend fun accessToken(): String

    /** Drops a cached token the cloud rejected; false when no other token can be obtained. */
    fun invalidateAccessToken(): Boolean

    fun remoteStore(accessToken: String): RemoteStore

    /** Revokes access on the provider's side where it can; local secrets are cleared by the caller. */
    suspend fun signOut()
}
