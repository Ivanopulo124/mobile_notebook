package com.ivanopulo.planner.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DriveAuthException : IOException("Drive rejected the access token")

class DriveHttpException(val code: Int, message: String) : IOException("Drive HTTP $code: $message")

/** Stores the sync file in the hidden appDataFolder of the user's Drive, via the Drive v3 REST API. */
class DriveRemoteStore(private val accessToken: String) : RemoteStore {
    private var fileId: String? = null

    override suspend fun download(): String? = withContext(Dispatchers.IO) {
        val id = findFileId() ?: return@withContext null
        request("GET", "$API/files/$id?alt=media")
    }

    override suspend fun upload(content: String) = withContext(Dispatchers.IO) {
        val id = findFileId() ?: createFile()
        request("PATCH", "$UPLOAD/files/$id?uploadType=media", content)
        Unit
    }

    suspend fun accountEmail(): String = withContext(Dispatchers.IO) {
        val about = parse(request("GET", "$API/about?fields=user(emailAddress)"))
        about["user"]!!.jsonObject["emailAddress"]!!.jsonPrimitive.content
    }

    private fun findFileId(): String? {
        fileId?.let { return it }
        val query = URLEncoder.encode("name='$FILE_NAME' and trashed=false", "UTF-8")
        // If two devices created the file concurrently, all of them settle on the oldest one.
        val response = request(
            "GET",
            "$API/files?spaces=appDataFolder&q=$query&orderBy=createdTime&fields=files(id)",
        )
        return parse(response)["files"]!!.jsonArray.firstOrNull()
            ?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?.also { fileId = it }
    }

    private fun createFile(): String {
        val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"],"mimeType":"application/json"}"""
        val id = parse(request("POST", "$API/files?fields=id", metadata))["id"]!!.jsonPrimitive.content
        fileId = id
        return id
    }

    private fun request(method: String, url: String, body: String? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            if (method == "PATCH") {
                // HttpURLConnection does not officially support PATCH; Google APIs honour the override.
                connection.requestMethod = "POST"
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                connection.requestMethod = method
            }
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) throw DriveAuthException()
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw DriveHttpException(code, error.take(500))
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(response: String): JsonObject = Json.parseToJsonElement(response).jsonObject

    private companion object {
        const val API = "https://www.googleapis.com/drive/v3"
        const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        const val FILE_NAME = "planner.json"
        const val TIMEOUT_MS = 30_000
    }
}
