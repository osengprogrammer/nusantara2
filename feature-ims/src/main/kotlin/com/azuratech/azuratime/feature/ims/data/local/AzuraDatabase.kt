package com.azuratech.azuratime.feature.ims.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.azuratech.azuratime.feature.ims.data.local.converters.Converters

@Database(entities = [ItemEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AzuraDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
