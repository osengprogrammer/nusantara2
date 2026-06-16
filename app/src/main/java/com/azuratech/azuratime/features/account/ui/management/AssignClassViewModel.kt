package com.azuratech.azuratime.features.account.ui.management

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result as AzuraResult
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import com.azuratech.azuratime.features.account.domain.usecase.AssignClassToSupervisorUseCase
import com.azuratech.azuratime.features.session.SessionRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 ASSIGN CLASS VIEW MODEL (v3.4.0-matrix)
 * ViewModel for assigning classes and subjects to a supervisor.
 * Refactored for robust reactive search and decoupled class/subject selection.
 */
@HiltViewModel
class AssignClassViewModel @Inject constructor(
    private val assignClassUseCase: AssignClassToSupervisorUseCase,
    private val schoolRepository: SchoolRepository,
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager,
    private val database: AppDatabase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(AssignClassUiState())
    val uiStateFlow: StateFlow<AssignClassUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<AssignClassUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    fun onEvent(event: AssignClassUiEvent) {
        when (event) {
            is AssignClassUiEvent.LoadInitialData -> loadData(event.targetAccountId)
            is AssignClassUiEvent.ToggleClass -> toggleClass(event.classId)
            is AssignClassUiEvent.ToggleAssignment -> toggleAssignment(event.classId, event.subjectId)
            is AssignClassUiEvent.UpdateSearchQuery -> updateSearch(event.query)
            AssignClassUiEvent.SelectAllFiltered -> selectAllFiltered()
            AssignClassUiEvent.ClearAllSelections -> clearAll()
            is AssignClassUiEvent.SaveAssignments -> saveAssignments()
            is AssignClassUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private val _searchQueryFlow = MutableStateFlow("")
    private var isInitialDataLoaded = false

    private fun toggleClass(classId: String) {
        _uiStateFlow.update { state ->
            val currentClasses = state.selectedClassIds.toMutableSet()
            val currentAssignments = state.selectedAssignments.toMutableList()

            if (currentClasses.contains(classId)) {
                currentClasses.remove(classId)
                // 🔥 AI Native Cleanup: Remove all assignments for this class when class is deselected
                currentAssignments.removeAll { it.classId == classId }
            } else {
                currentClasses.add(classId)
                // Don't auto-assign anything, let user choose in Step 2
            }
            state.copy(selectedClassIds = currentClasses, selectedAssignments = currentAssignments)
        }
    }

    private fun toggleAssignment(classId: String, subjectId: String?) {
        _uiStateFlow.update { state ->
            val currentSelected = state.selectedAssignments.toMutableList()
            val exists = currentSelected.any { it.classId == classId && it.subjectId == subjectId }

            if (exists) {
                currentSelected.removeAll { it.classId == classId && it.subjectId == subjectId }
            } else {
                currentSelected.add(TeacherAssignment(classId, subjectId))
            }
            state.copy(selectedAssignments = currentSelected)
        }
    }

    private fun updateSearch(query: String) {
        _searchQueryFlow.value = query
    }

    private fun selectAllFiltered() {
        _uiStateFlow.update { state ->
            val currentClasses = state.selectedClassIds.toMutableSet()
            state.filteredClasses.forEach { currentClasses.add(it.id) }
            state.copy(selectedClassIds = currentClasses)
        }
    }

    private fun clearAll() {
        _uiStateFlow.update { it.copy(selectedClassIds = emptySet(), selectedAssignments = emptyList()) }
    }

    private fun loadData(targetAccountId: String) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        _uiStateFlow.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                Log.d("AZURA_DEBUG", "🚀 Initializing Matrix Setup for: $targetAccountId")
                val account = database.accountDao().getAccountById(targetAccountId)
                if (account == null) {
                    _uiStateFlow.update { it.copy(isLoading = false, error = "Account not found") }
                    return@launch
                }

                // 🔥 AI Native: Reactive Data Pipeline
                combine(
                    schoolRepository.observeClassesFlow(schoolId),
                    sessionRepository.observeAllSubjectsFlow(schoolId),
                    database.accountClassAccessDao().getAssignmentsFlow(targetAccountId, schoolId),
                    _searchQueryFlow,
                ) { classesRes, subjectsRes, assignments, query ->
                    DataSnapshot(classesRes, subjectsRes, assignments, query)
                }.collect { snapshot ->
                    val classesResult = snapshot.classesRes
                    val subjectsResult = snapshot.subjectsRes
                    val assignments = snapshot.assignments
                    val query = snapshot.query

                    if (classesResult is AzuraResult.Success && subjectsResult is AzuraResult.Success) {
                        val classes = classesResult.data
                        val subjects = subjectsResult.data

                        _uiStateFlow.update { state ->
                            val currentAssignments = if (!isInitialDataLoaded) {
                                assignments.map { tuple ->
                                    TeacherAssignment(tuple.classId, tuple.subjectId.takeIf { s -> s.isNotEmpty() })
                                }
                            } else {
                                state.selectedAssignments
                            }

                            val currentSelectedClasses = if (!isInitialDataLoaded) {
                                currentAssignments.map { it.classId }.toSet()
                            } else {
                                state.selectedClassIds
                            }

                            if (!isInitialDataLoaded && (classes.isNotEmpty() || assignments.isNotEmpty())) {
                                isInitialDataLoaded = true
                            }

                            state.copy(
                                isLoading = false,
                                targetAccount = account,
                                availableClasses = classes,
                                filteredClasses = if (query.isBlank()) classes else classes.filter { it.name.contains(query, ignoreCase = true) },
                                availableSubjects = subjects,
                                selectedClassIds = currentSelectedClasses,
                                selectedAssignments = currentAssignments,
                                searchQuery = query,
                            )
                        }
                    } else {
                        val error = (classesResult as? AzuraResult.Failure)?.error?.message
                            ?: (subjectsResult as? AzuraResult.Failure)?.error?.message
                        if (error != null) {
                            _uiStateFlow.update { it.copy(isLoading = false, error = error) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AZURA_DEBUG", "❌ Matrix Load Error: ${e.message}")
                _uiStateFlow.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private data class DataSnapshot(
        val classesRes: AzuraResult<List<com.azuratech.azuraengine.model.ClassModel>>,
        val subjectsRes: AzuraResult<List<com.azuratech.azuratime.features.session.data.local.SubjectEntity>>,
        val assignments: List<com.azuratech.azuratime.features.account.data.local.TeacherAssignmentTuple>,
        val query: String,
    )

    private fun saveAssignments() {
        val state = _uiStateFlow.value
        val targetId = state.targetAccount?.accountId ?: return
        val schoolId = sessionManager.getActiveSchoolId() ?: return

        _uiStateFlow.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                Log.d("AZURA_DEBUG", "Saving matrix assignments for $targetId")
                assignClassUseCase(targetId, schoolId, state.selectedAssignments)
                    .onSuccess {
                        Log.d("AZURA_DEBUG", "Matrix assignments saved successfully")
                        _uiStateFlow.update { it.copy(isSaving = false) }
                        _uiEffectFlow.emit(AssignClassUiEffect.ShowSnackbar("Class matrix assignment saved successfully"))
                        _uiEffectFlow.emit(AssignClassUiEffect.NavigateBack)
                    }
                    .onFailure { error ->
                        Log.e("AZURA_DEBUG", "Failed to save matrix assignments: ${error.message}")
                        _uiStateFlow.update { it.copy(isSaving = false, error = error.message) }
                    }
            } catch (e: Exception) {
                Log.e("AZURA_DEBUG", "Unexpected error in saveAssignments: ${e.message}", e)
                _uiStateFlow.update { it.copy(isSaving = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }
}
