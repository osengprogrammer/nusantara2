package com.azuratech.azuratime.features.student.ui.roster

import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem

/**
 * 🎓 STUDENT ROSTER BARCODE UI STATE
 */
data class StudentRosterBarcodeUiState(
    val isLoading: Boolean = false,
    val students: List<StudentDisplayItem> = emptyList(),
    val selectedStudentIds: Set<String> = emptySet(),
    val schoolId: String? = null,
    val error: String? = null,
)
