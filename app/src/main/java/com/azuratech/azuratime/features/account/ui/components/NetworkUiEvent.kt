package com.azuratech.azuratime.features.account.ui.components

/**
 * 🚥 NETWORK UI EVENT (v3.2.0-ai-native)
 */
sealed class NetworkUiEvent {
    data class SearchByEmail(val email: String) : NetworkUiEvent()
    data class SendFriendRequest(val targetEmail: String) : NetworkUiEvent()
    object ClearError : NetworkUiEvent()
    object NavigateBack : NetworkUiEvent()
}
