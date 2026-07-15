package com.azuratech.azuratime.features.account.ui.management

import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.features.account.domain.model.AccountProfile

/**
 * 👤 ACCOUNT PREVIEW MOCKS (v3.2.0-ai-native)
 */
object AccountPreviewMocks {
    fun loading(): AccountUiState = AccountUiState(isLoading = true)

    fun populated(
        profile: AccountProfile = AccountProfile(
            accountId = "acc_123",
            email = "admin@azuratech.com",
            name = "Admin Azura",
            role = "ADMIN",
        ),
        classes: List<ClassModel> = listOf(
            ClassModel("cls_1", "sch_1", "Kelas 10-A", "10", "", 0),
            ClassModel("cls_2", "sch_1", "Kelas 11-B", "11", "", 0),
        ),
    ): AccountUiState = AccountUiState(
        accountProfile = profile,
        availableClasses = classes,
        activeClassId = "cls_1",
    )

    fun error(message: String = "Gagal memuat profil. Periksa koneksi internet."): AccountUiState = AccountUiState(
        error = message,
    )
}
