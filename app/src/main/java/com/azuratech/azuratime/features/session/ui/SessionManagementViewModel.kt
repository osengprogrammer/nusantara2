package com.azuratech.azuratime.features.session.ui
import com.azuratech.azuratime.core.domain.model.toSubjectTemplate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.SubjectEntity
import com.azuratech.azuratime.features.session.domain.usecase.CreateSessionUseCase
import com.azuratech.azuratime.features.session.domain.usecase.UpdateSessionUseCase
import com.azuratech.azuratime.features.session.domain.usecase.SaveSubjectUseCase
import com.azuratech.azuratime.features.session.domain.usecase.DeleteSubjectUseCase
import com.azuratech.azuratime.features.session.domain.usecase.DeleteSessionUseCase
import com.azuratech.azuratime.features.session.domain.usecase.FetchGlobalSubjectsUseCase
import com.azuratech.azuratime.features.session.domain.usecase.ObserveSessionManagementDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

import com.azuratech.azuratime.features.session.domain.model.SessionType
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.core.domain.model.SubjectTemplate

@HiltViewModel
class SessionManagementViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val createSessionUseCase: CreateSessionUseCase,
    private val updateSessionUseCase: UpdateSessionUseCase,
    private val saveSubjectUseCase: SaveSubjectUseCase,
    private val deleteSubjectUseCase: DeleteSubjectUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val fetchGlobalSubjectsUseCase: FetchGlobalSubjectsUseCase,
    private val observeSessionManagementDataUseCase: ObserveSessionManagementDataUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(SessionManagementUiState())
    val uiStateFlow: StateFlow<SessionManagementUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<SessionManagementUiEffect>()
    val uiEffectFlow: SharedFlow<SessionManagementUiEffect> = _uiEffectFlow.asSharedFlow()

    init {
        observeData()
        loadAvailableSubjectsFromTemplate()
    }

    private fun loadAvailableSubjectsFromTemplate() {
        viewModelScope.launch {
            fetchGlobalSubjectsUseCase()
                .onSuccess { globalSubjects ->
                    val distinctSubjects = globalSubjects.distinctBy { it.name }.sortedBy { it.name }
                    _uiStateFlow.update { it.copy(availableSubjects = distinctSubjects) }
                }
                .onFailure {
                    val fallback = listOf(
                        SubjectTemplate(id = "fb_math", name = "Matematika", category = "MIPA"),
                        SubjectTemplate(id = "fb_ind", name = "Bahasa Indonesia", category = "Bahasa"),
                        SubjectTemplate(id = "fb_eng", name = "Bahasa Inggris", category = "Bahasa"),
                        SubjectTemplate(id = "fb_phy", name = "Fisika", category = "MIPA"),
                        SubjectTemplate(id = "fb_chem", name = "Kimia", category = "MIPA"),
                        SubjectTemplate(id = "fb_bio", name = "Biologi", category = "MIPA"),
                        SubjectTemplate(id = "fb_hist", name = "Sejarah", category = "IPS"),
                        SubjectTemplate(id = "fb_geo", name = "Geografi", category = "IPS"),
                        SubjectTemplate(id = "fb_soc", name = "Sosiologi", category = "IPS"),
                        SubjectTemplate(id = "fb_econ", name = "Ekonomi", category = "IPS"),
                        SubjectTemplate(id = "fb_civ", name = "PPKn", category = "Umum"),
                        SubjectTemplate(id = "fb_art", name = "Seni Budaya", category = "Umum"),
                        SubjectTemplate(id = "fb_pe", name = "PJOK", category = "Umum"),
                        SubjectTemplate(id = "fb_rel", name = "Pendidikan Agama", category = "Umum"),
                    )
                    _uiStateFlow.update { it.copy(availableSubjects = fallback) }
                }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            observeSessionManagementDataUseCase(
                schoolIdFlow = sessionManager.activeSchoolIdFlow,
                accountIdFlow = sessionManager.currentAccountIdFlow,
            ).collect { data ->
                val subjects = data.subjectsResult.getOrNull() ?: emptyList()
                val sessions = data.sessionsResult.getOrNull() ?: emptyList()
                val classes = data.classesResult.getOrNull() ?: emptyList()
                val account = data.accountResult.getOrNull()

                // Populate class names
                sessions.forEach { sessionDetails ->
                    sessionDetails.className = classes.find { it.id == sessionDetails.classId }?.name
                }

                if (account != null) {
                    val membership = account.memberships[data.schoolId]
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
                                val classMatches = assignment.classId == sessionDetails.classId
                                val subjectMatches = assignment.subjectId == null || assignment.subjectId == sessionDetails.subjectId
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

    fun onEvent(event: SessionManagementUiEvent) {
        when (event) {
            is SessionManagementUiEvent.AddSubject -> addSubject(event.name, event.description)
            is SessionManagementUiEvent.UpdateSubject -> updateSubject(event.subject, event.newName, event.newDescription)
            is SessionManagementUiEvent.DeleteSubject -> deleteSubject(event.subject)
            is SessionManagementUiEvent.SelectTier -> _uiStateFlow.update { it.copy(selectedTier = event.tier) }
            is SessionManagementUiEvent.AddSession -> addSession(event)
            is SessionManagementUiEvent.UpdateSession -> updateSession(event)
            is SessionManagementUiEvent.DeleteSession -> deleteSession(event.session)
            SessionManagementUiEvent.GenerateFromMatrix -> generateFromMatrix()
        }
    }

    private fun updateSubject(subject: SubjectEntity, newName: String, newDescription: String?) {
        if (subject.isFromTemplate) {
            viewModelScope.launch {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Template subjects cannot be edited."))
            }
            return
        }

        viewModelScope.launch {
            val updated = subject.copy(name = newName, description = newDescription)
            val result = saveSubjectUseCase(updated)
            if (result is Result.Success) {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Subject updated"))
            } else if (result is Result.Failure) {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast(result.error.message ?: "Failed to update subject"))
            }
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
        // Start loading
        _uiStateFlow.update { it.copy(isLoading = true) }
        val schoolId = sessionManager.getActiveSchoolId()
        if (schoolId == null) {
            _uiStateFlow.update { it.copy(isLoading = false) }
            viewModelScope.launch { _uiEffectFlow.emit(SessionManagementUiEffect.ShowError("Pilih sekolah terlebih dahulu sebelum menambahkan mata pelajaran")) }
            return
        }
        // Generate a new UUID for the school‑specific subject ID
        val newSubjectId = UUID.randomUUID().toString()
        val matchingTemplate = _uiStateFlow.value.availableSubjects
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
        val subject = SubjectEntity(
            subjectId = newSubjectId,
            name = name,
            description = description,
            schoolId = schoolId,
            isFromTemplate = matchingTemplate != null,
        )
        viewModelScope.launch {
            // clear previous error before trying
            val result = saveSubjectUseCase(subject)
            if (result is Result.Success) {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Subject added"))
            } else if (result is Result.Failure) {
                val errorMsg = if (result.error is AppError.Conflict) {
                    "Mata pelajaran dengan nama tersebut sudah terdaftar di sekolah ini"
                } else {
                    result.error.message ?: "Failed to save subject"
                }
                viewModelScope.launch { _uiEffectFlow.emit(SessionManagementUiEffect.ShowError(errorMsg)) }
            }
            // loading finished
            _uiStateFlow.update { it.copy(isLoading = false) }
        }
    }
    private fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            val result = deleteSubjectUseCase(subject)
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

    private fun updateSession(event: SessionManagementUiEvent.UpdateSession) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        val supervisorEmail = sessionManager.getAccountEmail()

        viewModelScope.launch {
            val result = updateSessionUseCase(
                sessionId = event.sessionId,
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
                    _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Session updated"))
                }
                is Result.Failure -> {
                    _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Conflict: ${result.error.message ?: "Unknown error"}"))
                }
                else -> {}
            }
        }
    }

    private fun deleteSession(session: com.azuratech.azuratime.core.data.local.SessionWithDetails) {
        viewModelScope.launch {
            val entity = ClassSessionEntity(
                sessionId = session.sessionId,
                classId = session.classId,
                subjectId = session.subjectId,
                sessionType = session.sessionType,
                supervisorEmail = session.supervisorEmail,
                dayOfWeek = session.dayOfWeek,
                startTime = session.startTime,
                endTime = session.endTime,
                schoolId = session.schoolId,
                lookupKey = session.lookupKey,
                isActive = session.isActive,
                isSynced = session.isSynced,
            )
            val result = deleteSessionUseCase(entity)
            if (result is Result.Success) {
                _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Session removed"))
            }
        }
    }
}
