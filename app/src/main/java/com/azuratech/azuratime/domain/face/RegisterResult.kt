package com.azuratech.azuratime.domain.face

sealed class RegisterResult {
    object Success : RegisterResult()
    data class Duplicate(val name: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}
