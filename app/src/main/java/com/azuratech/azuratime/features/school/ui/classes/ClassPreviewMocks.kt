package com.azuratech.azuratime.features.school.ui.classes

import com.azuratech.azuraengine.model.ClassModel

/**
 * 🏰 CLASS PREVIEW MOCKS (v3.2.0-ai-native)
 */
object ClassPreviewMocks {
    fun loading(): ClassUiState = ClassUiState(isLoading = true)

    fun populated(): ClassUiState = ClassUiState(
        classes = listOf(
            ClassModel(
                id = "cls_1",
                schoolId = "sch_1",
                name = "10-IPA-1",
                grade = "10",
                accountId = "user_1",
                studentCount = 32,
                createdAt = System.currentTimeMillis(),
            ),
            ClassModel(
                id = "cls_2",
                schoolId = "sch_1",
                name = "11-IPS-2",
                grade = "11",
                accountId = "user_2",
                studentCount = 28,
                createdAt = System.currentTimeMillis(),
            ),
        ),
        availableClasses = listOf("10-IPA-1", "10-IPA-2", "11-IPS-1", "11-IPS-2"),
    )

    fun error(): ClassUiState = ClassUiState(
        error = "Waduh, koneksi ke server sekolah lagi bapuk, Lur.",
    )
}
