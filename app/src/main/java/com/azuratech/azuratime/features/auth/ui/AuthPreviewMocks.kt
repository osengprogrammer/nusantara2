package com.azuratech.azuratime.features.auth.ui

/**
 * 🔐 AUTH PREVIEW MOCKS
 */
object AuthPreviewMocks {
    fun idle(): AuthUiState = AuthUiState()

    fun loading(): AuthUiState = AuthUiState(
        isLoading = true,
    )

    fun googleSigning(): AuthUiState = AuthUiState(
        isLoading = true,
        isGoogleSigning = true,
    )

    fun error(): AuthUiState = AuthUiState(
        error = "Gagal menghubungkan ke server Google. Periksa koneksi Anda.",
    )

    fun success(): AuthUiState = AuthUiState(
        authStatus = AuthStatus.LoggedIn,
        accountEmail = "osengprogrammer@gmail.com",
        accountRole = "ADMIN",
    )
}
