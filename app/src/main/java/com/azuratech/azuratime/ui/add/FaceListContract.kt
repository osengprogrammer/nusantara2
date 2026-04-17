package com.azuratech.azuratime.ui.add

import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceWithDetails

/**
 * A display-ready data class that acts as a "ViewModel" for each item in the student list.
 * It contains all pre-computed data the UI needs, so the Composable does zero logic.
 */
data class StudentDisplayItem(
    val faceWithDetails: FaceWithDetails,
    val assignedClassNames: String,
    val isBiometricReady: Boolean,
    val assignedClassIds: List<String>
)

/**
 * Represents the complete state of the FaceListScreen.
 */
data class FaceListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedClassName: String? = null,
    val students: List<StudentDisplayItem> = emptyList(),
    val allClasses: List<ClassEntity> = emptyList(),

    // State for driving dialogs
    val studentForClassAssignment: FaceEntity? = null,
    val studentForQuickEdit: FaceWithDetails? = null
)
