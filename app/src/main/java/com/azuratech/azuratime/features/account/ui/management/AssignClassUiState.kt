package com.azuratech.azuratime.features.account.ui.management

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.data.local.AccountEntity
import com.azuratech.azuratime.core.domain.model.TeacherAssignment
import com.azuratech.azuratime.core.data.local.SubjectEntity

data class AssignClassUiState(
    val isLoading: Boolean = false,
    val targetAccount: AccountEntity? = null,
    val availableClasses: List<ClassModel> = emptyList(),
    val filteredClasses: List<ClassModel> = emptyList(),
    val availableSubjects: List<SubjectEntity> = emptyList(),
    val selectedClassIds: Set<String> = emptySet(), // 🔥 Track selected classes independently
    val selectedAssignments: List<TeacherAssignment> = emptyList(),
    val isSaving: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null,
)
