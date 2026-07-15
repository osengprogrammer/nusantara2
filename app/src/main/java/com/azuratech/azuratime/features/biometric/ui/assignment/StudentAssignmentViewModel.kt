package com.azuratech.azuratime.features.biometric.ui.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.map
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.biometric.domain.usecase.ObserveStudentsWithDetailsUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.ObserveClassesForSchoolUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.ObserveAssignmentsUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.AssignStudentToClassUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.RemoveStudentFromClassUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.RemoveAllAssignmentsForStudentUseCase
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
    private val observeStudentsWithDetailsUseCase: ObserveStudentsWithDetailsUseCase,
    private val observeClassesForSchoolUseCase: ObserveClassesForSchoolUseCase,
    private val observeAssignmentsUseCase: ObserveAssignmentsUseCase,
    private val assignStudentToClassUseCase: AssignStudentToClassUseCase,
    private val removeStudentFromClassUseCase: RemoveStudentFromClassUseCase,
    private val removeAllAssignmentsForStudentUseCase: RemoveAllAssignmentsForStudentUseCase,
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
            .flatMapLatest { schoolId -> observeStudentsWithDetailsUseCase(schoolId) }
            .onEach { result ->
                result.onSuccess { roster ->
                    _uiStateFlow.update { it.copy(roster = roster) }
                }
            }
            .launchIn(viewModelScope)

        // 2. Observe Available Classes
        schoolIdFlow
            .flatMapLatest { schoolId -> observeClassesForSchoolUseCase(schoolId) }
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
            observeAssignmentsUseCase(schoolId).map { result ->
                result.map { assignments ->
                    assignments.groupBy { it.studentId }
                        .mapValues { entry -> entry.value.mapNotNull { classMap[it.classId] } }
                }
            }
        }.onEach { result ->
            result.onSuccess { assignedMap ->
                _uiStateFlow.update { it.copy(assignedClasses = assignedMap) }
            }
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
            assignStudentToClassUseCase(studentId, classId)
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
            removeStudentFromClassUseCase(studentId, classId)
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
            removeAllAssignmentsForStudentUseCase(studentId)
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
