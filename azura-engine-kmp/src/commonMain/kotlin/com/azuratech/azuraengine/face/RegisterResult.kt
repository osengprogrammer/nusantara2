package com.azuratech.azuraengine.face

sealed class RegisterResult {
    object Success : RegisterResult()
    data class Duplicate(val name: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}
