package com.azuratech.azuratime.features.account.ui.components

sealed class WorkspaceUiEvent {
    data class ChangeWorkspace(val accountId: String, val newSchoolId: String, val newSchoolName: String) : WorkspaceUiEvent()
    data class UpdateSearchQuery(val query: String) : WorkspaceUiEvent()
    data class SendJoinRequest(val accountId: String, val schoolId: String, val schoolName: String) : WorkspaceUiEvent()
    data class LeaveSchool(val schoolId: String) : WorkspaceUiEvent()
    data class CreateNewSchool(val accountId: String, val accountEmail: String, val schoolName: String) : WorkspaceUiEvent()
    data class FinalizeSetup(val schoolId: String) : WorkspaceUiEvent()
    data class UpdateSchoolName(val schoolId: String, val accountId: String, val newName: String, val onSuccess: () -> Unit, val onError: (String) -> Unit) : WorkspaceUiEvent()
    object ResetState : WorkspaceUiEvent()
}
