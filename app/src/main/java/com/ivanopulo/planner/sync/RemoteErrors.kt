package com.ivanopulo.planner.sync

import java.io.IOException

class NotAuthorizedException : Exception("Cloud access is not granted")

class RemoteAuthException : IOException("The cloud rejected the access token")

class RemoteHttpException(val code: Int, message: String) : IOException("HTTP $code: $message")
