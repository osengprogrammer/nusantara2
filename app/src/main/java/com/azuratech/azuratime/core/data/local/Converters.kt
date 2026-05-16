package com.azuratech.azuratime.core.data.local

import androidx.room.TypeConverter
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String? {
        if (array == null) return null
        val buffer = ByteBuffer.allocate(array.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (value in array) { buffer.putFloat(value) }
        return Base64.encode(buffer.array())
    }

    @TypeConverter
    fun toFloatArray(base64Str: String?): FloatArray? {
        if (base64Str.isNullOrEmpty()) return null
        return try {
            val bytes = Base64.decode(base64Str)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val array = FloatArray(bytes.size / 4)
            for (i in array.indices) { array[i] = buffer.float }
            array
        } catch (e: Exception) { null }
    }

    @TypeConverter
    fun toLocalDateTimeString(date: LocalDateTime?): String? = date?.toString()

    @TypeConverter
    fun fromLocalDateTimeString(value: String?): LocalDateTime? = value?.let { 
        try { LocalDateTime.parse(it) } catch (e: Exception) { null } 
    }

    @TypeConverter
    fun toLocalDateString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromLocalDateString(value: String?): LocalDate? = value?.let { 
        try { LocalDate.parse(it) } catch (e: Exception) { null } 
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun fromMembershipsMap(map: Map<String, Membership>?): String? {
        if (map == null) return null
        val type = object : TypeToken<Map<String, Membership>>() {}.type
        return gson.toJson(map, type)
    }

    @TypeConverter
    fun toMembershipsMap(value: String?): Map<String, Membership>? {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = object : TypeToken<Map<String, Membership>>() {}.type
        return try { gson.fromJson(value, type) } catch (e: Exception) { emptyMap() }
    }

    @TypeConverter
    fun fromAccessRequestStatus(status: AccessRequestStatus): String = status.name

    @TypeConverter
    fun toAccessRequestStatus(value: String): AccessRequestStatus = AccessRequestStatus.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
