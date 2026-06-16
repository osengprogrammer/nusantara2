package com.azuratech.azuratime.features.account.ui.management

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            is AssignClassUiEvent.ToggleClassSelection -> toggleSelection(event.classId, event.subjectId)
            is AssignClassUiEvent.UpdateSearchQuery -> updateSearch(event.query)
            AssignClassUiEvent.SelectAllFiltered -> selectAllFiltered()
            AssignClassUiEvent.ClearAllSelections -> clearAll()
            is AssignClassUiEvent.SaveAssignments -> saveAssignments()
            is AssignClassUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun updateSearch(query: String) {
        _uiStateFlow.update { state ->
            val filtered = if (query.isBlank()) {
                state.availableClasses
            } else {
                state.availableClasses.filter { it.name.contains(query, ignoreCase = true) }
            }
            state.copy(searchQuery = query, filteredClasses = filtered)
        }
    }

    private fun selectAllFiltered() {
        _uiStateFlow.update { state ->
            val currentSelected = state.selectedAssignments.toMutableList()
            state.filteredClasses.forEach { classModel ->
                if (currentSelected.none { it.classId == classModel.id }) {
                    currentSelected.add(TeacherAssignment(classModel.id, null))
                }
            }
            state.copy(selectedAssignments = currentSelected)
        }
    }

    private fun clearAll() {
        _uiStateFlow.update { it.copy(selectedAssignments = emptyList()) }
    }

    private var isInitialDataLoaded = false

    private fun loadData(targetAccountId: String) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        _uiStateFlow.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                Log.d("AZURA_DEBUG", "Loading matrix data for account: $targetAccountId")
                // 1. Get Target Account
                val account = database.accountDao().getAccountById(targetAccountId)

                if (account == null) {
                    Log.e("AZURA_DEBUG", "Account not found: $targetAccountId")
                    _uiStateFlow.update { it.copy(isLoading = false, error = "Account not found") }
                    return@launch
                }

                // 2. Reactive Data Stream
                combine(
                    schoolRepository.observeClassesFlow(schoolId),
                    sessionRepository.observeAllSubjectsFlow(schoolId),
                    database.accountClassAccessDao().getAssignmentsFlow(targetAccountId, schoolId),
                ) { classesRes, subjectsRes, assignments ->
                    Triple(classesRes, subjectsRes, assignments)
                }.collect { (classesRes, subjectsRes, assignments) ->
                    classesRes.onSuccess { classes ->
                        subjectsRes.onSuccess { subjects ->
                            _uiStateFlow.update { state ->
                                val updatedAssignments = if (!isInitialDataLoaded) {
                                    assignments.map { tuple ->
                                        TeacherAssignment(tuple.classId, tuple.subjectId.takeIf { s -> s != null && s.isNotEmpty() })
                                    }
                                } else {
                                    state.selectedAssignments
                                }

                                if (!isInitialDataLoaded && updatedAssignments.isNotEmpty()) {
                                    isInitialDataLoaded = true
                                } else if (!isInitialDataLoaded && classes.isNotEmpty()) {
                                    // Even if assignments are empty, if we have classes, we consider initial load done
                                    isInitialDataLoaded = true
                                }

                                state.copy(
                                    isLoading = false,
                                    targetAccount = account,
                                    availableClasses = classes,
                                    filteredClasses = if (state.searchQuery.isBlank()) classes else classes.filter { it.name.contains(state.searchQuery, ignoreCase = true) },
                                    availableSubjects = subjects,
                                    selectedAssignments = updatedAssignments,
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AZURA_DEBUG", "Unexpected error in loadData: ${e.message}", e)
                _uiStateFlow.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    private fun toggleSelection(classId: String, subjectId: String?) {
        _uiStateFlow.update { state ->
            val currentSelected = state.selectedAssignments.toMutableList()
            val assignment = TeacherAssignment(classId, subjectId)

            if (currentSelected.any { it.classId == classId && it.subjectId == subjectId }) {
                currentSelected.removeAll { it.classId == classId && it.subjectId == subjectId }
            } else {
                currentSelected.add(assignment)
            }
            state.copy(selectedAssignments = currentSelected)
        }
    }

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
