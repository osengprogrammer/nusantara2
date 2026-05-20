package com.azuratech.azuratime.features.student.ui.roster

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem

/**
 * 🎓 STUDENT ROSTER UI STATE (v3.2.0-ai-native)
 */
data class StudentRosterUiState(
    val isLoading: Boolean = false,
    val students: List<StudentDisplayItem> = emptyList(),
    val allClasses: List<ClassModel> = emptyList(),
    val selectedClassId: String? = null,
    val searchQuery: String = "",
    val isDeleteDialogVisible: Boolean = false,
    val targetStudentId: String? = null,
)
