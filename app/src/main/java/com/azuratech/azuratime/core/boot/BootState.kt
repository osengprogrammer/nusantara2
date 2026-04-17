package com.azuratech.azuratime.core.boot

sealed class BootState {

    object Loading : BootState()
    object NeedLogin : BootState()
    object NeedActivation : BootState()
    object Expired : BootState()
    object Ready : BootState()
    data class Error(val message: String) : BootState()
}