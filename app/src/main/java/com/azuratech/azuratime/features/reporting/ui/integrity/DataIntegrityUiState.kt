package com.azuratech.azuratime.features.reporting.ui.integrity

import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict

/**
 * 🏰 DATA INTEGRITY UI STATE
 * v3.2.0-ai-native compliant
 */
data class DataIntegrityUiState(
    val totalStudents: Int = 0,
    val totalRecords: Int = 0,
    val missingAssignments: Int = 0,
    val brokenAssignments: Int = 0,
    val unsyncedCount: Int = 0,
    val conflicts: List<AttendanceConflict> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
