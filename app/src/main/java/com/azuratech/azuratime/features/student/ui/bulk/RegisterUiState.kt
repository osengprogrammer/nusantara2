package com.azuratech.azuratime.features.student.ui.bulk

import com.azuratech.azuratime.core.result.ProcessResult

/**
 * 📝 REGISTER UI STATE (v3.2.0-ai-native)
 */
data class RegisterUiState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val status: String = "",
    val results: List<ProcessResult> = emptyList(),
)
