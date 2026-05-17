package com.azuratech.azuratime.features.school.ui.classes

import com.azuratech.azuraengine.model.ClassModel

/**
 * 🏫 CLASS UI EVENT (v3.2.0-ai-native)
 */
sealed class ClassUiEvent {
    data object LoadClasses : ClassUiEvent()
    data class CreateClass(val name: String) : ClassUiEvent()
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
}
