package com.azuratech.azuratime.features.account.ui.management
import com.azuratech.azuratime.core.data.local.AccessRequestEntity
import com.azuratech.azuratime.core.data.local.AccountEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.account.domain.model.AccountProfile



/**
 * 👤 ACCOUNT UI STATE (v3.2.0-ai-native)
 */
data class AccountUiState(
    val accountProfile: AccountProfile? = null,
    val activeClassId: String? = null,
    val availableClasses: List<ClassModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditingProfile: Boolean = false,
    val pendingPhotoUri: String? = null,
    // SSOT Fields
    val assignedClassIds: List<String> = emptyList(),
    val allAccountsInSameSchool: List<AccountEntity> = emptyList(),
    val selectedTargetAccount: AccountEntity? = null,
    val targetAssignedClassIds: List<String> = emptyList(),
    val pendingFollowers: List<AccessRequestEntity> = emptyList(),
    val selectedRoles: Map<String, com.azuratech.azuratime.core.domain.model.AccountRole> = emptyMap(),
    val currentAccountRole: com.azuratech.azuratime.core.domain.model.AccountRole = com.azuratech.azuratime.core.domain.model.AccountRole.USER,
    val activeSchoolId: String? = null,
    val isLoggingOut: Boolean = false,
)
