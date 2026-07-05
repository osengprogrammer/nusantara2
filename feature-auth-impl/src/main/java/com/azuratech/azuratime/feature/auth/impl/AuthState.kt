package com.azuratech.azuratime.feature.auth.api

data class AuthState(val status: AuthStatus, val userId: String? = null)
