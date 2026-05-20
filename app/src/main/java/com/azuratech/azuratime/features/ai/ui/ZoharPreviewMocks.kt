package com.azuratech.azuratime.features.ai.ui

/**
 * 🧬 ZOHAR PREVIEW MOCKS (v3.2.0-ai-native)
 */
object ZoharPreviewMocks {
    fun loading(): ZoharUiState = ZoharUiState(isLoading = true)

    fun success(): ZoharUiState = ZoharUiState(
        response = "Berdasarkan data hari ini, 90% siswa hadir tepat waktu. Mantap Brother!",
        conversationHistory = listOf(
            ChatMessage(ChatRole.USER, "Bagaimana kehadiran hari ini?"),
            ChatMessage(ChatRole.ZOHAR, "Berdasarkan data hari ini, 90% siswa hadir tepat waktu. Mantap Brother!"),
        ),
    )

    fun error(): ZoharUiState = ZoharUiState()
}
