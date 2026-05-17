package com.azuratech.azuratime.features.auth.ui

/**
 * 🔐 AUTH UI EVENT
 * v3.2.0-ai-native compliant
 */
sealed class AuthUiEvent {
    data class UpdateEmail(val email: String) : AuthUiEvent()
    data class UpdatePassword(val password: String) : AuthUiEvent()
    data class UpdateSchoolName(val schoolName: String) : AuthUiEvent()
    object LoginWithEmail : AuthUiEvent()
    object RegisterSchool : AuthUiEvent()
    data class SignInWithGoogle(val idToken: String) : AuthUiEvent()
    object ClearError : AuthUiEvent()
    object Logout : AuthUiEvent()
    object NavigateToDashboard : AuthUiEvent()
}
