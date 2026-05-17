package com.azuratech.azuratime.features.student.ui.form

import android.graphics.Bitmap

/**
 * 🎨 STUDENT FORM UI EVENT
 * v3.2.0-ai-native compliant
 */
sealed class StudentFormUiEvent {
    data class UpdateField(val field: String, val value: Any) : StudentFormUiEvent()
    object CapturePhoto : StudentFormUiEvent()
    data class PhotoSelected(val uri: String) : StudentFormUiEvent()
    data class PhotoCaptured(val bitmap: Bitmap) : StudentFormUiEvent()
    data class BiometricScanned(val encoding: ByteArray) : StudentFormUiEvent()
    data class FaceCaptured(val bitmap: Bitmap, val embedding: FloatArray) : StudentFormUiEvent()
    object SubmitForm : StudentFormUiEvent()
    object Retry : StudentFormUiEvent()
    object ClearError : StudentFormUiEvent()
    object NavigateBack : StudentFormUiEvent()
}
