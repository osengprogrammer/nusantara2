package com.azuratech.azuratime.features.student.ui.roster

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem

object StudentRosterPreviewMocks {

    fun loading(): StudentRosterUiState = StudentRosterUiState(
        isLoading = true,
    )

    fun populated(): StudentRosterUiState = StudentRosterUiState(
        isLoading = false,
        students = listOf(
            StudentDisplayItem(
                profile = StudentProfile(
                    studentId = "stu_preview_1",
                    studentCode = "STD001",
                    name = "Ahmad Rizki",
                    schoolId = "sch_azura_jkt",
                    classIds = listOf("cls_xii_a", "cls_debate"),
                    faceId = "face_abc123",
                    embedding = null,
                    photoUrl = "https://example.com/ahmad.jpg",
                    syncStatus = SyncStatus.SYNCED,
                    createdAt = System.currentTimeMillis() - 86400000,
                    updatedAt = System.currentTimeMillis(),
                ),
                assignedClassNames = "XII A, Debate",
                isBiometricReady = true,
            ),
            StudentDisplayItem(
                profile = StudentProfile(
                    studentId = "stu_preview_2",
                    studentCode = "STD002",
                    name = "Siti Nurhaliza",
                    schoolId = "sch_azura_jkt",
                    classIds = listOf("cls_xii_b"),
                    faceId = null,
                    embedding = null,
                    photoUrl = null,
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    createdAt = System.currentTimeMillis() - 172800000,
                    updatedAt = System.currentTimeMillis() - 3600000,
                ),
                assignedClassNames = "XII B",
                isBiometricReady = false,
            ),
        ),
        allClasses = listOf(
            ClassModel(
                id = "cls_xii_a",
                name = "XII A",
                schoolId = "sch_azura_jkt",
                grade = "12",
                teacherId = null,
                createdAt = System.currentTimeMillis(),
            ),
            ClassModel(
                id = "cls_xii_b",
                name = "XII B",
                schoolId = "sch_azura_jkt",
                grade = "12",
                teacherId = null,
                createdAt = System.currentTimeMillis(),
            ),
        ),
    )

    fun error(): StudentRosterUiState = StudentRosterUiState(
        error = "Failed to load student roster. Please check your connection.",
    )
}
