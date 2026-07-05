package com.azuratech.azuratime.core.auth.api.model

/**
 * Authentication state sealed class for reactive UI updates.
 * Pure Kotlin, zero Android/Firebase dependencies.
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object NewAccountNeedRegistration : AuthState()
    data class Success(
        val email: String,
        val uid: String,
        val role: String = "USER"
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}