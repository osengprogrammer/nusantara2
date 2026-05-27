package com.azuratech.azuratime.features.update.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 🚀 APP VERSION INFO (v3.2.0-ai-native)
 * Updated to match server JSON keys.
 */
@Serializable
data class AppVersionInfo(
    @SerialName("latest_version_code")
    val versionCode: Int,
    @SerialName("latest_version")
    val versionName: String,
    @SerialName("download_url")
    val downloadUrl: String,
    @SerialName("release_notes")
    val releaseNotes: String,
)
