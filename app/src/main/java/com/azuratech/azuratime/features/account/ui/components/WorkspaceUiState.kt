package com.azuratech.azuratime.features.account.ui.components

import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile

data class WorkspaceUiState(
    val status: WorkspaceStatus = WorkspaceStatus.Idle,
    val searchQuery: String = "",
    val searchResults: List<Map<String, Any>> = emptyList(),
    val accessRequests: List<AccessRequestProfile> = emptyList(),
)

sealed class WorkspaceStatus {
    object Idle : WorkspaceStatus()
    object Switching : WorkspaceStatus()
    data class Success(val schoolName: String) : WorkspaceStatus()
    data class RequestSent(val schoolName: String) : WorkspaceStatus()
    data class RequestFailed(val message: String?) : WorkspaceStatus()
    data class Error(val message: String) : WorkspaceStatus()
}
