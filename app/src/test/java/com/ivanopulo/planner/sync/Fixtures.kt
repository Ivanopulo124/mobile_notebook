package com.ivanopulo.planner.sync

import com.ivanopulo.planner.data.TaskEntity

fun task(
    id: String,
    title: String = id,
    updatedAt: Long = 1L,
    date: String? = "2026-10-26",
    done: Boolean = false,
    deleted: Boolean = false,
) = TaskEntity(
    id = id,
    date = date,
    title = title,
    done = done,
    position = 0,
    color = null,
    updatedAt = updatedAt,
    deleted = deleted,
)

class FakeRemoteStore(var content: String? = null) : RemoteStore {
    var uploads = 0
        private set

    override suspend fun download(): String? = content

    override suspend fun upload(content: String) {
        uploads++
        this.content = content
    }
}
