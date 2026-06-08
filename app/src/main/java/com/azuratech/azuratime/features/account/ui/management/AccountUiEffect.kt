package com.azuratech.azuratime.features.account.ui.management

/**
 * 🚀 ACCOUNT UI EFFECT (v3.2.0-ai-native)
 * Transient events for account management.
 */
sealed class AccountUiEffect {
    data class ShowToast(val message: String) : AccountUiEffect()
    data class ShowSnackbar(val message: String) : AccountUiEffect()
    data class NavigateTo(val route: String) : AccountUiEffect()
    data object NavigateToWelcome : AccountUiEffect()
    data object NavigateBack : AccountUiEffect()
}
