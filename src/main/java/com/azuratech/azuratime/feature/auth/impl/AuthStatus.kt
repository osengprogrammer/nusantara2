package com.azuratech.azuratime.feature.auth.api

sealed interface AuthStatus
object LoggedIn : AuthStatus
object LoggedOut : AuthStatus
data class Error(val message: String) : AuthStatus
