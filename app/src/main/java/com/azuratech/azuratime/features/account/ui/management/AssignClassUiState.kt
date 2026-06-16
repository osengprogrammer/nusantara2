package com.azuratech.azuratime.features.account.ui.management

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import com.azuratech.azuratime.features.session.data.local.SubjectEntity

data class AssignClassUiState(
    val isLoading: Boolean = false,
    val targetAccount: AccountEntity? = null,
    val availableClasses: List<ClassModel> = emptyList(),
    val filteredClasses: List<ClassModel> = emptyList(), // 🔥 Supporting search
    val availableSubjects: List<SubjectEntity> = emptyList(),
    val selectedAssignments: List<TeacherAssignment> = emptyList(),
    val isSaving: Boolean = false,
    val searchQuery: String = "", // 🔥 Supporting search
    val error: String? = null,
)
