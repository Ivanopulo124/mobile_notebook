package com.ivanopulo.planner.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Stores the sync file in the app folder (Applications/<app name>) of the user's Yandex Disk. The API
 * hands out a one-off link for each download and upload.
 */
class YandexRemoteStore(private val accessToken: String, private val http: HttpClient) : RemoteStore {

    override suspend fun download(): String? = withContext(Dispatchers.IO) {
        val link = http.execute(api("GET", "$API/resources/download?path=${encode(FILE_PATH)}"))
        if (link.code == 404) return@withContext null
        http.execute(HttpRequest("GET", href(link))).successBody()
    }

    override suspend fun upload(content: String) = withContext(Dispatchers.IO) {
        val linkUrl = "$API/resources/upload?path=${encode(FILE_PATH)}&overwrite=true"
        var link = http.execute(api("GET", linkUrl))
        if (link.code == 409) {
            // The app folder does not exist yet; 409 on creation means another device just made it.
            val created = http.execute(api("PUT", "$API/resources?path=${encode(APP_FOLDER)}"))
            if (created.code != 409) created.successBody()
            link = http.execute(api("GET", linkUrl))
        }
        http.execute(HttpRequest("PUT", href(link), body = content)).successBody()
        Unit
    }

    private fun api(method: String, url: String) =
        HttpRequest(method, url, headers = mapOf("Authorization" to "OAuth $accessToken"))

    private fun href(link: HttpResponse): String = json.decodeFromString(Link.serializer(), link.successBody()).href

    @Serializable
    private data class Link(val href: String)

    private companion object {
        const val API = "https://cloud-api.yandex.net/v1/disk"
        const val APP_FOLDER = "app:/"
        const val FILE_PATH = "app:/planner.json"
        val json = Json { ignoreUnknownKeys = true }

        fun encode(path: String): String = URLEncoder.encode(path, "UTF-8")
    }
}
