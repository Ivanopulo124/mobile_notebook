package com.ivanopulo.planner.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TaskMergeTest {

    @Test
    fun newerVersionWins() {
        val old = task("a", title = "old", updatedAt = 1)
        val new = task("a", title = "new", updatedAt = 2)
        assertSame(new, TaskMerge.winner(old, new))
        assertSame(new, TaskMerge.winner(new, old))
    }

    @Test
    fun deletionWinsATie() {
        val edited = task("a", title = "edited", updatedAt = 5)
        val deleted = task("a", updatedAt = 5, deleted = true)
        assertSame(deleted, TaskMerge.winner(edited, deleted))
        assertSame(deleted, TaskMerge.winner(deleted, edited))
    }

    @Test
    fun tieIsResolvedTheSameWayOnEveryDevice() {
        val a = task("a", title = "from phone", updatedAt = 5)
        val b = task("a", title = "from tablet", updatedAt = 5)
        assertEquals(TaskMerge.winner(a, b), TaskMerge.winner(b, a))
    }

    @Test
    fun mergeKeepsTasksFromBothSides() {
        val merged = TaskMerge.merge(
            local = listOf(task("a"), task("b", title = "local b", updatedAt = 3)),
            remote = listOf(task("b", title = "remote b", updatedAt = 2), task("c")),
        )
        assertEquals(listOf("a", "local b", "c"), merged.map { it.title })
    }
}
