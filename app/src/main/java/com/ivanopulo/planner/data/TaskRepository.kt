package com.ivanopulo.planner.data

import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.domain.TaskColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class TaskRepository(
    private val dao: TaskDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val onLocalChange: () -> Unit = {},
) {
    fun observeWeek(monday: LocalDate): Flow<Map<LocalDate, List<Task>>> =
        dao.observeRange(monday.toString(), monday.plusDays(6).toString())
            .map { rows -> rows.map { it.toTask() }.groupBy { it.date!! } }

    fun observeSomeday(): Flow<List<Task>> =
        dao.observeUndated().map { rows -> rows.map { it.toTask() } }

    suspend fun add(date: LocalDate?, title: String) {
        val key = date?.toString()
        dao.upsert(
            TaskEntity(
                id = newId(),
                date = key,
                title = title,
                done = false,
                position = dao.nextPosition(key),
                color = null,
                updatedAt = clock(),
                deleted = false,
            )
        )
        onLocalChange()
    }

    suspend fun rename(id: String, title: String) = update(id) { copy(title = title) }

    suspend fun setDone(id: String, done: Boolean) = update(id) { copy(done = done) }

    suspend fun setColor(id: String, color: TaskColor?) = update(id) { copy(color = color?.id) }

    suspend fun move(id: String, date: LocalDate?) {
        val key = date?.toString()
        val current = dao.get(id) ?: return
        if (current.date == key) return
        dao.upsert(current.copy(date = key, position = dao.nextPosition(key), updatedAt = clock()))
        onLocalChange()
    }

    suspend fun delete(id: String) = update(id) { copy(deleted = true) }

    /** Deletes the done tasks of [date] (null is the Other list) and returns their ids for undo. */
    suspend fun deleteDone(date: LocalDate?): List<String> {
        val ids = dao.doneIds(date?.toString())
        ids.forEach { delete(it) }
        return ids
    }

    suspend fun restore(id: String) = update(id) { copy(deleted = false) }

    private suspend fun update(id: String, change: TaskEntity.() -> TaskEntity) {
        val current = dao.get(id) ?: return
        val changed = current.change()
        if (changed == current) return
        dao.upsert(changed.copy(updatedAt = clock()))
        onLocalChange()
    }

    private fun TaskEntity.toTask() = Task(
        id = id,
        date = date?.let(LocalDate::parse),
        title = title,
        done = done,
        color = TaskColor.fromId(color),
    )
}
