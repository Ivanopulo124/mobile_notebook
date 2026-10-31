package com.ivanopulo.planner.sync

import com.ivanopulo.planner.data.TaskDao
import com.ivanopulo.planner.data.TaskEntity

class SyncStore(
    private val dao: TaskDao,
    private val inTransaction: suspend (suspend () -> Unit) -> Unit,
) {
    suspend fun allTasks(): List<TaskEntity> = dao.getAll()

    /**
     * Writes each row unless the local copy beats it. Rows were merged from a snapshot, so a task
     * edited locally since then must not be overwritten.
     */
    suspend fun applyIfNewer(rows: List<TaskEntity>) = inTransaction {
        rows.forEach { row ->
            val current = dao.get(row.id)
            if (current == null || (current != row && TaskMerge.winner(current, row) == row)) {
                dao.upsert(row)
            }
        }
    }
}
