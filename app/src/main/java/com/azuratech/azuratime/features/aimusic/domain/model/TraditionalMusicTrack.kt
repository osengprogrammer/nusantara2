package com.azuratech.azuratime.features.aimusic.domain.model

/**
 * 🚀 TraditionalMusicTrack.kt (v3.2.0-ai-native)
 * Domain model for a traditional music track.
 */
data class TraditionalMusicTrack(
    val id: String,
    val name: String,
    val region: String,
    val instrument: String,
    val mood: String,
    val audioUrl: String?,
)
