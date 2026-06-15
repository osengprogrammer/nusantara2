package com.azuratech.azuratime.features.session.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.CreateSessionUseCase
import com.azuratech.azuratime.features.session.SessionRepository
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

import com.azuratech.azuratime.features.session.domain.model.SessionType

@HiltViewModel
class SessionManagementViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val schoolRepository: SchoolRepository,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    private val createSessionUseCase: CreateSessionUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(SessionManagementUiState())
    val uiStateFlow: StateFlow<SessionManagementUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<SessionManagementUiEffect>()
    val uiEffectFlow: SharedFlow<SessionManagementUiEffect> = _uiEffectFlow.asSharedFlow()

    init {
        observeData()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeData() {
        val schoolIdFlow = sessionManager.activeSchoolIdFlow.filterNotNull()
        val accountIdFlow = sessionManager.currentAccountIdFlow.filterNotNull()

        viewModelScope.launch {
            combine(schoolIdFlow, accountIdFlow) { schoolId, accountId ->
                schoolId to accountId
            }.flatMapLatest { (schoolId, accountId) ->
                combine(
                    sessionRepository.observeAllSubjectsFlow(schoolId),
                    sessionRepository.observeAllSessionsFlow(schoolId),
                    schoolRepository.observeClassesFlow(schoolId),
                    accountRepository.getAccountFlow(accountId),
                ) { subjectsResult, sessionsResult, classesResult, accountResult ->
                    DataContainer(subjectsResult, sessionsResult, classesResult, accountResult, schoolId)
                }
            }.collect { container ->
                val subjects = container.subjectsResult.getOrNull() ?: emptyList()
                val sessions = container.sessionsResult.getOrNull() ?: emptyList()
                val classes = container.classesResult.getOrNull() ?: emptyList()
                val account = container.accountResult.getOrNull()

                if (account != null) {
                    val membership = account.memberships[container.schoolId]
                    val role = membership?.role?.let { AccountRole.fromString(it) } ?: account.role
                    val assignments = membership?.assignments ?: emptyList()

                    if (role == AccountRole.ADMIN || role == AccountRole.SUPER_ADMIN) {
                        _uiStateFlow.update {
                            it.copy(
                                subjects = subjects,
                                sessions = sessions,
                                availableClasses = classes,
                                assignments = emptyList(), // Admin can see everything, usually doesn't have personal matrix
                            )
                        }
                    } else {
                        // Filter for Supervisor
                        val assignedClassIds = assignments.map { it.classId }.toSet()

                        val filteredClasses = classes.filter { it.id in assignedClassIds }

                        val filteredSessions = sessions.filter { sessionDetails ->
                            assignments.any { assignment ->
                                val classMatches = assignment.classId == sessionDetails.session.classId
                                val subjectMatches = assignment.subjectId == null || assignment.subjectId == sessionDetails.session.subjectId
                                classMatches && subjectMatches
                            }
                        }

                        // Filter subjects: Keep if Wali Kelas for ANY class, OR if specifically assigned to that subject
                        val isHomeroomForAny = assignments.any { it.subjectId == null }
                        val assignedSubjectIds = assignments.mapNotNull { it.subjectId }.toSet()

                        val filteredSubjects = if (isHomeroomForAny) {
                            subjects // Can see all subjects if Wali Kelas for at least one class
                        } else {
                            subjects.filter { it.subjectId in assignedSubjectIds }
                        }

                        _uiStateFlow.update {
                            it.copy(
                                subjects = filteredSubjects,
                                sessions = filteredSessions,
                                availableClasses = filteredClasses,
                                assignments = assignments,
                            )
                        }
                    }
                }
            }
        }
    }

    private data class DataContainer(
        val subjectsResult: Result<List<SubjectEntity>>,
        val sessionsResult: Result<List<com.azuratech.azuratime.features.session.data.local.SessionWithDetails>>,
        val classesResult: Result<List<com.azuratech.azuraengine.model.ClassModel>>,
        val accountResult: Result<Account>,
        val schoolId: String,
    )

    fun onEvent(event: SessionManagementUiEvent) {
        when (event) {
            is SessionManagementUiEvent.AddSubject -> addSubject(event.name, event.description)
            is SessionManagementUiEvent.DeleteSubject -> deleteSubject(event.subject)
            is SessionManagementUiEvent.SelectTier -> _uiStateFlow.update { it.copy(selectedTier = event.tier) }
            is SessionManagementUiEvent.AddSession -> addSession(event)
            is SessionManagementUiEvent.DeleteSession -> deleteSession(event.session)
            SessionManagementUiEvent.GenerateFromMatrix -> generateFromMatrix()
        }
    }

    private fun generateFromMatrix() {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        val supervisorEmail = sessionManager.getAccountEmail()
        val assignments = _uiStateFlow.value.assignments

        if (assignments.isEmpty()) {
            viewModelScope.launch { _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("No assignments found to generate sessions.")) }
            return
        }

        viewModelScope.launch {
            var successCount = 0
            for (assignment in assignments) {
                // Default: create one session for Monday 08:00 - 09:30 as a placeholder
                val result = createSessionUseCase(
                    classId = assignment.classId,
                    subjectId = assignment.subjectId,
                    sessionType = if (assignment.subjectId != null) SessionType.ACADEMIC else SessionType.CLASS_WIDE,
                    supervisorEmail = supervisorEmail,
                    dayOfWeek = 1, // Monday
                    startTime = "08:00",
                    endTime = "09:30",
                    schoolId = schoolId,
                )
                if (result is Result.Success) {
                    successCount++
                }
            }
            _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Generated $successCount sessions from matrix."))
        }
    }

    private fun addSubject(name: String, description: String?) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        val subject = SubjectEntity(
            subjectId = UUID.randomUUID().toString(),
            name = name,
            description = description,
            schoolId = schoolId,
        )
        viewModelScope.launch {
            val result = sessionRepository.saveSubject(subject)
            if (result is Result.Success) {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Subject added"))
            }
        }
    }

    private fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            val result = sessionRepository.deleteSubject(subject)
            when (result) {
                is Result.Success -> _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Subject deleted"))
                is Result.Failure -> _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast(result.error.message ?: "Error deleting subject"))
                else -> {}
            }
        }
    }

    private fun addSession(event: SessionManagementUiEvent.AddSession) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        val supervisorEmail = sessionManager.getAccountEmail()

        viewModelScope.launch {
            val result = createSessionUseCase(
                classId = event.classId,
                subjectId = event.subjectId,
                sessionType = event.sessionType,
                supervisorEmail = supervisorEmail,
                dayOfWeek = event.dayOfWeek,
                startTime = event.startTime,
                endTime = event.endTime,
                schoolId = schoolId,
            )

            when (result) {
                is Result.Success -> {
                    _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Session added"))
                }
                is Result.Failure -> {
                    _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Conflict: ${result.error.message ?: "Unknown error"}"))
                }
                else -> {}
            }
        }
    }

    private fun deleteSession(session: com.azuratech.azuratime.features.session.data.local.SessionWithDetails) {
        viewModelScope.launch {
            val result = sessionRepository.deleteSession(session.session)
            if (result is Result.Success) {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Session removed"))
            }
        }
    }
}
