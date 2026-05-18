package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.reporting.domain.repository.DataIntegrityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val _isLoadingFlow = MutableStateFlow(false)
    private val _errorFlow = MutableStateFlow<String?>(null)

    val uiStateFlow: StateFlow<DataIntegrityUiState> = combine(
        repository.totalStudentsFlow,
        repository.totalRecordsFlow,
        repository.missingAssignmentFlow,
        repository.brokenAssignmentsFlow,
        repository.globalUnsyncedCountFlow,
        repository.conflictsFlow.map { result ->
            result.getOrNull()?.map { it.toDomain() } ?: emptyList()
        },
        _isLoadingFlow,
        _errorFlow,
    ) { params: Array<Any?> ->
        val totalStudentsResult = params[0] as Result<Int>
        val totalRecordsResult = params[1] as Result<Int>
        val missingAssignmentResult = params[2] as Result<Int>
        val brokenAssignmentsResult = params[3] as Result<Int>
        val globalUnsyncedCountResult = params[4] as Result<Int>

        DataIntegrityUiState(
            totalStudents = totalStudentsResult.getOrNull() ?: 0,
            totalRecords = totalRecordsResult.getOrNull() ?: 0,
            missingAssignments = missingAssignmentResult.getOrNull() ?: 0,
            brokenAssignments = brokenAssignmentsResult.getOrNull() ?: 0,
            unsyncedCount = globalUnsyncedCountResult.getOrNull() ?: 0,
            conflicts = params[5] as List<com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict>,
            isLoading = params[6] as Boolean,
            error = params[7] as? String,
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
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Konflik berhasil diselesaikan"))
                }
                is Result.Failure -> {
                    _isLoadingFlow.value = false
                    _errorFlow.value = result.error.message
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Gagal: ${result.error.message}"))
                }
                is Result.Loading -> {}
            }
        }
    }
}
