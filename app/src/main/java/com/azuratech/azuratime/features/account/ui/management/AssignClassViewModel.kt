package com.azuratech.azuratime.features.account.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.domain.usecase.AssignClassToSupervisorUseCase
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 ASSIGN CLASS VIEW MODEL (v3.2.0-ai-native)
 * ViewModel for assigning classes to a supervisor.
 */
@HiltViewModel
class AssignClassViewModel @Inject constructor(
    private val assignClassUseCase: AssignClassToSupervisorUseCase,
    private val schoolRepository: SchoolRepository,
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
            is AssignClassUiEvent.ToggleClassSelection -> toggleSelection(event.classId)
            is AssignClassUiEvent.SaveAssignments -> saveAssignments()
            is AssignClassUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun loadData(targetAccountId: String) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        _uiStateFlow.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // 1. Get Target Account
            val account = database.accountDao().getAccountById(targetAccountId)

            // 2. Get Available Classes
            schoolRepository.observeClasses(schoolId).first().onSuccess { classes ->
                // 3. Get Currently Assigned Classes (from Local for immediate UI, but Remote is Source of Truth)
                val assignedIds = database.accountClassAccessDao().getAssignedClassIds(targetAccountId, schoolId).first()

                _uiStateFlow.update {
                    it.copy(
                        isLoading = false,
                        targetAccount = account,
                        availableClasses = classes,
                        selectedClassIds = assignedIds,
                    )
                }
            }.onFailure { error ->
                _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    private fun toggleSelection(classId: String) {
        _uiStateFlow.update { state ->
            val currentSelected = state.selectedClassIds.toMutableList()
            if (currentSelected.contains(classId)) {
                currentSelected.remove(classId)
            } else {
                currentSelected.add(classId)
            }
            state.copy(selectedClassIds = currentSelected)
        }
    }

    private fun saveAssignments() {
        val state = _uiStateFlow.value
        val targetId = state.targetAccount?.accountId ?: return
        val schoolId = sessionManager.getActiveSchoolId() ?: return

        _uiStateFlow.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            assignClassUseCase(targetId, schoolId, state.selectedClassIds)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isSaving = false) }
                    _uiEffectFlow.emit(AssignClassUiEffect.ShowSnackbar("Penugasan kelas berhasil disimpan"))
                    _uiEffectFlow.emit(AssignClassUiEffect.NavigateBack)
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isSaving = false, error = error.message) }
                }
        }
    }
}
