package com.azuratech.azuratime.core.api.store

import com.azuratech.azuratime.core.api.models.Sale

interface StoreRepository {
    suspend fun processSale(itemId: String, quantity: Int): Boolean
    suspend fun getRecentSales(): List<Sale>
}
