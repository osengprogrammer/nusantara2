package com.azuratech.azuratime.features.school.ui.classes

import com.azuratech.azuraengine.model.ClassModel

/**
 * 🏫 CLASS UI STATE (v3.2.0-ai-native)
 */
data class ClassUiState(
    val isLoading: Boolean = false,
    val classes: List<ClassModel> = emptyList(),
    val availableClasses: List<String> = emptyList(),
    val selectedClassId: String? = null,
    val error: String? = null,
    val isAddDialogVisible: Boolean = false,
    val classToEdit: ClassModel? = null,
    val classToDelete: ClassModel? = null,
)
