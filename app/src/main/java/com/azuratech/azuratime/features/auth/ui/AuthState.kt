package com.azuratech.azuratime.features.auth.ui

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object NewAccountNeedRegistration : AuthState()
    data class Success(val email: String, val role: String = "USER") : AuthState()
    data class Error(val message: String) : AuthState()
}
