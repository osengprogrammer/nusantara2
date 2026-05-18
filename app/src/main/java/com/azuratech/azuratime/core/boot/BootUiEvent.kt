package com.azuratech.azuratime.core.boot

/**
 * 🚀 BOOT UI EVENT (v3.2.0-ai-native)
 */
sealed class BootUiEvent {
    data object CheckAuthStatus : BootUiEvent()
    data object Recheck : BootUiEvent()
}
