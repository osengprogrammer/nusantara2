package com.azuratech.azuratime.features.account.ui.components

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.data.local.AccountEntity

/**
 * 🚥 FOLLOWING UI STATE (v3.2.0-ai-native)
 */
data class FollowingUiState(
    val searchQuery: String = "",
    val results: List<AccountEntity> = emptyList(),
    val sentRequestIds: Set<String> = emptySet(),
    val pendingRequests: List<AccountEntity> = emptyList(),
    val connections: List<AccountEntity> = emptyList(),
    val availableClasses: List<ClassModel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val isLoadingConnections: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isProcessing: Boolean = false,
    val selectedFriendForAssignment: AccountEntity? = null,
    val isAdmin: Boolean = false,
    val activeSchoolId: String? = null,
)
