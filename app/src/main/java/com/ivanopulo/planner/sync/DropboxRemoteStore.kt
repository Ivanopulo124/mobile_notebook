package com.ivanopulo.planner.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Stores the sync file in the app folder (Apps/<app name>) of the user's Dropbox. */
class DropboxRemoteStore(private val accessToken: String, private val http: HttpClient) : RemoteStore {

    override suspend fun download(): String? = withContext(Dispatchers.IO) {
        val response = http.execute(
            HttpRequest(
                method = "POST",
                url = "$CONTENT/files/download",
                headers = mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Dropbox-API-Arg" to """{"path":"$FILE_PATH"}""",
                ),
            )
        )
        if (response.code == 409 && "path/not_found" in response.body) null else response.successBody()
    }

    override suspend fun upload(content: String) = withContext(Dispatchers.IO) {
        http.execute(
            HttpRequest(
                method = "POST",
                url = "$CONTENT/files/upload",
                headers = mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Dropbox-API-Arg" to """{"path":"$FILE_PATH","mode":"overwrite","mute":true}""",
                    "Content-Type" to "application/octet-stream",
                ),
                body = content,
            )
        ).successBody()
        Unit
    }

    private companion object {
        const val CONTENT = "https://content.dropboxapi.com/2"
        const val FILE_PATH = "/planner.json"
    }
}
