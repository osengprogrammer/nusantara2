package com.azuratech.azuratime.features.student.ui.form

import android.graphics.Bitmap
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.student.domain.model.StudentProfile

/**
 * 🎨 STUDENT FORM UI STATE (v3.2.0-ai-native)
 */
data class StudentFormUiState(
    val profile: StudentProfile = StudentProfile(
        studentId = "",
        name = "",
        schoolId = "",
    ),
    val availableClasses: List<ClassModel> = emptyList(),
    val isSubmitting: Boolean = false,
    val isCapturingPhoto: Boolean = false,
    val biometricStatus: BiometricStatus = BiometricStatus.Idle,
    val validationErrors: Map<String, String> = emptyMap(),
    val isSubmitted: Boolean = false,
    val isEditMode: Boolean = false,
    val pageTitle: String = "Add Student",
    val capturedBitmap: Bitmap? = null,
) {
    val isFormValid: Boolean
        get() = profile.name.isNotBlank() &&
            profile.studentId.isNotBlank() &&
            profile.classIds.isNotEmpty() &&
            profile.embedding != null
}

enum class BiometricStatus {
    Idle, Ready, Scanning, Success, Failure
}
