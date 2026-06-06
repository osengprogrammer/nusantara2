package com.azuratech.azuratime.features.account.ui.management

import android.net.Uri

/**
 * 👤 ACCOUNT UI EVENT (v3.2.0-ai-native)
 */
sealed class AccountUiEvent {
    data object LoadProfile : AccountUiEvent()
    data class UpdateDisplayName(val newName: String) : AccountUiEvent()
    data class SelectActiveClass(val classId: String?, val targetAccountId: String? = null) : AccountUiEvent()
    data class UpdatePhoto(val uri: Uri) : AccountUiEvent()
    data class AssignClassToAccount(val classId: String, val targetAccountId: String? = null) : AccountUiEvent()
    data class RemoveClassAccess(val classId: String, val targetAccountId: String? = null) : AccountUiEvent()
    data object ClearPhoto : AccountUiEvent()
    data object Logout : AccountUiEvent()
    data object ClearError : AccountUiEvent()
    data object NavigateBack : AccountUiEvent()
    data class UpdatePendingRole(val requestId: String, val role: com.azuratech.azuratime.core.domain.model.AccountRole) : AccountUiEvent()
    data class ApproveFollower(val requestId: String) : AccountUiEvent()
}
