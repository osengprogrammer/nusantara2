package com.azuratech.azuratime.features.school.ui.classes

import com.azuratech.azuratime.core.domain.model.ClassModel

/**
 * 🏫 CLASS UI EVENT (v3.2.0-ai-native)
 */
sealed class ClassUiEvent {
    data object LoadClasses : ClassUiEvent()
    data class CreateClass(
        val name: String,
        val level: Int = 0,
        val category: String = "",
        val major: String = "",
        val section: String = "",
    ) : ClassUiEvent()
    data class UpdateClass(val id: String, val newName: String) : ClassUiEvent()
    data class RequestDeleteClass(val classModel: ClassModel) : ClassUiEvent()
    data object ConfirmDeleteClass : ClassUiEvent()
    data object CancelDeleteClass : ClassUiEvent()
    data class RequestEditClass(val classModel: ClassModel) : ClassUiEvent()
    data object CancelEditClass : ClassUiEvent()
    data object ShowAddDialog : ClassUiEvent()
    data object DismissAddDialog : ClassUiEvent()
    data object ClearError : ClassUiEvent()
    data object SyncClasses : ClassUiEvent()
    data class SelectClass(val classId: String?) : ClassUiEvent()
    data object ShowAddStudentDialog : ClassUiEvent()
    data object DismissAddStudentDialog : ClassUiEvent()
    data class AddStudentToClass(val classId: String, val studentId: String) : ClassUiEvent()

    // Structured mode events
    data object ToggleInputMode : ClassUiEvent()
    data class SetSelectedLevel(val level: Int) : ClassUiEvent()
    data class SetSelectedCategory(val category: String) : ClassUiEvent()
    data class SetSelectedMajor(val major: String) : ClassUiEvent()
    data class SetSelectedSection(val section: String) : ClassUiEvent()
}
