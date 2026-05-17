package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.reporting.data.repo.DataIntegrityRepository
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

@HiltViewModel
class DataIntegrityViewModel @Inject constructor(
    private val repository: DataIntegrityRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DataIntegrityUiState> = combine(
        repository.totalStudentsFlow,
        repository.totalRecordsFlow,
        repository.missingAssignmentFlow,
        repository.brokenAssignmentsFlow,
        repository.globalUnsyncedCountFlow,
        repository.conflictsFlow.map { entities -> entities.map { it.toDomain() } },
        _isLoading,
        _error,
    ) { params: Array<Any?> ->
        DataIntegrityUiState(
            totalStudents = params[0] as Int,
            totalRecords = params[1] as Int,
            missingAssignments = params[2] as Int,
            brokenAssignments = params[3] as Int,
            unsyncedCount = params[4] as Int,
            conflicts = params[5] as List<com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict>,
            isLoading = params[6] as Boolean,
            error = params[7] as? String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataIntegrityUiState())

    fun onEvent(event: DataIntegrityUiEvent) {
        when (event) {
            is DataIntegrityUiEvent.ResolveConflict -> resolveConflict(event.conflictId, event.useCloud)
            DataIntegrityUiEvent.RefreshIntegrity -> { /* Flows react automatically */ }
            DataIntegrityUiEvent.ClearError -> _error.value = null
            is DataIntegrityUiEvent.ViewIncompleteProfiles -> { /* Managed by screen navigation */ }
        }
    }

    private fun resolveConflict(conflictId: String, useCloud: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = attendanceRepository.resolveConflict(conflictId, useCloud)) {
                is Result.Success -> {
                    _isLoading.value = false
                    _uiEvent.emit(UiEvent.ShowSnackbar("Konflik berhasil diselesaikan"))
                }
                is Result.Failure -> {
                    _isLoading.value = false
                    _error.value = result.error.message
                    _uiEvent.emit(UiEvent.ShowSnackbar("Gagal: ${result.error.message}"))
                }
                is Result.Loading -> {}
            }
        }
    }
}
