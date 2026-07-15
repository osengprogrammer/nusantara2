package com.azuratech.azuratime.core.boot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.domain.repository.BootRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.azuratech.azuratime.core.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 🚀 BOOT VIEW MODEL (v3.2.0-ai-native)
 * First gate of the application. Optimized for "Ultra-Stable" Solid-State transitions.
 */
@HiltViewModel
class BootViewModel @Inject constructor(
    application: Application,
    private val repository: BootRepository,
    private val sessionManager: SessionManager,
) : AndroidViewModel(application) {

    private val _uiStateFlow = MutableStateFlow<BootUiState>(
        if (sessionManager.getCurrentAccountId() == null) BootUiState.Auth else BootUiState.Loading,
    )
    val uiStateFlow: StateFlow<BootUiState> = _uiStateFlow.asStateFlow()

    val isLoggingOut: StateFlow<Boolean> = sessionManager.isLoggingOutFlow
    val isSessionClearing: StateFlow<Boolean> = sessionManager.isSessionClearingFlow

    private var authCheckJob: Job? = null

    init {
        observeSession()
    }

    /**
     * 🔐 OBSERVE SESSION
     * Reactive entry point. Uses StateFlow's native distinctUntilChanged behavior.
     */
    private fun observeSession() {
        sessionManager.currentAccountIdFlow
            .onEach { accountId ->
                if (accountId == null) {
                    authCheckJob?.cancel()

                    // 🔥 AI Native: If logging out, add a small buffer for cleanup
                    if (sessionManager.isLoggingOutFlow.value) {
                        kotlinx.coroutines.delay(500)
                    }

                    _uiStateFlow.value = BootUiState.Auth
                } else {
                    handleCheckAuthStatus()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BootUiEvent) {
        when (event) {
            BootUiEvent.CheckAuthStatus -> handleCheckAuthStatus()
            BootUiEvent.Recheck -> handleCheckAuthStatus()
        }
    }

    /**
     * 🛡️ HANDLE CHECK AUTH STATUS
     * "Ultra-Stable" logic with Synchronous Decoupling and Double-Lock Guarding.
     */
    private fun handleCheckAuthStatus() {
        // 1. Job Hygiene: Instantly kill any ghost repository checks.
        authCheckJob?.cancel()

        // 2. Early Exit: Respect the atomic exit flag.
        if (sessionManager.isLoggingOutFlow.value) return

        // 3. Synchronous Validation: Force Auth state synchronously without
        // spinning up a coroutine or Loading UI if the ID is null.
        if (sessionManager.getCurrentAccountId() == null) {
            _uiStateFlow.value = BootUiState.Auth
            return
        }

        // 4. Safe to proceed with async verification.
        authCheckJob = viewModelScope.launch {
            _uiStateFlow.value = BootUiState.Loading

            withContext(Dispatchers.IO) {
                delay(600) // For encryption stability

                // 🔒 Double-Lock Guarding: Re-verify logout/ID status after delay.
                if (sessionManager.isLoggingOutFlow.value) return@withContext
                if (sessionManager.getCurrentAccountId() == null) {
                    _uiStateFlow.value = BootUiState.Auth
                    return@withContext
                }

                val accountResult = repository.getCurrentAccount()
                val isLoggedIn = accountResult is Result.Success && accountResult.data != null

                if (!isLoggedIn) {
                    _uiStateFlow.value = BootUiState.Auth
                } else {
                    when (val result = repository.isSessionActive()) {
                        is Result.Success -> {
                            _uiStateFlow.value = if (result.data) BootUiState.Ready else BootUiState.NeedActivation
                        }
                        is Result.Failure -> {
                            _uiStateFlow.value = BootUiState.Error(result.error.message ?: "Failed to load session")
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
