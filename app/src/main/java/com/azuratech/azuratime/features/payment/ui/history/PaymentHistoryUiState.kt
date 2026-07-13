package com.azuratech.azuratime.features.payment.ui.history

import com.azuratech.azuratime.features.payment.data.local.PaymentEntity
import com.azuratech.azuratime.core.ui.components.StudentRosterItem

data class PaymentHistoryUiState(
    val isLoading: Boolean = false,
    val isPerformingAction: Boolean = false,
    val selectedStudentId: String? = null,
    val selectedStudentName: String? = null,
    val selectedStudentCode: String? = null,
    val selectedStudentBalance: Double = 0.0,
    val payments: List<PaymentEntity> = emptyList(),
    val students: List<StudentRosterItem> = emptyList(),
    val searchQuery: String = "",
)
