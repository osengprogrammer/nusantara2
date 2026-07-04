package com.azuratech.azuratime.feature.store.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.api.store.StoreRepository
import com.azuratech.azuratime.core.api.models.AuditEvent
import com.azuratech.azuratime.core.api.audit.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState: StateFlow<CheckoutUiState> = _uiState

    fun processCheckout(itemId: String, quantity: Int) {
        viewModelScope.launch {
            _uiState.value = CheckoutUiState.Loading
            try {
                val success = storeRepository.processSale(itemId, quantity)
                if (success) {
                    // Log successful sale
                    auditRepository.logEvent(
                        AuditEvent(
                            timestamp = System.currentTimeMillis(),
                            action = "SALE",
                            itemId = itemId,
                            status = "SUCCESS"
                        )
                    )
                    _uiState.value = CheckoutUiState.Success("Sale processed successfully!")
                } else {
                    _uiState.value = CheckoutUiState.Error("Insufficient Stock")
                }
            } catch (e: Exception) {
                _uiState.value = CheckoutUiState.Error("An error occurred: ${e.message}")
            }
        }
    }
}
