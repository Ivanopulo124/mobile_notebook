package com.ivanopulo.planner.sync

class SyncEngine(private val store: SyncStore, private val remote: RemoteStore) {

    suspend fun sync() {
        val remoteTasks = remote.download()?.let(SyncCodec::decode).orEmpty().sortedBy { it.id }
        val local = store.allTasks()
        val merged = TaskMerge.merge(local, remoteTasks)

        val localById = local.associateBy { it.id }
        store.applyIfNewer(merged.filter { it != localById[it.id] })

        if (merged != remoteTasks) remote.upload(SyncCodec.encode(merged))
    }
}
