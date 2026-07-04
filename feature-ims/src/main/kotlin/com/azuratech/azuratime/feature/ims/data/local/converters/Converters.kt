package com.azuratech.azuratime.feature.ims.data.local.converters

import androidx.room.TypeConverter
import com.azuratech.azuratime.core.api.models.Category
import com.azuratech.azuratime.core.api.models.ProductAttribute
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromCategory(category: Category): String {
        return gson.toJson(category)
    }

    @TypeConverter
    fun toCategory(categoryString: String): Category {
        return gson.fromJson(categoryString, Category::class.java)
    }

    @TypeConverter
    fun fromProductAttribute(attributes: ProductAttribute?): String? {
        return gson.toJson(attributes)
    }

    @TypeConverter
    fun toProductAttribute(attributesString: String?): ProductAttribute? {
        return if (attributesString == null) null else gson.fromJson(attributesString, ProductAttribute::class.java)
    }
}
