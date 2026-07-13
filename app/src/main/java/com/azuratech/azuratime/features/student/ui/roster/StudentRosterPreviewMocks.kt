package com.azuratech.azuratime.features.student.ui.roster

import com.azuratech.azuratime.core.ui.components.StudentRosterItem

object StudentRosterPreviewMocks {
    val mockStudents = listOf(
        StudentRosterItem(
            studentId = "1",
            displayName = "Alice Smith",
            studentCode = "STU001",
            assignedClassNames = "Grade 10A",
            isBiometricReady = true,
            currentBalance = 50000.0,
        ),
        StudentRosterItem(
            studentId = "2",
            displayName = "Bob Johnson",
            studentCode = "STU002",
            assignedClassNames = "Grade 10B",
            isBiometricReady = false,
            currentBalance = 0.0,
        ),
    )
}
