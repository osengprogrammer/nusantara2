package com.azuratech.azuratime.features.attendance.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 ATTENDANCE HISTORY VIEW MODEL (v3.2.0-ai-native)
 * Manages historical attendance records.
 */
@HiltViewModel
class AttendanceHistoryViewModel @Inject constructor(
    private val repository: AttendanceRepository,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(AttendanceHistoryUiState())
    val uiStateFlow: StateFlow<AttendanceHistoryUiState> = _uiStateFlow.asStateFlow()

    fun onEvent(event: AttendanceHistoryUiEvent) {
        when (event) {
            is AttendanceHistoryUiEvent.LoadHistory -> loadHistory(event.studentId)
            is AttendanceHistoryUiEvent.FilterByDate -> updateDateFilter(event.date)
            AttendanceHistoryUiEvent.Retry -> _uiStateFlow.value.studentId?.let { loadHistory(it) }
        }
    }

    private fun loadHistory(studentId: String) {
        _uiStateFlow.update { it.copy(isLoading = true, error = null, studentId = studentId) }
        viewModelScope.launch {
            repository.getStudentHistory(studentId)
                .onSuccess { records ->
                    _uiStateFlow.update { it.copy(isLoading = false, records = records) }
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun updateDateFilter(date: java.time.LocalDate) {
        _uiStateFlow.update { it.copy(selectedDate = date) }
    }
}
