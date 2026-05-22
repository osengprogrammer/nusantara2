package com.azuratech.azuratime.features.update.ui

import java.io.File

/**
 * 🚀 APP UPDATE UI STATE (v3.2.0-ai-native)
 */
data class AppUpdateUiState(
    val isLoading: Boolean = false,
    val updateAvailable: Boolean = false,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val downloadProgress: Float = 0f,
    val apkFile: File? = null,
    val error: String? = null,
    val showDialog: Boolean = false,
)
