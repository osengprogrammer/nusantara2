package com.azuratech.azuratime.features.student.ui.form

/**
 * ⚡ STUDENT FORM UI EFFECT (v3.2.0-ai-native)
 */
sealed class StudentFormUiEffect {
    data class ShowToast(val message: String) : StudentFormUiEffect()
    data class ShowSnackbar(val message: String) : StudentFormUiEffect()
    data object NavigateBack : StudentFormUiEffect()
}
