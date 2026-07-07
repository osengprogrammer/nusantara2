package com.azuratech.azuratime.features.student.ui.roster
import com.azuratech.azuratime.features.student.ui.components.StudentRosterItem

import com.azuratech.azuraengine.model.ClassModel

/**
 * 🎓 STUDENT ROSTER UI STATE (v3.2.0-ai-native)
 */
data class StudentRosterUiState(
    val isLoading: Boolean = false,
    val students: List<StudentRosterItem> = emptyList(),
    val allClasses: List<ClassModel> = emptyList(),
    val selectedClassId: String? = null,
    val searchQuery: String = "",
    val isDeleteDialogVisible: Boolean = false,
    val targetStudentId: String? = null,
)
