package com.azuratech.azuratime.features.update.domain.model

import kotlinx.serialization.Serializable

/**
 * 🚀 APP VERSION INFO (v3.2.0-ai-native)
 */
@Serializable
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
)
