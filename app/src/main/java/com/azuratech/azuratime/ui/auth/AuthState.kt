package com.azuratech.azuratime.ui.auth

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object NewUserNeedRegistration : AuthState()
    data class Success(val email: String, val role: String = "TEACHER") : AuthState()
    data class Error(val message: String) : AuthState()
}