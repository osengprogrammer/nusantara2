package com.azuratech.azuratime.features.auth.ui

/**
 * 🔐 AUTH UI STATE
 * v3.2.0-ai-native compliant
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val schoolName: String = "",
    val isLoading: Boolean = false,
    val isGoogleSigning: Boolean = false,
    val error: String? = null,
    val authStatus: AuthStatus = AuthStatus.Idle,
    val accountRole: String? = null,
    val accountEmail: String? = null,
)

enum class AuthStatus {
    Idle, LoggedIn, Registering
}
