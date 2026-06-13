package com.azuratech.azuratime.features.session.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionPickerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(SessionPickerUiState())
    val uiStateFlow: StateFlow<SessionPickerUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<SessionPickerUiEffect>()
    val uiEffectFlow: SharedFlow<SessionPickerUiEffect> = _uiEffectFlow.asSharedFlow()

    init {
        val schoolId = sessionManager.getActiveSchoolId()
        if (schoolId != null) {
            loadSessions(schoolId)
        } else {
            _uiStateFlow.update { it.copy(error = "No active school selected") }
        }
    }

    fun onEvent(event: SessionPickerUiEvent) {
        when (event) {
            is SessionPickerUiEvent.LoadSessions -> loadSessions(event.schoolId)
            is SessionPickerUiEvent.SelectSession -> selectSession(event.sessionId)
            SessionPickerUiEvent.Refresh -> sessionManager.getActiveSchoolId()?.let { loadSessions(it) }
        }
    }

    private fun loadSessions(schoolId: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }

            // 🔥 AI Native: Show ALL sessions in manual picker to prevent "empty list" confusion
            sessionRepository.observeAllSessionsFlow(schoolId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiStateFlow.update {
                            it.copy(
                                isLoading = false,
                                sessions = result.data,
                                error = null,
                            )
                        }
                    }
                    is Result.Failure -> {
                        _uiStateFlow.update {
                            it.copy(
                                isLoading = false,
                                error = result.error.message,
                            )
                        }
                        _uiEffectFlow.emit(SessionPickerUiEffect.ShowError(result.error.message ?: "Failed to load sessions"))
                    }
                    is Result.Loading -> {
                        _uiStateFlow.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun selectSession(sessionId: String) {
        viewModelScope.launch {
            _uiEffectFlow.emit(SessionPickerUiEffect.NavigateToScanner(sessionId))
        }
    }
}
