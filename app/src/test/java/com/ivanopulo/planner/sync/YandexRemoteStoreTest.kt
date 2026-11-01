package com.ivanopulo.planner.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YandexRemoteStoreTest {

    @Test
    fun missingFileDownloadsAsNull() = runBlocking {
        val http = FakeHttpClient(listOf(DOWNLOAD_LINK to { _ -> HttpResponse(404, """{"error":"DiskNotFoundError"}""") }))
        assertNull(YandexRemoteStore("token", http).download())
    }

    @Test
    fun downloadFollowsTheLink() = runBlocking {
        val http = FakeHttpClient(
            listOf(
                DOWNLOAD_LINK to { _ -> HttpResponse(200, """{"href":"https://downloader/file","method":"GET"}""") },
                "https://downloader/file" to { _ -> HttpResponse(200, "content") },
            )
        )
        assertEquals("content", YandexRemoteStore("token", http).download())
        assertEquals("OAuth token", http.requests.first().headers["Authorization"])
        assertEquals(
            "$DOWNLOAD_LINK?path=app%3A%2Fplanner.json",
            http.requests.first().url,
        )
    }

    @Test
    fun uploadCreatesMissingAppFolder() = runBlocking {
        var folderExists = false
        val http = FakeHttpClient(
            listOf(
                UPLOAD_LINK to { _ ->
                    if (folderExists) HttpResponse(200, """{"href":"https://uploader/file","method":"PUT"}""")
                    else HttpResponse(409, """{"error":"DiskPathDoesntExistsError"}""")
                },
                RESOURCES to { _ ->
                    folderExists = true
                    HttpResponse(201, """{"href":"https://cloud-api.yandex.net/v1/disk/resources?path=app%3A%2F"}""")
                },
                "https://uploader/file" to { _ -> HttpResponse(201, "") },
            )
        )
        YandexRemoteStore("token", http).upload("content")
        assertEquals(listOf("GET", "PUT", "GET", "PUT"), http.requests.map { it.method })
        assertEquals("content", http.requests.last().body)
    }

    private companion object {
        const val DOWNLOAD_LINK = "https://cloud-api.yandex.net/v1/disk/resources/download"
        const val UPLOAD_LINK = "https://cloud-api.yandex.net/v1/disk/resources/upload"
        const val RESOURCES = "https://cloud-api.yandex.net/v1/disk/resources?"
    }
}
