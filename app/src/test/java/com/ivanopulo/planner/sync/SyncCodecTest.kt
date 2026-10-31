package com.ivanopulo.planner.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncCodecTest {

    @Test
    fun roundTripKeepsEveryField() {
        val tasks = listOf(
            task("a", title = "Купить хлеб", updatedAt = 42, done = true),
            task("b", date = null, deleted = true).copy(color = 3, position = 7),
        )
        assertEquals(tasks, SyncCodec.decode(SyncCodec.encode(tasks)))
    }

    @Test
    fun unknownFieldsFromNewerMinorVersionsAreIgnored() {
        val json = """{"version":1,"device":"x","tasks":[{"id":"a","title":"t","updatedAt":1,"reminder":"09:00"}]}"""
        assertEquals("t", SyncCodec.decode(json).single().title)
    }

    @Test(expected = UnsupportedSyncFormatException::class)
    fun newerFormatVersionIsRejected() {
        SyncCodec.decode("""{"version":2,"tasks":[]}""")
    }
}
