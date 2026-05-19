package com.azuratech.azuratime.features.student.ui.bulk

import android.net.Uri

/**
 * 📝 REGISTER UI EVENT (v3.2.0-ai-native)
 */
sealed class RegisterUiEvent {
    data class ProcessCsv(val uri: Uri) : RegisterUiEvent()
    data object ResetState : RegisterUiEvent()
}
