package com.example.todoit.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.data.local.AppPreferences
import com.example.todoit.data.remote.auth.GoogleAuthManager
import com.example.todoit.data.sync.SyncScheduler
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val accountEmail: String? = null,
    val isSignedIn: Boolean = false,
    val lastSyncAt: Long = 0L,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val syncScheduler: SyncScheduler,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val account = authManager.getSignedInAccount()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    accountEmail = account?.email,
                    isSignedIn   = account != null,
                    lastSyncAt   = prefs.getLastSyncAt(),
                )
            }
        }
    }

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    fun onSignInResult(data: Intent?) {
        // data is null when the user pressed Back on the sign-in screen – just ignore it.
        if (data == null) return
        viewModelScope.launch {
            runCatching {
                // authManager.handleSignInResult parses the intent and extracts the account.
                // This MUST be called before getSignedInAccount() has any new value.
                val account = authManager.handleSignInResult(data)
                _uiState.update {
                    it.copy(accountEmail = account.email, isSignedIn = true, syncError = null)
                }
                syncScheduler.triggerImmediateSync()
            }.onFailure { e ->
                val msg = when (e) {
                    is ApiException ->
                        "Sign-in failed (code ${e.statusCode}). " +
                        "Ensure google-services.json is present and the OAuth client is configured."
                    else -> "Sign-in failed: ${e.message}"
                }
                _uiState.update { it.copy(syncError = msg) }
            }
        }
    }

    fun signOut() = viewModelScope.launch {
        runCatching { authManager.signOut() }
        _uiState.update { it.copy(accountEmail = null, isSignedIn = false) }
    }

    fun syncNow() {
        _uiState.update { it.copy(isSyncing = true, syncError = null) }
        syncScheduler.triggerImmediateSync()
        viewModelScope.launch {
            kotlinx.coroutines.delay(1_000)
            _uiState.update { it.copy(isSyncing = false, lastSyncAt = prefs.getLastSyncAt()) }
        }
    }
}

