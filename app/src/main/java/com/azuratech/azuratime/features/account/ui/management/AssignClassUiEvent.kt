package com.azuratech.azuratime.features.account.ui.management

sealed class AssignClassUiEvent {
    data class LoadInitialData(val targetAccountId: String) : AssignClassUiEvent()
    data class ToggleClass(val classId: String) : AssignClassUiEvent() // 🔥 Toggle class presence in Step 2
    data class ToggleAssignment(val classId: String, val subjectId: String?) : AssignClassUiEvent() // 🔥 Toggle specific pairing
    data class UpdateSearchQuery(val query: String) : AssignClassUiEvent()
    data object SelectAllFiltered : AssignClassUiEvent()
    data object ClearAllSelections : AssignClassUiEvent()
    data object SaveAssignments : AssignClassUiEvent()
    data object ClearError : AssignClassUiEvent()
}
