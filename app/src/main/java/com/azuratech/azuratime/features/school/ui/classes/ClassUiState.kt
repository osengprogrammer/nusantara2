package com.azuratech.azuratime.features.school.ui.classes

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.student.domain.model.StudentProfile

/**
 * 🏫 CLASS UI STATE (v3.2.0-ai-native)
 */
data class ClassUiState(
    val isLoading: Boolean = false,
    val classes: List<ClassModel> = emptyList(),
    val availableClasses: List<String> = emptyList(),
    val selectedClassId: String? = null,
    val studentsInClass: List<StudentProfile> = emptyList(),
    val allStudents: List<StudentProfile> = emptyList(),
    val error: String? = null,
    val isAddDialogVisible: Boolean = false,
    val isAddStudentDialogVisible: Boolean = false,
    val studentCountsByClassId: Map<String, Int> = emptyMap(),
    val classToEdit: ClassModel? = null,
    val classToDelete: ClassModel? = null,
)
