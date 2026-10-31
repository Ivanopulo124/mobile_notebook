package com.ivanopulo.planner.sync

import com.ivanopulo.planner.data.TaskEntity

object TaskMerge {
    /**
     * Last write wins. Ties are broken by a rule that does not depend on argument order, so every
     * device picks the same version: a deletion wins, then the content comparison decides.
     */
    fun winner(a: TaskEntity, b: TaskEntity): TaskEntity = when {
        a.updatedAt != b.updatedAt -> if (a.updatedAt > b.updatedAt) a else b
        a.deleted != b.deleted -> if (a.deleted) a else b
        else -> if (contentOrder.compare(a, b) >= 0) a else b
    }

    fun merge(local: List<TaskEntity>, remote: List<TaskEntity>): List<TaskEntity> {
        val byId = local.associateByTo(mutableMapOf()) { it.id }
        remote.forEach { incoming ->
            byId[incoming.id] = byId[incoming.id]?.let { winner(it, incoming) } ?: incoming
        }
        return byId.values.sortedBy { it.id }
    }

    private val contentOrder = compareBy<TaskEntity>(
        { it.title },
        { it.date },
        { it.done },
        { it.position },
        { it.color },
    )
}
