package com.ivanopulo.planner.sync

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Proof Key for Code Exchange (RFC 7636), which lets an app without a client secret use the code flow. */
object Pkce {
    private val random = SecureRandom()

    fun newVerifier(): String = base64Url(ByteArray(32).also(random::nextBytes))

    fun challenge(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    private fun base64Url(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
