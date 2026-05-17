package com.azuratech.azuratime.features.biometric.ui.assignment

/**
 * 🧬 ASSIGNMENT UI EVENT (v3.2.0-ai-native)
 */
sealed class AssignmentUiEvent {
    data object LoadAssignment : AssignmentUiEvent()
    data class AssignStudent(val studentId: String, val classId: String) : AssignmentUiEvent()
    data class RemoveAssignment(val studentId: String, val classId: String) : AssignmentUiEvent()
    data class RemoveAllAssignments(val studentId: String) : AssignmentUiEvent()
    data class SelectStudent(val studentId: String?) : AssignmentUiEvent()
    data object Refresh : AssignmentUiEvent()
    data object ClearError : AssignmentUiEvent()
}
