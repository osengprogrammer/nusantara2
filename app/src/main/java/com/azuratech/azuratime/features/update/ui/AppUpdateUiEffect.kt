package com.azuratech.azuratime.features.update.ui

import java.io.File

/**
 * 🚀 APP UPDATE UI EFFECT (v3.2.0-ai-native)
 */
sealed class AppUpdateUiEffect {
    data class ShowToast(val message: String) : AppUpdateUiEffect()
    data class InstallApk(val apkFile: File) : AppUpdateUiEffect()
}
