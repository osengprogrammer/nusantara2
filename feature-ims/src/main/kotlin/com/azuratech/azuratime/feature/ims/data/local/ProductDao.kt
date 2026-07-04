package com.azuratech.azuratime.feature.ims.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Query("UPDATE items SET stockQuantity = stockQuantity + :change WHERE id = :id")
    suspend fun updateStock(id: String, change: Int)
}
