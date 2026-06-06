package com.azuratech.azuratime.features.account.ui.components

/**
 * 🚥 FOLLOWING UI EVENT (v3.2.0-ai-native)
 */
sealed class FollowingUiEvent {
    data class SearchByEmail(val email: String) : FollowingUiEvent()
    data class SendConnectionRequest(val targetAccountId: String) : FollowingUiEvent()
    data class AcceptRequest(val senderId: String) : FollowingUiEvent()
    data class DeclineRequest(val senderId: String) : FollowingUiEvent()
    data class SelectFriendForAssignment(val friend: com.azuratech.azuratime.features.account.data.local.AccountEntity?) : FollowingUiEvent()
    data class AssignClasses(val targetId: String, val classIds: List<String>) : FollowingUiEvent()
    data class ChangeMemberRole(val targetAccountId: String, val newRole: com.azuratech.azuratime.core.domain.model.AccountRole) : FollowingUiEvent()
    data class UnfollowFriend(val targetAccountId: String) : FollowingUiEvent()
    object LoadData : FollowingUiEvent()
    object ClearError : FollowingUiEvent()
    object NavigateBack : FollowingUiEvent()
}
