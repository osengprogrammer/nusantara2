package com.azuratech.azuratime.features.student.ui.components

import com.azuratech.azuratime.features.student.domain.model.StudentProfile

/**
 * 🎓 STUDENT DISPLAY ITEM - UI model for list items
 * Maps the domain StudentProfile to a format optimized for the Roster UI.
 */
data class StudentDisplayItem(
    val profile: StudentProfile,
    val assignedClassNames: String,
    val isBiometricReady: Boolean,
)
