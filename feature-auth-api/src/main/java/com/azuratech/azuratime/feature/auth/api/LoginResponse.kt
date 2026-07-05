package com.azuratech.azuratime.feature.auth.api

data class LoginResponse(val token: String, val expiresIn: Long)
