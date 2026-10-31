package com.ivanopulo.planner.data

import com.ivanopulo.planner.domain.TaskColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskRepositoryTest {
    private val dao = FakeTaskDao()
    private var now = 1_000L
    private var nextId = 0
    private val repository = TaskRepository(dao, clock = { now }, newId = { "t${nextId++}" })

    private val monday = LocalDate.of(2026, 10, 26)
    private val tuesday = monday.plusDays(1)

    @Test
    fun movedTaskGoesToEndOfTargetDay() = runBlocking {
        repository.add(tuesday, "existing 1")
        repository.add(tuesday, "existing 2")
        repository.add(monday, "moving")

        repository.move("t2", tuesday)

        val tuesdayTasks = repository.observeWeek(monday).first()[tuesday].orEmpty()
        assertEquals(listOf("existing 1", "existing 2", "moving"), tuesdayTasks.map { it.title })
        assertNull(repository.observeWeek(monday).first()[monday])
    }

    @Test
    fun moveToSomedayAndBack() = runBlocking {
        repository.add(monday, "task")

        repository.move("t0", null)
        assertEquals(listOf("task"), repository.observeSomeday().first().map { it.title })
        assertTrue(repository.observeWeek(monday).first().isEmpty())

        repository.move("t0", tuesday)
        assertTrue(repository.observeSomeday().first().isEmpty())
        assertEquals(tuesday, repository.observeWeek(monday).first()[tuesday]!!.single().date)
    }

    @Test
    fun deleteHidesTaskAndRestoreBringsItBack() = runBlocking {
        repository.add(monday, "task")

        repository.delete("t0")
        assertTrue(repository.observeWeek(monday).first().isEmpty())
        assertTrue(dao.all.single().deleted)

        repository.restore("t0")
        assertEquals("task", repository.observeWeek(monday).first()[monday]!!.single().title)
    }

    @Test
    fun colorIsStoredAndCleared() = runBlocking {
        repository.add(monday, "task")

        repository.setColor("t0", TaskColor.GREEN)
        assertEquals(TaskColor.GREEN, repository.observeWeek(monday).first()[monday]!!.single().color)

        repository.setColor("t0", null)
        assertNull(repository.observeWeek(monday).first()[monday]!!.single().color)
    }

    @Test
    fun unchangedEditKeepsUpdatedAt() = runBlocking {
        repository.add(monday, "task")
        now = 2_000L

        repository.rename("t0", "task")
        repository.move("t0", monday)
        assertEquals(1_000L, dao.all.single().updatedAt)

        repository.rename("t0", "renamed")
        assertEquals(2_000L, dao.all.single().updatedAt)
    }
}
