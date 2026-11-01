package com.ivanopulo.planner.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The token comes from the Yandex ID SDK sign-in screen. It lives for about a year and cannot be
 * refreshed, so when it is rejected the user signs in again.
 */
class YandexAuth(
    private val clientId: String,
    private val secrets: SyncSecrets,
    private val http: HttpClient,
) : CloudAuth {
    override val isConfigured: Boolean get() = clientId.isNotEmpty()

    /** Returns the account email, or the login when the account has no email. */
    suspend fun finishSignIn(token: String): String {
        val info = withContext(Dispatchers.IO) {
            val response = http.execute(
                HttpRequest("GET", USER_INFO, headers = mapOf("Authorization" to "OAuth $token"))
            )
            json.decodeFromString(UserInfo.serializer(), response.successBody())
        }
        secrets.setYandexToken(token)
        return info.defaultEmail ?: info.login
    }

    override suspend fun accessToken(): String = secrets.yandexToken() ?: throw NotAuthorizedException()

    override fun invalidateAccessToken(): Boolean = false

    override fun remoteStore(accessToken: String): RemoteStore = YandexRemoteStore(accessToken, http)

    // Revoking needs the client secret, which a mobile app does not have; forgetting the token is enough.
    override suspend fun signOut() = Unit

    @Serializable
    private data class UserInfo(
        val login: String,
        @SerialName("default_email") val defaultEmail: String? = null,
    )

    private companion object {
        const val USER_INFO = "https://login.yandex.ru/info?format=json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
