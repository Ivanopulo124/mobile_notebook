package com.ivanopulo.planner.sync

/** Answers each request with the first handler whose URL prefix matches, and records the requests. */
class FakeHttpClient(private val handlers: List<Pair<String, (HttpRequest) -> HttpResponse>>) : HttpClient {
    val requests = mutableListOf<HttpRequest>()

    override fun execute(request: HttpRequest): HttpResponse {
        requests += request
        val handler = handlers.firstOrNull { request.url.startsWith(it.first) }?.second
            ?: error("Unexpected request ${request.method} ${request.url}")
        return handler(request)
    }
}
