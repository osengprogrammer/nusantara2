package com.azuratech.azuratime.features.session.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.azuratech.azuratime.features.session.domain.usecase.CreateSessionUseCase
import com.azuratech.azuratime.features.session.domain.usecase.GetAssignedSessionsUseCase
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@HiltViewModel
class SessionPickerViewModel @Inject constructor(
    private val getAssignedSessionsUseCase: GetAssignedSessionsUseCase,
    private val sessionManager: SessionManager,
    private val createSessionUseCase: CreateSessionUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(SessionPickerUiState())
    val uiStateFlow: StateFlow<SessionPickerUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<SessionPickerUiEffect>()
    val uiEffectFlow: SharedFlow<SessionPickerUiEffect> = _uiEffectFlow.asSharedFlow()

    init {
        observeSessions()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeSessions() {
        val schoolIdFlow = sessionManager.activeSchoolIdFlow.filterNotNull()
        val accountIdFlow = sessionManager.currentAccountIdFlow.filterNotNull()

        combine(schoolIdFlow, accountIdFlow) { schoolId, accountId ->
            schoolId to accountId
        }.flatMapLatest { (schoolId, accountId) ->
            getAssignedSessionsUseCase(schoolId, accountId)
        }.onEach { result ->
            when (result) {
                is Result.Loading -> {
                    _uiStateFlow.update { it.copy(isLoading = true) }
                }
                is Result.Success -> {
                    val finalSessions = result.data
                    _uiStateFlow.update {
                        it.copy(
                            isLoading = false,
                            sessions = finalSessions,
                            filteredSessions = if (it.searchQuery.isEmpty()) {
                                finalSessions
                            } else {
                                finalSessions.filter { s ->
                                    (s.subjectName ?: "").contains(it.searchQuery, ignoreCase = true) ||
                                        (s.className ?: "").contains(it.searchQuery, ignoreCase = true)
                                }
                            },
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
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SessionPickerUiEvent) {
        when (event) {
            is SessionPickerUiEvent.LoadSessions -> { /* Handled by observer */ }
            is SessionPickerUiEvent.SelectSession -> selectSession(event.sessionId)
            is SessionPickerUiEvent.UpdateSearchQuery -> updateSearch(event.query)
            SessionPickerUiEvent.Refresh -> { /* Flows will auto-refresh */ }
        }
    }

    private fun updateSearch(query: String) {
        _uiStateFlow.update { state ->
            val filtered = if (query.isBlank()) {
                state.sessions
            } else {
                state.sessions.filter {
                    (it.subjectName ?: "").contains(query, ignoreCase = true) ||
                        (it.className ?: "").contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, filteredSessions = filtered)
        }
    }

    private fun selectSession(sessionId: String) {
        viewModelScope.launch {
            if (sessionId.startsWith("ADHOC_")) {
                // Create the actual session record before navigating
                val state = _uiStateFlow.value
                val adhocSession = state.sessions.find { it.session.sessionId == sessionId }?.session
                if (adhocSession != null) {
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val now = LocalTime.now()

                    val result = createSessionUseCase(
                        classId = adhocSession.classId,
                        subjectId = adhocSession.subjectId,
                        sessionType = adhocSession.sessionType,
                        supervisorEmail = adhocSession.supervisorEmail,
                        dayOfWeek = java.time.LocalDate.now().dayOfWeek.value,
                        startTime = now.format(timeFormatter),
                        endTime = now.plusHours(2).format(timeFormatter),
                        schoolId = adhocSession.schoolId,
                    )

                    when (result) {
                        is Result.Success -> {
                            _uiEffectFlow.emit(SessionPickerUiEffect.NavigateToScanner(result.data))
                        }
                        is Result.Failure -> {
                            _uiEffectFlow.emit(SessionPickerUiEffect.ShowError("Failed to start session: ${result.error.message}"))
                        }
                        else -> {}
                    }
                }
            } else {
                _uiEffectFlow.emit(SessionPickerUiEffect.NavigateToScanner(sessionId))
            }
        }
    }
}
