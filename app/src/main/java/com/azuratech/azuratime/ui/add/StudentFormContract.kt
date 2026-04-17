package com.azuratech.azuratime.ui.add

import android.graphics.Bitmap
import com.azuratech.azuratime.data.local.ClassEntity

/**
 * Represents the state of the student registration/edit form.
 * This is the single source of truth for the UI.
 */
data class StudentFormUiState(
    // Form Fields
    val name: String = "",
    val studentId: String = "",
    val selectedClassId: String? = null,
    val capturedBitmap: Bitmap? = null,
    val embedding: FloatArray? = null,

    // UI State
    val availableClasses: List<ClassEntity> = emptyList(),
    val isSubmitting: Boolean = false,
    val formError: String? = null,
    val isEditMode: Boolean = false,
    val pageTitle: String = "Pendaftaran Baru",

    // Real-time Validation
    val isFormValid: Boolean = false
) {
    // Overriding equals and hashCode for FloatArray comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StudentFormUiState

        if (name != other.name) return false
        if (studentId != other.studentId) return false
        if (selectedClassId != other.selectedClassId) return false
        if (capturedBitmap != other.capturedBitmap) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (availableClasses != other.availableClasses) return false
        if (isSubmitting != other.isSubmitting) return false
        if (formError != other.formError) return false
        if (isEditMode != other.isEditMode) return false
        if (pageTitle != other.pageTitle) return false
        if (isFormValid != other.isFormValid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + studentId.hashCode()
        result = 31 * result + (selectedClassId?.hashCode() ?: 0)
        result = 31 * result + (capturedBitmap?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + availableClasses.hashCode()
        result = 31 * result + isSubmitting.hashCode()
        result = 31 * result + (formError?.hashCode() ?: 0)
        result = 31 * result + isEditMode.hashCode()
        result = 31 * result + pageTitle.hashCode()
        result = 31 * result + isFormValid.hashCode()
        return result
    }
}
