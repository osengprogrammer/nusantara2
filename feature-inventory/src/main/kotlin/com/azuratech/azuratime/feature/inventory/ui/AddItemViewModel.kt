package com.azuratech.azuratime.feature.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.api.ims.ImsRepository
import com.azuratech.azuratime.core.api.models.Category
import com.azuratech.azuratime.core.api.models.Item
import com.azuratech.azuratime.core.api.models.ProductAttribute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val repository: ImsRepository
) : ViewModel() {

    fun addItem(
        name: String,
        categoryName: String = "General",
        stockQuantity: Int,
        size: String? = null,
        color: String? = null
    ) {
        if (name.isBlank() || stockQuantity < 0) return

        val category = Category(id = UUID.randomUUID().toString(), name = categoryName)
        val attributes = ProductAttribute(size = size, color = color)
        val item = Item(
            id = UUID.randomUUID().toString(),
            name = name,
            stockQuantity = stockQuantity,
            category = category,
            attributes = attributes
        )

        viewModelScope.launch {
            repository.addItem(item)
        }
    }
}