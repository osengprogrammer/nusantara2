package com.azuratech.azuratime.features.session.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.session.SessionRepository
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.azuratech.azuratime.features.session.CreateSessionUseCase
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.domain.model.SessionType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@HiltViewModel
class SessionPickerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val accountRepository: AccountRepository,
    private val schoolRepository: com.azuratech.azuratime.features.school.domain.repository.SchoolRepository,
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

        viewModelScope.launch {
            combine(schoolIdFlow, accountIdFlow) { schoolId, accountId ->
                schoolId to accountId
            }.flatMapLatest { (schoolId, accountId) ->
                combine(
                    sessionRepository.observeAllSessionsFlow(schoolId),
                    accountRepository.getAccountFlow(accountId),
                    schoolRepository.observeClassesFlow(schoolId),
                ) { sessionsResult, accountResult, classesResult ->
                    PickerContainer(sessionsResult, accountResult, classesResult, schoolId)
                }
            }.collect { container ->
                _uiStateFlow.update { it.copy(isLoading = true) }

                val sessions = container.sessionsResult.getOrNull() ?: emptyList()
                val account = container.accountResult.getOrNull()
                val classes = container.classesResult.getOrNull() ?: emptyList()

                if (account != null) {
                    val membership = account.memberships[container.schoolId]
                    val role = membership?.role?.let { AccountRole.fromString(it) } ?: account.role
                    val assignments = membership?.assignments ?: emptyList()

                    val filteredSessions = if (role == AccountRole.ADMIN || role == AccountRole.SUPER_ADMIN) {
                        sessions
                    } else {
                        sessions.filter { sessionDetails ->
                            val session = sessionDetails.session
                            assignments.any { assignment ->
                                val classMatches = assignment.classId == session.classId
                                val subjectMatches = assignment.subjectId == null || assignment.subjectId == session.subjectId
                                classMatches && subjectMatches
                            }
                        }
                    }

                    // 🔥 AI Native: Populate className for regular sessions
                    filteredSessions.forEach { sessionDetails ->
                        sessionDetails.className = classes.find { it.id == sessionDetails.session.classId }?.name
                    }

                    // 🔥 Point B: If no sessions, provide "Ad-hoc" options from assignments
                    val finalSessions = if (filteredSessions.isEmpty() && assignments.isNotEmpty()) {
                        assignments.map { assignment ->
                            val classObj = classes.find { it.id == assignment.classId }
                            SessionWithDetails(
                                session = ClassSessionEntity(
                                    sessionId = "ADHOC_${assignment.classId}_${assignment.subjectId ?: "ALL"}",
                                    classId = assignment.classId,
                                    subjectId = assignment.subjectId,
                                    sessionType = if (assignment.subjectId != null) SessionType.ACADEMIC else SessionType.CLASS_WIDE,
                                    supervisorEmail = account.email,
                                    dayOfWeek = 0, // Ad-hoc
                                    startTime = "00:00",
                                    endTime = "23:59",
                                    schoolId = container.schoolId,
                                    lookupKey = "ADHOC_${UUID.randomUUID()}",
                                ),
                                subjectName = "Matrix Assignment",
                            ).apply {
                                className = classObj?.name ?: "Unknown Class"
                            }
                        }
                    } else {
                        filteredSessions
                    }

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
                            error = if (container.sessionsResult is Result.Failure) container.sessionsResult.error.message else null,
                        )
                    }
                } else {
                    _uiStateFlow.update { it.copy(isLoading = false, error = "Account not found") }
                }
            }
        }
    }

    private data class PickerContainer(
        val sessionsResult: Result<List<com.azuratech.azuratime.features.session.data.local.SessionWithDetails>>,
        val accountResult: Result<Account>,
        val classesResult: Result<List<com.azuratech.azuraengine.model.ClassModel>>,
        val schoolId: String,
    )

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
