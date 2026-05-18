package com.azuratech.azuratime.features.account.ui.components

import com.azuratech.azuratime.features.account.data.local.AccountEntity

/**
 * 🚥 NETWORK UI STATE (v3.2.0-ai-native)
 */
data class NetworkUiState(
    val searchQuery: String = "",
    val results: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSendingRequest: Boolean = false,
)
