package com.azuratech.azuratime.core.ui

/**
 * 🛠️ MAIN UI STATE (v3.2.0-ai-native)
 */
data class MainUiState(
    val isRevoked: Boolean = false,
    val currentEmail: String = "",
    val isInitialized: Boolean = false,
)
