package com.ivanopulo.planner.sync

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

class NotAuthorizedException : Exception("Google Drive access is not granted")

class GoogleAuth(context: Context) {
    private val client = Identity.getAuthorizationClient(context)
    private val scopes = listOf(Scope(DRIVE_APPDATA_SCOPE))

    /** Without [email] Google shows the account picker; with it, a granted token comes back silently. */
    suspend fun authorize(email: String? = null): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes)
            .apply { if (email != null) setAccount(googleAccount(email)) }
            .build()
        return client.authorize(request).await()
    }

    suspend fun accessToken(email: String): String {
        val result = authorize(email)
        if (result.hasResolution()) throw NotAuthorizedException()
        return result.accessToken ?: throw NotAuthorizedException()
    }

    fun resultFromIntent(data: Intent?): AuthorizationResult = client.getAuthorizationResultFromIntent(data)

    suspend fun clearToken(token: String) {
        client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
    }

    suspend fun revoke(email: String) {
        val request = RevokeAccessRequest.builder()
            .setAccount(googleAccount(email))
            .setScopes(scopes)
            .build()
        client.revokeAccess(request).await()
    }

    private fun googleAccount(email: String) = Account(email, "com.google")

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
