package com.azuratech.azuratime.feature.store.data

import com.azuratech.azuratime.core.api.ims.ImsRepository
import com.azuratech.azuratime.core.api.models.Sale
import com.azuratech.azuratime.core.api.store.StoreRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val imsRepository: ImsRepository
) : StoreRepository {

    private val sales = mutableListOf<Sale>()

    override suspend fun processSale(itemId: String, quantity: Int): Boolean {
        // Fetch current item to check stock
        val item = imsRepository.getItemById(itemId) ?: return false
        
        if (item.stockQuantity < quantity) {
            return false
        }
        
        // Try to reduce stock
        imsRepository.reduceStock(itemId, quantity)
        
        // Log sale (simulated)
        sales.add(Sale(System.currentTimeMillis().toString(), itemId, quantity, System.currentTimeMillis()))
        return true
    }

    override suspend fun getRecentSales(): List<Sale> {
        return sales
    }
}
