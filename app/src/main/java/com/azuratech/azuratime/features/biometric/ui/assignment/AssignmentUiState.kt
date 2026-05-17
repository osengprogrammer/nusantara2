package com.azuratech.azuratime.features.biometric.ui.assignment

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails

/**
 * 🧬 ASSIGNMENT UI STATE (v3.2.0-ai-native)
 */
data class AssignmentUiState(
    val isLoading: Boolean = false,
    val roster: List<StudentBiometricDetails> = emptyList(),
    val assignedClasses: Map<String, List<ClassModel>> = emptyMap(),
    val availableClasses: List<ClassModel> = emptyList(),
    val selectedStudentId: String? = null,
    val error: String? = null,
)
