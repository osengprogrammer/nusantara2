package com.azuratech.azuratime.features.student.ui.form

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.student.domain.model.StudentProfile

object StudentFormPreviewMocks {
    fun initial(): StudentFormUiState = StudentFormUiState(
        availableClasses = listOf(
            ClassModel("cls_1", "sch_1", "10-A", "10", null, 0, System.currentTimeMillis()),
            ClassModel("cls_2", "sch_1", "10-B", "10", null, 0, System.currentTimeMillis()),
        ),
    )

    fun photoCapturing(): StudentFormUiState = initial().copy(
        isCapturingPhoto = true,
        profile = StudentProfile(
            studentId = "STU-123",
            name = "John Doe",
            schoolId = "sch_1",
        ),
    )

    fun biometricScanning(): StudentFormUiState = initial().copy(
        biometricStatus = BiometricStatus.Scanning,
        profile = StudentProfile(
            studentId = "STU-123",
            name = "John Doe",
            schoolId = "sch_1",
            classIds = listOf("cls_1"),
        ),
    )

    fun submitting(): StudentFormUiState = initial().copy(
        isSubmitting = true,
        profile = StudentProfile(
            studentId = "STU-123",
            name = "John Doe",
            schoolId = "sch_1",
            classIds = listOf("cls_1"),
            embedding = FloatArray(512) { 0.1f },
        ),
    )

    fun error(): StudentFormUiState = initial().copy(
        error = "Koneksi internet bermasalah. Silakan coba lagi.",
        profile = StudentProfile(
            studentId = "STU-123",
            name = "John Doe",
            schoolId = "sch_1",
            classIds = listOf("cls_1"),
            embedding = FloatArray(512) { 0.1f },
        ),
    )

    fun success(): StudentFormUiState = initial().copy(
        isSubmitted = true,
        profile = StudentProfile(
            studentId = "STU-123",
            name = "John Doe",
            schoolId = "sch_1",
            classIds = listOf("cls_1"),
            embedding = FloatArray(512) { 0.1f },
        ),
    )
}
