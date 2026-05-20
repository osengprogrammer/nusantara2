package com.azuratech.azuratime.features.student.ui.roster

/**
 * 🎓 STUDENT ROSTER UI EFFECT (v3.2.0-ai-native)
 */
sealed class StudentRosterUiEffect {
    data class ShowToast(val message: String) : StudentRosterUiEffect()
    data class NavigateToDetail(val studentId: String) : StudentRosterUiEffect()
}
