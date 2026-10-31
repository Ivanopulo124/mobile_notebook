package com.ivanopulo.planner.sync

interface RemoteStore {
    /** Returns the sync file content, or null when it does not exist yet. */
    suspend fun download(): String?

    suspend fun upload(content: String)
}
