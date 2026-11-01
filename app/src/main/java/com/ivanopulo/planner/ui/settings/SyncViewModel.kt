package com.ivanopulo.planner.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ivanopulo.planner.PlannerApp
import com.ivanopulo.planner.sync.SyncManager
import com.ivanopulo.planner.sync.SyncProvider
import com.ivanopulo.planner.sync.SyncStatus
import com.yandex.authsdk.YandexAuthResult
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

class SyncViewModel(private val sync: SyncManager) : ViewModel() {

    val status: StateFlow<SyncStatus?> =
        sync.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val providers: List<SyncProvider> = sync.availableProviders

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _signInFailed = MutableStateFlow(false)
    val signInFailed: StateFlow<Boolean> = _signInFailed.asStateFlow()

    private val _browserRequests = Channel<String>(Channel.BUFFERED)
    val browserRequests: Flow<String> = _browserRequests.receiveAsFlow()

    fun signInWithDropbox() = runSignInStep {
        _browserRequests.send(sync.dropboxAuthorizationUrl())
    }

    fun onDropboxRedirect(code: String?, state: String?) = runSignInStep {
        sync.finishDropboxSignIn(code, state)
    }

    fun onYandexResult(result: YandexAuthResult) {
        when (result) {
            is YandexAuthResult.Success -> runSignInStep { sync.finishYandexSignIn(result.token.value) }
            is YandexAuthResult.Failure -> {
                Log.w(TAG, "Yandex sign-in failed", result.exception)
                _signInFailed.value = true
            }
            else -> Unit
        }
    }

    fun onSignInLaunchFailed() {
        _signInFailed.value = true
    }

    fun syncNow() = sync.requestSync()

    fun signOut() {
        viewModelScope.launch { sync.signOut() }
    }

    private fun runSignInStep(step: suspend () -> Unit) {
        viewModelScope.launch {
            _signInFailed.value = false
            _signingIn.value = true
            try {
                step()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Sign-in failed", e)
                _signInFailed.value = true
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
