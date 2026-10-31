package com.ivanopulo.planner.sync

import com.ivanopulo.planner.data.TaskEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class UnsupportedSyncFormatException(version: Int) :
    IllegalStateException("Sync file version $version is newer than this app supports")

object SyncCodec {
    const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(tasks: List<TaskEntity>): String =
        json.encodeToString(SyncFile.serializer(), SyncFile(VERSION, tasks.map { it.toSyncTask() }))

    fun decode(content: String): List<TaskEntity> {
        val file = json.decodeFromString(SyncFile.serializer(), content)
        // Writing back a newer format with this app's fields would silently drop data.
        if (file.version > VERSION) throw UnsupportedSyncFormatException(file.version)
        return file.tasks.map { it.toEntity() }
    }

    @Serializable
    private data class SyncFile(val version: Int, val tasks: List<SyncTask>)

    @Serializable
    private data class SyncTask(
        val id: String,
        val date: String? = null,
        val title: String,
        val done: Boolean = false,
        val position: Int = 0,
        val color: Int? = null,
        val updatedAt: Long,
        val deleted: Boolean = false,
    )

    private fun TaskEntity.toSyncTask() =
        SyncTask(id, date, title, done, position, color, updatedAt, deleted)

    private fun SyncTask.toEntity() =
        TaskEntity(id, date, title, done, position, color, updatedAt, deleted)
}
