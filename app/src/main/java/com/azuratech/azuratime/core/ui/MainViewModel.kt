package com.azuratech.azuratime.core.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.domain.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ MAIN VIEW MODEL (v3.2.0-ai-native)
 * Application entry point. Strict MVI implementation.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: MainRepository,
) : AndroidViewModel(application) {

    private val _uiStateFlow = MutableStateFlow(MainUiState())
    val uiStateFlow: StateFlow<MainUiState> = _uiStateFlow.asStateFlow()

    private var revokeJob: Job? = null

    init {
        loadInitialData()
    }

    fun onEvent(event: MainUiEvent) {
        when (event) {
            MainUiEvent.InitializeApp -> initializeApp()
            is MainUiEvent.HandleRevoke -> {
                if (event.isRevoked) {
                    viewModelScope.launch {
                        repository.executeRevocationCleanup()
                        _uiStateFlow.update { it.copy(isRevoked = true) }
                    }
                }
            }
        }
    }

    private fun loadInitialData() {
        val email = when (val result = repository.getCurrentEmail()) {
            is Result.Success -> result.data
            else -> ""
        }
        _uiStateFlow.update { it.copy(currentEmail = email) }
    }

    private fun initializeApp() {
        if (_uiStateFlow.value.isInitialized) return
        _uiStateFlow.update { it.copy(isInitialized = true) }

        // 1. Awake AI Brain (Background)
        viewModelScope.launch {
            repository.initializeAiBrain(getApplication())
        }

        // 2. Security Cloud (Background)
        val uidResult = repository.getCurrentUid()
        if (uidResult is Result.Success && uidResult.data != null) {
            startRealtimeRevokeListener(uidResult.data!!)
        }
    }

    private fun startRealtimeRevokeListener(uid: String) {
        revokeJob?.cancel()

        revokeJob = viewModelScope.launch {
            repository.observeRevokeStatusFlow(uid).collect { result ->
                if (result is Result.Success && result.data) {
                    onEvent(MainUiEvent.HandleRevoke(true))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        revokeJob?.cancel()
    }
}
