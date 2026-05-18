package com.azuratech.azuratime.core.boot

/**
 * 🚀 BOOT UI STATE (v3.2.0-ai-native)
 */
sealed class BootUiState {
    data object Loading : BootUiState()
    data object Ready : BootUiState()
    data object NeedLogin : BootUiState()
    data object NeedActivation : BootUiState()
    data class Error(val message: String) : BootUiState()
}
