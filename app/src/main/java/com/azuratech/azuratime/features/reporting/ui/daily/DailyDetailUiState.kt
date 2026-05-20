package com.azuratech.azuratime.features.reporting.ui.daily

import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord

/**
 * 📝 DAILY DETAIL UI STATE (v3.2.0-ai-native)
 */
data class DailyDetailUiState(
    val isLoading: Boolean = false,
    val records: List<AttendanceRecord> = emptyList(),
)
