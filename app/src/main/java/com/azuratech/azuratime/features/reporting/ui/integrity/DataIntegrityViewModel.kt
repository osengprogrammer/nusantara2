package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.reporting.data.repo.DataIntegrityRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.ui.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataIntegrityViewModel @Inject constructor(
    private val repository: DataIntegrityRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val totalStudentsFlow: Flow<Int> = repository.totalStudentsFlow
    val missingAssignmentFlow: Flow<Int> = repository.missingAssignmentFlow
    val brokenAssignmentsFlow: Flow<Int> = repository.brokenAssignmentsFlow
    val unsyncedCountFlow: Flow<Int> = repository.globalUnsyncedCountFlow

    val conflictsState: StateFlow<List<AttendanceConflict>> = repository.conflictsFlow
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveConflict(conflictId: String, useCloud: Boolean) {
        viewModelScope.launch {
            val result = attendanceRepository.resolveConflict(conflictId, useCloud)
            when (result) {
                is Result.Success -> _uiEvent.emit(UiEvent.ShowSnackbar("Konflik berhasil diselesaikan"))
                is Result.Failure -> _uiEvent.emit(UiEvent.ShowSnackbar("Gagal: ${result.error.message}"))
                else -> {}
            }
        }
    }

    fun getIncompleteProfiles(type: String) = repository.getIncompleteProfiles(type)
}
