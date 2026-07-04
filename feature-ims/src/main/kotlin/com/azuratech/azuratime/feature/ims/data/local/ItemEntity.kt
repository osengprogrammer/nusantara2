package com.azuratech.azuratime.feature.ims.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.azuratech.azuratime.core.api.models.Category
import com.azuratech.azuratime.core.api.models.ProductAttribute

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stockQuantity: Int,
    val category: Category,
    val attributes: ProductAttribute
)
