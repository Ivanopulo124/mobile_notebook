package com.ivanopulo.planner.data

import com.ivanopulo.planner.domain.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class TaskRepository(
    private val dao: TaskDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeWeek(monday: LocalDate): Flow<Map<LocalDate, List<Task>>> =
        dao.observeRange(monday.toString(), monday.plusDays(6).toString())
            .map { rows -> rows.map { it.toTask() }.groupBy { it.date!! } }

    suspend fun add(date: LocalDate?, title: String) {
        val key = date?.toString()
        dao.upsert(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                date = key,
                title = title,
                done = false,
                position = dao.nextPosition(key),
                color = null,
                updatedAt = clock(),
                deleted = false,
            )
        )
    }

    suspend fun rename(id: String, title: String) = update(id) { copy(title = title) }

    suspend fun setDone(id: String, done: Boolean) = update(id) { copy(done = done) }

    suspend fun delete(id: String) = update(id) { copy(deleted = true) }

    private suspend fun update(id: String, change: TaskEntity.() -> TaskEntity) {
        val current = dao.get(id) ?: return
        val changed = current.change()
        if (changed != current) dao.upsert(changed.copy(updatedAt = clock()))
    }

    private fun TaskEntity.toTask() = Task(
        id = id,
        date = date?.let(LocalDate::parse),
        title = title,
        done = done,
    )
}
