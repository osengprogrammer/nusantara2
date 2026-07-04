package com.azuratech.azuratime.feature.ims.data

import com.azuratech.azuratime.core.api.ims.ImsRepository
import com.azuratech.azuratime.core.api.models.Category
import com.azuratech.azuratime.core.api.models.Item
import com.azuratech.azuratime.core.api.models.ProductAttribute
import com.azuratech.azuratime.feature.ims.data.local.ItemEntity
import com.azuratech.azuratime.feature.ims.data.local.ProductDao
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImsRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ImsRepository {

    private val _items = MutableStateFlow<Map<String, Item>>(emptyMap())

    override fun getAllItems(): Flow<List<Item>> {
        return productDao.getAllItems().map { entities ->
            entities.map { entity ->
                Item(
                    id = entity.id,
                    name = entity.name,
                    stockQuantity = entity.stockQuantity,
                    category = entity.category,
                    attributes = entity.attributes
                )
            }
        }
    }

    override suspend fun getItemById(id: String): Item? {
        // Fallback: return null or implement proper query if needed
        // For now, rely on getAllItems Flow for reading data
        return null
    }

    override suspend fun updateStock(id: String, quantityChange: Int) {
        // Query current data, then update
        // Since we are using Flow, we might need to handle this carefully
        // For now, we assume the DAO update handles persistence and we let Flow emit new data
        productDao.updateStock(id, quantityChange)
    }

    override suspend fun addStock(id: String, amount: Int) {
        updateStock(id, amount)
    }

    override suspend fun reduceStock(id: String, amount: Int) {
        updateStock(id, -amount)
    }

    override suspend fun addItem(item: Item) {
        val entity = ItemEntity(
            id = item.id,
            name = item.name,
            stockQuantity = item.stockQuantity,
            category = item.category,
            attributes = item.attributes
        )
        productDao.insertItem(entity)
    }
}
