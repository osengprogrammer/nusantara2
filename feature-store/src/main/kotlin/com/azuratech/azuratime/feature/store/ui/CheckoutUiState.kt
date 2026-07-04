package com.azuratech.azuratime.feature.store.ui

sealed class CheckoutUiState {
    object Loading : CheckoutUiState()
    data class Success(val message: String) : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}
