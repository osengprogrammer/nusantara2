package com.azuratech.azuratime.features.account.ui.membership

import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile

/**
 * 🛠️ MEMBERSHIP UI STATE (v3.2.0-ai-native)
 */
data class MembershipUiState(
    val account: AccountEntity? = null,
    val accessRequests: List<AccessRequestProfile> = emptyList(),
    val memberships: List<Membership> = emptyList(),
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
