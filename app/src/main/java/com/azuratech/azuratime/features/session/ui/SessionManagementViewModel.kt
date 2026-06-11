package com.azuratech.azuratime.features.session.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.CreateSessionUseCase
import com.azuratech.azuratime.features.session.SessionRepository
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SessionManagementViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val schoolRepository: SchoolRepository,
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

    private fun observeData() {
        val schoolId = sessionManager.getActiveSchoolId() ?: return

        viewModelScope.launch {
            combine(
                sessionRepository.observeAllSubjectsFlow(schoolId),
                sessionRepository.observeAllSessionsFlow(schoolId),
                schoolRepository.observeClassesFlow(schoolId),
            ) { subjectsResult, sessionsResult, classesResult ->
                _uiStateFlow.update {
                    it.copy(
                        subjects = subjectsResult.getOrNull() ?: emptyList(),
                        sessions = sessionsResult.getOrNull() ?: emptyList(),
                        availableClasses = classesResult.getOrNull() ?: emptyList(),
                    )
                }
            }.collect()
        }
    }

    fun onEvent(event: SessionManagementUiEvent) {
        when (event) {
            is SessionManagementUiEvent.AddSubject -> addSubject(event.name, event.description)
            is SessionManagementUiEvent.DeleteSubject -> deleteSubject(event.subject)
            is SessionManagementUiEvent.AddSession -> addSession(event)
            is SessionManagementUiEvent.DeleteSession -> deleteSession(event.session)
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
            sessionRepository.saveSubject(subject).onSuccess {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Subject added"))
            }
        }
    }

    private fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            sessionRepository.deleteSubject(subject)
                .onSuccess { _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Subject deleted")) }
                .onFailure { error -> _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast(error.message ?: "Error deleting subject")) }
        }
    }

    private fun addSession(event: SessionManagementUiEvent.AddSession) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        val supervisorEmail = sessionManager.getAccountEmail()

        viewModelScope.launch {
            createSessionUseCase(
                classId = event.classId,
                subjectId = event.subjectId,
                supervisorEmail = supervisorEmail,
                dayOfWeek = event.dayOfWeek,
                startTime = event.startTime,
                endTime = event.endTime,
                schoolId = schoolId,
            ).onSuccess {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Session added"))
            }.onFailure { error ->
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Conflict: ${error.message ?: "Unknown error"}"))
            }
        }
    }

    private fun deleteSession(session: com.azuratech.azuratime.features.session.data.local.SessionWithDetails) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session.session)
                .onSuccess { _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Session removed")) }
        }
    }
}
