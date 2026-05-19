package com.azuratech.azuratime.features.attendance.ui

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord

/**
 * 📝 ATTENDANCE UI STATE (v3.2.0-ai-native)
 */
data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isExporting: Boolean = false,
    val exportPath: String? = null,
    val records: List<AttendanceRecord> = emptyList(),
    val classes: List<ClassModel> = emptyList(),
    val selectedClassId: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
)
