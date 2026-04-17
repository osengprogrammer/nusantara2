package com.azuratech.azuratime.ui.membership

sealed class MembershipState {
    object Idle : MembershipState()
    object Loading : MembershipState()
    object Pending : MembershipState()
    object Approved : MembershipState()
    data class Rejected(val reason: String?) : MembershipState()
    data class Error(val message: String) : MembershipState()
}