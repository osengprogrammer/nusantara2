package com.azuratech.azuratime.features.reporting.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import javax.inject.Inject

/**
 * 📊 DAILY DETAIL VIEW MODEL (v3.2.0-ai-native)
 * Optimized with Effect-Driven MVI pattern.
 */
@HiltViewModel
class DailyDetailViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiStateFlow = MutableStateFlow(DailyDetailUiState())
    val uiStateFlow: StateFlow<DailyDetailUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<DailyDetailUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    fun onEvent(event: DailyDetailUiEvent) {
        when (event) {
            is DailyDetailUiEvent.LoadData -> loadData(event.studentId, event.date)
        }
    }

    private fun loadData(studentId: String, dateString: String) {
        _uiStateFlow.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId == null) {
                _uiStateFlow.update { it.copy(isLoading = false) }
                _uiEffectFlow.emit(DailyDetailUiEffect.ShowToast("Sesi sekolah tidak valid"))
                return@launch
            }

            try {
                val date = LocalDate.parse(dateString)
                attendanceRepository.getStudentHistory(studentId)
                    .onSuccess { records ->
                        val dayRecords = records.filter { record ->
                            val recordDate = Instant.ofEpochMilli(record.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            recordDate == date
                        }
                        _uiStateFlow.update {
                            it.copy(
                                isLoading = false,
                                records = dayRecords,
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiStateFlow.update { it.copy(isLoading = false) }
                        _uiEffectFlow.emit(DailyDetailUiEffect.ShowToast("Gagal: ${error.message}"))
                    }
            } catch (e: Exception) {
                _uiStateFlow.update { it.copy(isLoading = false) }
                _uiEffectFlow.emit(DailyDetailUiEffect.ShowToast("Format tanggal tidak valid"))
            }
        }
    }
}
