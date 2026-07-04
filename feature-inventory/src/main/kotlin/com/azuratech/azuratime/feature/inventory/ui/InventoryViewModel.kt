package com.azuratech.azuratime.feature.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.api.ims.ImsRepository
import com.azuratech.azuratime.core.api.models.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    imsRepository: ImsRepository
) : ViewModel() {

    val items: StateFlow<List<Item>> = imsRepository.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
