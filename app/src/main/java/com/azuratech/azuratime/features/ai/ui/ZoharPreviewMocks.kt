package com.azuratech.azuratime.features.ai.ui

/**
 * 🧬 ZOHAR PREVIEW MOCKS (v3.2.0-ai-native)
 */
object ZoharPreviewMocks {
    fun loading(): ZoharUiState = ZoharUiState(isLoading = true)

    fun success(): ZoharUiState = ZoharUiState(
        response = "Based on today's data, 90% of students attended on time. Great job Brother!",
        conversationHistory = listOf(
            ChatMessage(ChatRole.USER, "How is the attendance today?"),
            ChatMessage(ChatRole.ZOHAR, "Based on today's data, 90% of students attended on time. Great job Brother!"),
        ),
    )

    fun error(): ZoharUiState = ZoharUiState()
}
