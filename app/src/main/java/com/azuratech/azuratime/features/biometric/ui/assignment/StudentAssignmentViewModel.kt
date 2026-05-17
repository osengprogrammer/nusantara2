package com.azuratech.azuratime.features.biometric.ui.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🧬 STUDENT ASSIGNMENT VIEW MODEL (v3.2.0-ai-native)
 * Unified ViewModel for managing student-class assignments.
 */
@HiltViewModel
class StudentAssignmentViewModel @Inject constructor(
    private val biometricRepository: BiometricRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(AssignmentUiState())
    val uiStateFlow: StateFlow<AssignmentUiState> = _uiStateFlow.asStateFlow()

    private val _refreshTriggerFlow = MutableStateFlow(0)

    init {
        observeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        val schoolIdFlow = sessionManager.activeSchoolIdFlow.filterNotNull()

        // 1. Observe Roster
        schoolIdFlow
            .flatMapLatest { schoolId -> biometricRepository.getStudentsWithDetailsFlow(schoolId) }
            .onEach { roster -> _uiStateFlow.update { it.copy(roster = roster) } }
            .launchIn(viewModelScope)

        // 2. Observe Available Classes
        schoolIdFlow
            .flatMapLatest { schoolId -> schoolRepository.observeClasses(schoolId) }
            .onEach { result ->
                result.onSuccess { classes ->
                    _uiStateFlow.update { it.copy(availableClasses = classes) }
                }
            }
            .launchIn(viewModelScope)

        // 3. Observe Assigned Classes Map
        combine(
            schoolIdFlow,
            _uiStateFlow.map { it.availableClasses }.distinctUntilChanged(),
            _refreshTriggerFlow,
        ) { schoolId, availableClasses, _ ->
            schoolId to availableClasses
        }.flatMapLatest { (schoolId, availableClasses) ->
            val classMap = availableClasses.associateBy { it.id }
            biometricRepository.getAllAssignmentsFlow(schoolId).map { assignments ->
                assignments.groupBy { it.studentId }
                    .mapValues { entry -> entry.value.mapNotNull { classMap[it.classId] } }
            }
        }.onEach { assignedMap ->
            _uiStateFlow.update { it.copy(assignedClasses = assignedMap) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AssignmentUiEvent) {
        when (event) {
            AssignmentUiEvent.LoadAssignment -> _refreshTriggerFlow.value++
            is AssignmentUiEvent.AssignStudent -> assignToClass(event.studentId, event.classId)
            is AssignmentUiEvent.RemoveAssignment -> removeSpecificAssignment(event.studentId, event.classId)
            is AssignmentUiEvent.RemoveAllAssignments -> removeAllAssignmentsForStudent(event.studentId)
            is AssignmentUiEvent.SelectStudent -> _uiStateFlow.update { it.copy(selectedStudentId = event.studentId) }
            AssignmentUiEvent.Refresh -> _refreshTriggerFlow.value++
            AssignmentUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun assignToClass(studentId: String, classId: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            biometricRepository.assignStudentToClass(studentId, classId)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _refreshTriggerFlow.value++
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun removeSpecificAssignment(studentId: String, classId: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            biometricRepository.removeStudentFromClass(studentId, classId)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _refreshTriggerFlow.value++
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun removeAllAssignmentsForStudent(studentId: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            biometricRepository.removeAllAssignmentsForStudent(studentId)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _refreshTriggerFlow.value++
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
