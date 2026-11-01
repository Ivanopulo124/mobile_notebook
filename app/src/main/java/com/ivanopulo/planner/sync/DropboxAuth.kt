package com.ivanopulo.planner.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * OAuth 2 code flow with PKCE: the app key is public, and a long-lived refresh token is exchanged for
 * access tokens that expire after a few hours.
 */
class DropboxAuth(
    private val appKey: String,
    private val secrets: SyncSecrets,
    private val http: HttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
) : CloudAuth {
    private val redirectUri = "$REDIRECT_SCHEME_PREFIX$appKey://oauth"

    @Volatile
    private var cached: CachedToken? = null

    override val isConfigured: Boolean get() = appKey.isNotEmpty()

    suspend fun authorizationUrl(): String {
        val pending = PendingAuth(verifier = Pkce.newVerifier(), state = Pkce.newVerifier())
        secrets.savePendingDropbox(pending)
        return AUTHORIZE + "?" + form(
            "client_id" to appKey,
            "response_type" to "code",
            "token_access_type" to "offline",
            "code_challenge" to Pkce.challenge(pending.verifier),
            "code_challenge_method" to "S256",
            "redirect_uri" to redirectUri,
            "state" to pending.state,
        )
    }

    /** Returns the account email, or null when the user declined on the consent page. */
    suspend fun finishSignIn(code: String?, state: String?): String? {
        val pending = secrets.takePendingDropbox() ?: throw NotAuthorizedException()
        if (code == null) return null
        if (state != pending.state) throw NotAuthorizedException()
        val token = requestToken(
            "grant_type" to "authorization_code",
            "code" to code,
            "client_id" to appKey,
            "code_verifier" to pending.verifier,
            "redirect_uri" to redirectUri,
        )
        secrets.setDropboxRefreshToken(token.refreshToken ?: throw NotAuthorizedException())
        remember(token)
        return accountEmail(token.accessToken)
    }

    override suspend fun accessToken(): String {
        cached?.takeIf { it.expiresAt > clock() + EXPIRY_MARGIN_MS }?.let { return it.value }
        val refreshToken = secrets.dropboxRefreshToken() ?: throw NotAuthorizedException()
        val token = requestToken(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to appKey,
        )
        return remember(token)
    }

    override fun invalidateAccessToken(): Boolean {
        cached = null
        return true
    }

    override fun remoteStore(accessToken: String): RemoteStore = DropboxRemoteStore(accessToken, http)

    override suspend fun signOut() {
        val token = accessToken()
        withContext(Dispatchers.IO) { http.execute(rpc(REVOKE, token)).successBody() }
        cached = null
    }

    private suspend fun accountEmail(accessToken: String): String = withContext(Dispatchers.IO) {
        json.decodeFromString(Account.serializer(), http.execute(rpc(CURRENT_ACCOUNT, accessToken)).successBody()).email
    }

    private suspend fun requestToken(vararg params: Pair<String, String>): TokenResponse =
        withContext(Dispatchers.IO) {
            val response = http.execute(
                HttpRequest(
                    method = "POST",
                    url = TOKEN,
                    headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    body = form(*params),
                )
            )
            // A revoked or expired refresh token comes back as 400 invalid_grant rather than 401.
            if (response.code == 400 && "invalid_grant" in response.body) throw RemoteAuthException()
            parseTokenResponse(response.successBody())
        }

    private fun remember(token: TokenResponse): String {
        cached = CachedToken(token.accessToken, clock() + token.expiresIn * 1000)
        return token.accessToken
    }

    private class CachedToken(val value: String, val expiresAt: Long)

    @Serializable
    data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Long,
        @SerialName("refresh_token") val refreshToken: String? = null,
    )

    @Serializable
    private data class Account(val email: String)

    companion object {
        const val REDIRECT_SCHEME_PREFIX = "db-"
        private const val AUTHORIZE = "https://www.dropbox.com/oauth2/authorize"
        private const val TOKEN = "https://api.dropboxapi.com/oauth2/token"
        private const val REVOKE = "https://api.dropboxapi.com/2/auth/token/revoke"
        private const val CURRENT_ACCOUNT = "https://api.dropboxapi.com/2/users/get_current_account"
        private const val EXPIRY_MARGIN_MS = 60_000L

        private val json = Json { ignoreUnknownKeys = true }

        fun parseTokenResponse(body: String): TokenResponse = json.decodeFromString(TokenResponse.serializer(), body)

        // Endpoints without arguments still expect a JSON body.
        private fun rpc(url: String, accessToken: String) = HttpRequest(
            method = "POST",
            url = url,
            headers = mapOf("Authorization" to "Bearer $accessToken", "Content-Type" to "application/json"),
            body = "null",
        )

        private fun form(vararg params: Pair<String, String>) =
            params.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
    }
}
