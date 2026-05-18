package com.azuratech.azuratime.core.ui

/**
 * 🛠️ MAIN UI EVENT (v3.2.0-ai-native)
 */
sealed class MainUiEvent {
    data object InitializeApp : MainUiEvent()
    data class HandleRevoke(val isRevoked: Boolean) : MainUiEvent()
}
