package com.azuratech.azuratime.core.api.models

data class Sale(
    val id: String,
    val itemId: String,
    val quantity: Int,
    val timestamp: Long,
)
