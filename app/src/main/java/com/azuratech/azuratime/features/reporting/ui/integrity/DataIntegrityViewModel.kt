package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.data.local.AttendanceConflictEntity
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.reporting.domain.repository.DataIntegrityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 DATA INTEGRITY VIEW MODEL (v3.2.0-ai-native)
 * Central hub for monitoring system health and resolving data collisions.
 */
@HiltViewModel
class DataIntegrityViewModel @Inject constructor(
    private val repository: DataIntegrityRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _uiEffectFlow = MutableSharedFlow<DataIntegrityUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _isLoadingFlow = MutableStateFlow(false)
    private val _errorFlow = MutableStateFlow<String?>(null)

    val uiStateFlow: StateFlow<DataIntegrityUiState> = combine(
        repository.totalStudentsFlow,
        repository.totalRecordsFlow,
        repository.missingAssignmentFlow,
        repository.brokenAssignmentsFlow,
        repository.globalUnsyncedCountFlow,
        repository.conflictsFlow,
        _isLoadingFlow,
        _errorFlow,
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val totalStudents = params[0] as Result<Int>

        @Suppress("UNCHECKED_CAST")
        val totalRecords = params[1] as Result<Int>

        @Suppress("UNCHECKED_CAST")
        val missing = params[2] as Result<Int>

        @Suppress("UNCHECKED_CAST")
        val broken = params[3] as Result<Int>

        @Suppress("UNCHECKED_CAST")
        val unsynced = params[4] as Result<Int>

        @Suppress("UNCHECKED_CAST")
        val conflicts = params[5] as Result<List<AttendanceConflictEntity>>
        val isLoading = params[6] as Boolean
        val error = params[7] as? String

        DataIntegrityUiState(
            totalStudents = totalStudents.getOrNull() ?: 0,
            totalRecords = totalRecords.getOrNull() ?: 0,
            missingAssignments = missing.getOrNull() ?: 0,
            brokenAssignments = broken.getOrNull() ?: 0,
            unsyncedCount = unsynced.getOrNull() ?: 0,
            conflicts = conflicts.getOrNull()?.map { it.toDomain() } ?: emptyList(),
            isLoading = isLoading,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataIntegrityUiState())

    fun onEvent(event: DataIntegrityUiEvent) {
        when (event) {
            is DataIntegrityUiEvent.ResolveConflict -> resolveConflict(event.conflictId, event.useCloud)
            DataIntegrityUiEvent.RefreshIntegrity -> { /* Flows react automatically */ }
            DataIntegrityUiEvent.ClearError -> _errorFlow.value = null
            is DataIntegrityUiEvent.ViewIncompleteProfiles -> { /* Managed by screen navigation */ }
        }
    }

    private fun resolveConflict(conflictId: String, useCloud: Boolean) {
        viewModelScope.launch {
            _isLoadingFlow.value = true
            when (val result = attendanceRepository.resolveConflict(conflictId, useCloud)) {
                is Result.Success -> {
                    _isLoadingFlow.value = false
                    _uiEffectFlow.emit(DataIntegrityUiEffect.ShowSnackbar("Conflict resolved successfully"))
                }
                is Result.Failure -> {
                    _isLoadingFlow.value = false
                    _errorFlow.value = result.error.message
                    _uiEffectFlow.emit(DataIntegrityUiEffect.ShowSnackbar("Failed: ${result.error.message}"))
                }
                is Result.Loading -> {}
                Result.Network -> {}
            }
        }
    }
}
