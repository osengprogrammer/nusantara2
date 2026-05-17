package com.azuratech.azuratime.features.school.ui.list

import com.azuratech.azuraengine.model.School

object SchoolPreviewMocks {
    fun loading(): SchoolUiState = SchoolUiState(isLoading = true)

    fun success(): SchoolUiState = SchoolUiState(
        schools = listOf(
            School(
                id = "sch_1",
                accountId = "acc_1",
                name = "SMK Budi Utomo",
                timezone = "Asia/Jakarta",
                status = "ACTIVE",
                createdAt = System.currentTimeMillis() - 86400000L,
                updatedAt = System.currentTimeMillis(),
            ),
            School(
                id = "sch_2",
                accountId = "acc_1",
                name = "SMA Negeri 1",
                timezone = "Asia/Jakarta",
                status = "ACTIVE",
                createdAt = System.currentTimeMillis() - 172800000L,
                updatedAt = System.currentTimeMillis(),
            ),
        ),
        activeSchoolId = "sch_1",
        accountId = "acc_1",
    )

    fun error(): SchoolUiState = SchoolUiState(error = "Gagal memuat data sekolah")
}
