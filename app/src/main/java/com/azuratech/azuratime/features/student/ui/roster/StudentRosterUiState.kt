package com.azuratech.azuratime.features.student.ui.roster

import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem
import com.azuratech.azuraengine.model.ClassModel

/**
 * 🎓 STUDENT ROSTER UI STATE
 */
sealed class StudentRosterUiState {
    object Loading : StudentRosterUiState()
    data class Success(val data: StudentRosterData) : StudentRosterUiState()
    data class Error(val message: String) : StudentRosterUiState()
}

data class StudentRosterData(
    val searchQuery: String = "",
    val selectedClassName: String? = null,
    val students: List<StudentDisplayItem> = emptyList(),
    val allClasses: List<ClassModel> = emptyList(),
    val isSyncing: Boolean = false,
    val studentForClassAssignment: StudentDisplayItem? = null,
    val studentForQuickEdit: StudentDisplayItem? = null
)
