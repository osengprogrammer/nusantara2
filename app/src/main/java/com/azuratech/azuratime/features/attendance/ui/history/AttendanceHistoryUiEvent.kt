package com.azuratech.azuratime.features.attendance.ui.history

import java.time.LocalDate

sealed class AttendanceHistoryUiEvent {
    data class LoadHistory(val studentId: String) : AttendanceHistoryUiEvent()
    data class FilterByDate(val date: LocalDate) : AttendanceHistoryUiEvent()
    data object Retry : AttendanceHistoryUiEvent()
}
