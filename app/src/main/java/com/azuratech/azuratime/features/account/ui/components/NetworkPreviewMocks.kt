package com.azuratech.azuratime.features.account.ui.components

import com.azuratech.azuratime.features.account.data.local.AccountEntity

/**
 * 🧬 NETWORK PREVIEW MOCKS (v3.2.0-ai-native)
 */
object NetworkPreviewMocks {
    fun loading(): NetworkUiState = NetworkUiState(isLoading = true)

    fun success(): NetworkUiState = NetworkUiState(
        results = listOf(
            AccountEntity(accountId = "acc_1", name = "Budi Santoso", email = "budi@azura.com", role = "TEACHER"),
            AccountEntity(accountId = "acc_2", name = "Siti Aminah", email = "siti@azura.com", role = "ADMIN"),
        ),
    )

    fun error(): NetworkUiState = NetworkUiState(error = "Gagal mencari akun. Silakan coba lagi.")
}
