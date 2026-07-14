package com.azuratech.azuratime.features.biometric.ui.assignment

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity

/**
 * 🧬 ASSIGNMENT PREVIEW MOCKS (v3.2.0-ai-native)
 */
object AssignmentPreviewMocks {
    fun loading(): AssignmentUiState = AssignmentUiState(isLoading = true)

    fun success(): AssignmentUiState {
        val std1 = "std_1"
        val std2 = "std_2"
        val cls1 = ClassModel(id = "cls_1", schoolId = "sch_1", name = "Kelas 10A", grade = "10", studentCount = 20, accountId = "acc_1", createdAt = 0L)
        val cls2 = ClassModel(id = "cls_2", schoolId = "sch_1", name = "Kelas 10B", grade = "10", studentCount = 18, accountId = "acc_1", createdAt = 0L)

        return AssignmentUiState(
            roster = listOf(
                StudentBiometricDetails(
                    biometric = StudentBiometricEntity(studentId = std1, name = "Budi Santoso", schoolId = "sch_1"),
                    className = "Kelas 10A",
                    classId = "cls_1",
                    classIds = listOf("cls_1"),
                ),
                StudentBiometricDetails(
                    biometric = StudentBiometricEntity(studentId = std2, name = "Siti Aminah", schoolId = "sch_1"),
                    className = null,
                    classId = null,
                    classIds = emptyList(),
                ),
            ),
            availableClasses = listOf(cls1, cls2),
            assignedClasses = mapOf(
                std1 to listOf(cls1),
            ),
        )
    }

    fun error(): AssignmentUiState = AssignmentUiState(error = "Failed to load assignment data")
}
