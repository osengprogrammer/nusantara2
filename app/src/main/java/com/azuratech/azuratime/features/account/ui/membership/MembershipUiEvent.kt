package com.azuratech.azuratime.features.account.ui.membership

/**
 * 🛠️ MEMBERSHIP UI EVENT (v3.2.0-ai-native)
 */
sealed class MembershipUiEvent {
    data class CheckMembership(val email: String, val displayName: String? = null) : MembershipUiEvent()
    data object ActivateMembership : MembershipUiEvent()
    data object ClearError : MembershipUiEvent()
}
