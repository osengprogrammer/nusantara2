package com.azuratech.azuratime.features.account.ui.components

import com.azuratech.azuratime.features.account.data.local.AccountEntity

/**
 * 🧬 FOLLOWING PREVIEW MOCKS (v3.2.0-ai-native)
 */
object FollowingPreviewMocks {
    fun loading(): FollowingUiState = FollowingUiState(isLoading = true)

    fun success(): FollowingUiState = FollowingUiState(
        results = listOf(
            AccountEntity(accountId = "acc_1", name = "Budi Santoso", email = "budi@azura.com", role = "SUPERVISOR"),
            AccountEntity(accountId = "acc_2", name = "Siti Aminah", email = "siti@azura.com", role = "ADMIN"),
        ),
    )

    fun error(): FollowingUiState = FollowingUiState(error = "Gagal mencari akun. Silakan coba lagi.")
}
