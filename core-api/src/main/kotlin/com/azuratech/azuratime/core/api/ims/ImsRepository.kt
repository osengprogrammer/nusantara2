package com.azuratech.azuratime.core.api.ims

import kotlinx.coroutines.flow.Flow
import com.azuratech.azuratime.core.api.models.Item

interface ImsRepository {
    fun getAllItems(): Flow<List<Item>>

    suspend fun getItemById(id: String): Item?

    suspend fun updateStock(id: String, quantityChange: Int)
    suspend fun addStock(id: String, amount: Int)
    suspend fun reduceStock(id: String, amount: Int)
    suspend fun addItem(item: Item)
}
