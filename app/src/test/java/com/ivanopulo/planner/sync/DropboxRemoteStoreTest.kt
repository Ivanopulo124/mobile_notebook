package com.ivanopulo.planner.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DropboxRemoteStoreTest {

    @Test
    fun missingFileDownloadsAsNull() = runBlocking {
        val http = FakeHttpClient(
            listOf(DOWNLOAD to { _ -> HttpResponse(409, """{"error_summary":"path/not_found/.."}""") })
        )
        assertNull(DropboxRemoteStore("token", http).download())
    }

    @Test
    fun downloadReturnsFileContent() = runBlocking {
        val http = FakeHttpClient(listOf(DOWNLOAD to { _ -> HttpResponse(200, "content") }))
        assertEquals("content", DropboxRemoteStore("token", http).download())
        val request = http.requests.single()
        assertEquals("Bearer token", request.headers["Authorization"])
        assertEquals("""{"path":"/planner.json"}""", request.headers["Dropbox-API-Arg"])
    }

    @Test
    fun uploadOverwritesFile() = runBlocking {
        val http = FakeHttpClient(listOf(UPLOAD to { _ -> HttpResponse(200, "{}") }))
        DropboxRemoteStore("token", http).upload("content")
        val request = http.requests.single()
        assertEquals("content", request.body)
        assertTrue(request.headers["Dropbox-API-Arg"]!!.contains(""""mode":"overwrite""""))
    }

    @Test(expected = RemoteAuthException::class)
    fun expiredTokenIsAnAuthError(): Unit = runBlocking {
        val http = FakeHttpClient(listOf(DOWNLOAD to { _ -> HttpResponse(401, "expired_access_token") }))
        DropboxRemoteStore("token", http).download()
    }

    @Test
    fun tokenResponseIgnoresUnknownFields() {
        val token = DropboxAuth.parseTokenResponse(
            """{"access_token":"a","expires_in":14400,"token_type":"bearer","refresh_token":"r","account_id":"x"}"""
        )
        assertEquals(DropboxAuth.TokenResponse("a", 14400, "r"), token)
        assertNull(DropboxAuth.parseTokenResponse("""{"access_token":"a","expires_in":1}""").refreshToken)
    }

    private companion object {
        const val DOWNLOAD = "https://content.dropboxapi.com/2/files/download"
        const val UPLOAD = "https://content.dropboxapi.com/2/files/upload"
    }
}
