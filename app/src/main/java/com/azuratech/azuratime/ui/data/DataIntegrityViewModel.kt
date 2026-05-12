package com.azuratech.azuratime.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.repo.DataIntegrityRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.attendance.domain.repository.CheckInRepository
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataIntegrityViewModel @Inject constructor(
    private val repository: DataIntegrityRepository,
    private val checkInRepository: CheckInRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val totalFaces: Flow<Int> = repository.totalFaces
    val missingAssignment: Flow<Int> = repository.missingAssignment
    val brokenAssignments: Flow<Int> = repository.brokenAssignments
    val unsyncedCount: Flow<Int> = repository.globalUnsyncedCount

    val conflicts: StateFlow<List<AttendanceConflict>> = repository.conflicts
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveConflict(conflictId: String, useCloud: Boolean) {
        viewModelScope.launch {
            val result = checkInRepository.resolveConflict(conflictId, useCloud)
            when (result) {
                is Result.Success -> _uiEvent.emit(UiEvent.ShowSnackbar("Konflik berhasil diselesaikan"))
                is Result.Failure -> _uiEvent.emit(UiEvent.ShowSnackbar("Gagal: ${result.error.message}"))
                else -> {}
            }
        }
    }

    fun getIncompleteProfiles(type: String) = repository.getIncompleteProfiles(type)
}
