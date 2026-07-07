package com.azuratech.azuratime.features.payment.ui.history

sealed class PaymentHistoryUiEffect {
    data class ShowToast(val message: String) : PaymentHistoryUiEffect()
}
