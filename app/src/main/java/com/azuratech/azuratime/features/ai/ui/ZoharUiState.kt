package com.azuratech.azuratime.features.ai.ui

/**
 * 🤖 ZOHAR UI STATE (v3.2.0-ai-native)
 */
data class ZoharUiState(
    val query: String = "",
    val response: String = "Halo Brother! Zohar siap mengawal Azura Ecosystem. Ada yang bisa Zohar bantu? Joss Gandos!",
    val isLoading: Boolean = false,
    val conversationHistory: List<ChatMessage> = emptyList(),
)

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class ChatRole {
    USER,
    ZOHAR,
}
