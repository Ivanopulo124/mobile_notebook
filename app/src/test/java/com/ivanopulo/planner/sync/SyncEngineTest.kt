package com.ivanopulo.planner.sync

import com.ivanopulo.planner.data.FakeTaskDao
import com.ivanopulo.planner.data.TaskDao
import com.ivanopulo.planner.data.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEngineTest {
    private val remote = FakeRemoteStore()

    private fun engine(dao: TaskDao) = SyncEngine(SyncStore(dao) { it() }, remote)

    @Test
    fun firstSyncUploadsLocalTasks() = runBlocking {
        val dao = FakeTaskDao().apply { upsert(task("a")) }

        engine(dao).sync()

        assertEquals(listOf(task("a")), SyncCodec.decode(remote.content!!))
    }

    @Test
    fun twoDevicesConverge() = runBlocking {
        val phone = FakeTaskDao().apply { upsert(task("p")) }
        val tablet = FakeTaskDao().apply { upsert(task("t")) }

        engine(phone).sync()
        engine(tablet).sync()
        engine(phone).sync()

        assertEquals(setOf("p", "t"), phone.all.map { it.id }.toSet())
        assertEquals(phone.all.toSet(), tablet.all.toSet())
    }

    @Test
    fun remoteDeletionReachesThisDevice() = runBlocking {
        val phone = FakeTaskDao().apply { upsert(task("a")) }
        engine(phone).sync()
        remote.content = SyncCodec.encode(listOf(task("a", updatedAt = 2, deleted = true)))

        engine(phone).sync()

        assertTrue(phone.all.single().deleted)
    }

    @Test
    fun nothingIsUploadedWhenAlreadyInSync() = runBlocking {
        val dao = FakeTaskDao().apply { upsert(task("a")) }
        engine(dao).sync()

        engine(dao).sync()

        assertEquals(1, remote.uploads)
    }

    @Test
    fun localEditDuringSyncIsNotOverwritten() = runBlocking {
        val dao = FakeTaskDao().apply { upsert(task("a", title = "original", updatedAt = 1)) }
        remote.content = SyncCodec.encode(listOf(task("a", title = "remote", updatedAt = 2)))
        // The user renames the task after the engine took its snapshot but before the merge is applied.
        val racingDao = object : TaskDao by dao {
            override suspend fun getAll(): List<TaskEntity> = dao.getAll().also {
                dao.upsert(task("a", title = "typed just now", updatedAt = 3))
            }
        }

        engine(racingDao).sync()
        assertEquals("typed just now", dao.all.single().title)

        engine(dao).sync()
        assertEquals("typed just now", SyncCodec.decode(remote.content!!).single().title)
    }
}
