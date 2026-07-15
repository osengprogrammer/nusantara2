package com.azuratech.azuratime.features.school.ui.admin

import com.azuratech.azuratime.features.school.domain.model.School

/**
 * 🧬 PENDING SCHOOLS PREVIEW MOCKS (v3.2.0-ai-native)
 */
object PendingSchoolsPreviewMocks {
    fun loading(): PendingSchoolsUiState = PendingSchoolsUiState(isLoading = true)

    fun success(): PendingSchoolsUiState = PendingSchoolsUiState(
        pendingSchools = listOf(
            School(id = "sch_1", name = "SMKN 1 Banyuwangi", timezone = "Asia/Jakarta", status = "PENDING", accountId = "acc_1", createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()),
            School(id = "sch_2", name = "SMKN 2 Banyuwangi", timezone = "Asia/Jakarta", status = "PENDING", accountId = "acc_2", createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()),
        ),
    )

    fun error(): PendingSchoolsUiState = PendingSchoolsUiState(error = "Failed to load school list")
}
