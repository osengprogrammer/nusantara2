package com.azuratech.azuratime.features.dashboard.ui

import com.azuratech.azuratime.features.account.data.local.AccountEntity

object DashboardPreviewMocks {
    fun loading(): DashboardUiState = DashboardUiState(isLoading = true)

    fun empty(): DashboardUiState = DashboardUiState(isReady = true)

    fun success(): DashboardUiState = DashboardUiState(
        isReady = true,
        user = AccountEntity(
            accountId = "acc_1",
            email = "admin@azuratech.com",
            name = "Azura Admin",
            activeSchoolId = "sch_1",
            activeClassId = "cls_1",
        ),
        currentRole = "ADMIN",
        isApproved = true,
        totalStudents = 120,
        recentRecords = emptyList(),
    )

    fun error(): DashboardUiState = DashboardUiState(error = "Gagal memuat dashboard")
}
