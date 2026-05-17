package com.azuratech.azuratime.features.attendance.ui.history

import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import java.time.LocalDate

data class AttendanceHistoryUiState(
    val isLoading: Boolean = false,
    val records: List<AttendanceRecord> = emptyList(),
    val error: String? = null,
    val selectedDate: LocalDate? = null,
    val studentId: String? = null,
)
