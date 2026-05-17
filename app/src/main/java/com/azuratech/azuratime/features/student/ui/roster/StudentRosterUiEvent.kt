package com.azuratech.azuratime.features.student.ui.roster

/**
 * 🎓 STUDENT ROSTER UI EVENT (v3.2.0-ai-native)
 */
sealed class StudentRosterUiEvent {
    data object LoadRoster : StudentRosterUiEvent()
    data class SelectClass(val classId: String?) : StudentRosterUiEvent()
    data class UpdateSearch(val query: String) : StudentRosterUiEvent()
    data class RequestDelete(val studentId: String) : StudentRosterUiEvent()
    data object CancelDelete : StudentRosterUiEvent()
    data class ConfirmDelete(val studentId: String) : StudentRosterUiEvent()
    data object RetryDelete : StudentRosterUiEvent()
    data object ClearError : StudentRosterUiEvent()
    data class NavigateToDetail(val studentId: String) : StudentRosterUiEvent()
    data object SyncStudents : StudentRosterUiEvent()
}
