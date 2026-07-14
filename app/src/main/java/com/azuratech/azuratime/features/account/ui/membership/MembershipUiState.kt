package com.azuratech.azuratime.features.account.ui.membership

import com.azuratech.azuratime.core.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import com.azuratech.azuratime.features.account.domain.model.SchoolMembership



/**
 * 🛠️ MEMBERSHIP UI STATE (v3.2.1-ai-native)
 */
data class MembershipUiState(
    val account: AccountEntity? = null,
    val accessRequests: List<AccessRequestProfile> = emptyList(),
    val memberships: List<SchoolMembership> = emptyList(),
    val status: MembershipStatus = MembershipStatus.Loading,
    val error: String? = null,
)

sealed class MembershipStatus {
    data object Loading : MembershipStatus()
    data object Pending : MembershipStatus()
    data object Approved : MembershipStatus()
    data object Idle : MembershipStatus()
    data class Rejected(val message: String) : MembershipStatus()
}
