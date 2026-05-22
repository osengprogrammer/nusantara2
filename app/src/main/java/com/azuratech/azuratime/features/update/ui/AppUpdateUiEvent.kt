package com.azuratech.azuratime.features.update.ui

/**
 * 🚀 APP UPDATE UI EVENT (v3.2.0-ai-native)
 */
sealed class AppUpdateUiEvent {
    object CheckForUpdate : AppUpdateUiEvent()
    object DownloadUpdate : AppUpdateUiEvent()
    object InstallUpdate : AppUpdateUiEvent()
    object DismissDialog : AppUpdateUiEvent()
    object ClearError : AppUpdateUiEvent()
}
