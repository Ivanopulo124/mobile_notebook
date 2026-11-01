package com.ivanopulo.planner.sync

import java.net.HttpURLConnection
import java.net.URL

data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

class HttpResponse(val code: Int, val body: String)

fun interface HttpClient {
    /** Blocks; callers switch to the IO dispatcher. */
    fun execute(request: HttpRequest): HttpResponse
}

/** Returns the body of a 2xx response. 401 means the access token is no longer valid. */
fun HttpResponse.successBody(): String {
    if (code == HttpURLConnection.HTTP_UNAUTHORIZED) throw RemoteAuthException()
    if (code !in 200..299) throw RemoteHttpException(code, body.take(500))
    return body
}

class UrlConnectionHttpClient : HttpClient {
    override fun execute(request: HttpRequest): HttpResponse {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            request.headers.forEach(connection::setRequestProperty)
            if (request.body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(request.body.toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            return HttpResponse(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000
    }
}
