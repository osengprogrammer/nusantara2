package com.azuratech.azuratime.features.ai.ui

/**
 * 🤖 ZOHAR UI EVENT (v3.2.0-ai-native)
 */
sealed class ZoharUiEvent {
    data class AskZohar(val query: String) : ZoharUiEvent()
    object ClearChat : ZoharUiEvent()
    object Retry : ZoharUiEvent()
    object ClearError : ZoharUiEvent()
}
