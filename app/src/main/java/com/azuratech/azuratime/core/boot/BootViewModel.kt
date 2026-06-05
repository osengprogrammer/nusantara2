package com.azuratech.azuratime.core.boot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.repository.BootRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 🚀 BOOT VIEW MODEL (v3.2.0-ai-native)
 * First gate of the application. Strict MVI implementation.
 */
@HiltViewModel
class BootViewModel @Inject constructor(
    application: Application,
    private val repository: BootRepository,
) : AndroidViewModel(application) {

    private val _uiStateFlow = MutableStateFlow<BootUiState>(BootUiState.Loading)
    val uiStateFlow: StateFlow<BootUiState> = _uiStateFlow.asStateFlow()

    init {
        onEvent(BootUiEvent.CheckAuthStatus)
    }

    fun onEvent(event: BootUiEvent) {
        when (event) {
            BootUiEvent.CheckAuthStatus -> handleCheckAuthStatus()
            BootUiEvent.Recheck -> handleCheckAuthStatus()
        }
    }

    private fun handleCheckAuthStatus() {
        viewModelScope.launch {
            _uiStateFlow.value = BootUiState.Loading

            withContext(Dispatchers.IO) {
                delay(600) // For encryption stability
                val accountResult = repository.getCurrentAccount()

                val isLoggedIn = accountResult is Result.Success && accountResult.data != null

                if (!isLoggedIn) {
                    _uiStateFlow.value = BootUiState.NeedLogin
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
