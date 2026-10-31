package com.ivanopulo.planner.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTaskDao : TaskDao {
    private val rows = MutableStateFlow<Map<String, TaskEntity>>(emptyMap())

    val all: Collection<TaskEntity> get() = rows.value.values

    override fun observeRange(from: String, to: String): Flow<List<TaskEntity>> = rows.map { map ->
        map.values
            .filter { !it.deleted && it.date != null && it.date in from..to }
            .sortedWith(compareBy({ it.date }, { it.position }))
    }

    override fun observeUndated(): Flow<List<TaskEntity>> = rows.map { map ->
        map.values.filter { !it.deleted && it.date == null }.sortedBy { it.position }
    }

    override suspend fun getAll(): List<TaskEntity> = rows.value.values.toList()

    override suspend fun get(id: String): TaskEntity? = rows.value[id]

    override suspend fun nextPosition(date: String?): Int =
        (rows.value.values.filter { !it.deleted && it.date == date }.maxOfOrNull { it.position } ?: -1) + 1

    override suspend fun upsert(task: TaskEntity) {
        rows.value = rows.value + (task.id to task)
    }
}
