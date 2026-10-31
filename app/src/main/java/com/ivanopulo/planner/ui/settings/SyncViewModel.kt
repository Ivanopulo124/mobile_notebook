package com.ivanopulo.planner.ui.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.ivanopulo.planner.PlannerApp
import com.ivanopulo.planner.sync.SyncManager
import com.ivanopulo.planner.sync.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SignInError { CONFIGURATION, FAILED }

class SyncViewModel(private val sync: SyncManager) : ViewModel() {

    val status: StateFlow<SyncStatus?> =
        sync.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _signInError = MutableStateFlow<SignInError?>(null)
    val signInError: StateFlow<SignInError?> = _signInError.asStateFlow()

    private val _consentRequests = Channel<PendingIntent>(Channel.BUFFERED)
    val consentRequests: Flow<PendingIntent> = _consentRequests.receiveAsFlow()

    fun signIn() = runSignInStep {
        sync.beginSignIn()?.let { _consentRequests.send(it) }
    }

    fun onConsentResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        runSignInStep { sync.finishSignIn(data) }
    }

    fun syncNow() = sync.requestSync()

    fun signOut() {
        viewModelScope.launch { sync.signOut() }
    }

    private fun runSignInStep(step: suspend () -> Unit) {
        viewModelScope.launch {
            _signInError.value = null
            _signingIn.value = true
            try {
                step()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Google sign-in failed", e)
                // DEVELOPER_ERROR means no OAuth client matches this package name and signing key.
                val misconfigured = e is ApiException && e.statusCode == CommonStatusCodes.DEVELOPER_ERROR
                _signInError.value = if (misconfigured) SignInError.CONFIGURATION else SignInError.FAILED
            } finally {
                _signingIn.value = false
            }
        }
    }

    companion object {
        private const val TAG = "PlannerSync"

        val Factory = viewModelFactory {
            initializer {
                SyncViewModel((this[APPLICATION_KEY] as PlannerApp).container.syncManager)
            }
        }
    }
}
