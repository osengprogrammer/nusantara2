package com.azuratech.azuratime.features.account.ui.management

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.account.domain.model.UserProfile

/**
 * 👤 ACCOUNT UI STATE (v3.2.0-ai-native)
 */
data class AccountUiState(
    val userProfile: UserProfile? = null,
    val activeClassId: String? = null,
    val availableClasses: List<ClassModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditingProfile: Boolean = false,
    val pendingPhotoUri: String? = null,
    // SSOT Fields
    val assignedClassIds: List<String> = emptyList(),
    val allUsersInSameSchool: List<com.azuratech.azuratime.features.account.data.local.AccountEntity> = emptyList(),
    val selectedTargetUser: com.azuratech.azuratime.features.account.data.local.AccountEntity? = null,
    val targetAssignedClassIds: List<String> = emptyList(),
)
