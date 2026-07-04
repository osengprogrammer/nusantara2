package com.azuratech.azuratime.core.api.models

data class Item(
    val id: String,
    val name: String,
    val stockQuantity: Int,
    val category: Category,
    val attributes: ProductAttribute,
)
