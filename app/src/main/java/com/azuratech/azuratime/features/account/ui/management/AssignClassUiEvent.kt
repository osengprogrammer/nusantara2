package com.azuratech.azuratime.features.account.ui.management

sealed class AssignClassUiEvent {
    data class LoadInitialData(val targetAccountId: String) : AssignClassUiEvent()
    data class ToggleClassSelection(val classId: String) : AssignClassUiEvent()
    data object SaveAssignments : AssignClassUiEvent()
    data object ClearError : AssignClassUiEvent()
}
