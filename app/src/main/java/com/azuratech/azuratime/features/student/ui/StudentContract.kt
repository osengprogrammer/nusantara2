package com.azuratech.azuratime.features.student.ui

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.core.ui.components.StudentDisplayItem

/**
 * 🎓 STUDENT UI STATE
 */
data class StudentUiState(
    val isLoading: Boolean = false,
    val students: List<StudentDisplayItem> = emptyList(),
    val allClasses: List<ClassModel> = emptyList(),
    val selectedClassId: String? = null,
    val searchQuery: String = "",
    val isAddDialogVisible: Boolean = false,
    val isEditDialogVisible: Boolean = false,
    val editingStudent: StudentProfile? = null,
    val error: String? = null,
)

/**
 * ⚡ STUDENT UI EVENT
 */
sealed class StudentUiEvent {
    data object LoadStudents : StudentUiEvent()
    data class SelectClass(val classId: String?) : StudentUiEvent()
    data class UpdateSearch(val query: String) : StudentUiEvent()
    data object OpenAddDialog : StudentUiEvent()
    data class OpenEditDialog(val student: StudentProfile) : StudentUiEvent()
    data class DeleteStudent(val studentId: String) : StudentUiEvent()
    data object SyncStudents : StudentUiEvent()
    data object DismissDialog : StudentUiEvent()
}

/**
 * ✨ STUDENT UI EFFECT
 */
sealed class StudentUiEffect {
    data class ShowToast(val message: String) : StudentUiEffect()
    data class NavigateToDetail(val studentId: String) : StudentUiEffect()
}
