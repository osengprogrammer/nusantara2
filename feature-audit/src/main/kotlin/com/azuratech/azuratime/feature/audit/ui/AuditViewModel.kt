package com.azuratech.azuratime.feature.audit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.api.audit.AuditRepository
import com.azuratech.azuratime.core.api.models.AuditEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<AuditEvent>>(emptyList())
    val events: StateFlow<List<AuditEvent>> = _events

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _events.value = auditRepository.getRecentEvents()
        }
    }
}