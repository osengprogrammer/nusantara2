package com.azuratech.azuratime.features.account.ui.management

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.account.data.local.AccountEntity

data class AssignClassUiState(
    val isLoading: Boolean = false,
    val targetAccount: AccountEntity? = null,
    val availableClasses: List<ClassModel> = emptyList(),
    val selectedClassIds: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
)
