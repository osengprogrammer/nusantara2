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
 * Represents the core data of the FaceListScreen.
 */
data class FaceListData(
    val students: List<StudentDisplayItem> = emptyList(),
    val allClasses: List<ClassEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedClassName: String? = null,
    val studentForClassAssignment: FaceEntity? = null,
    val studentForQuickEdit: FaceWithDetails? = null
)

/**
 * Represents the complete state of the FaceListScreen using a sealed class pattern.
 */
sealed class FaceListUiState {
    object Loading : FaceListUiState()
    data class Success(val data: FaceListData) : FaceListUiState()
    data class Error(val message: String) : FaceListUiState()
}
