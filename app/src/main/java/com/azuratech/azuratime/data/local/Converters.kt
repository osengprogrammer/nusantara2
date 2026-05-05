package com.azuratech.azuratime.data.local

import androidx.room.TypeConverter
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

/**
 * 🛠️ AZURA CONVERTERS - THE DATABASE INTERPRETER
 * Handling complex data types for ML Embeddings, Multi-Tenant Maps, ISO Dates, and Friendships.
 */
@OptIn(ExperimentalEncodingApi::class)
class Converters {
    private val gson = Gson()

    // =====================================================
    // 🧬 FLOATARRAY (ML Biometric Embeddings)
    // Using Base64 + ByteBuffer for maximum performance and 100% precision.
    // =====================================================

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String? {
        if (array == null) return null
        
        // 1 Float = 4 Bytes. For a 512D embedding, this is exactly 2048 bytes.
        val buffer = ByteBuffer.allocate(array.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (value in array) {
            buffer.putFloat(value)
        }
        
        return Base64.encode(buffer.array())
    }

    @TypeConverter
    fun toFloatArray(base64Str: String?): FloatArray? {
        if (base64Str.isNullOrEmpty()) return null
        
        return try {
            val bytes = Base64.decode(base64Str)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            
            val array = FloatArray(bytes.size / 4)
            for (i in array.indices) {
                array[i] = buffer.float
            }
            array
        } catch (e: Exception) {
            // Fallback for any old JSON-formatted data to prevent crashes
            try {
                val type = object : TypeToken<FloatArray>() {}.type
                gson.fromJson(base64Str, type)
            } catch (ex: Exception) {
                null
            }
        }
    }

    // =====================================================
    // ⏰ DATE & TIME (Safety-First ISO-8601 & Timestamps)
    // =====================================================
    
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

    // 🔥 Safety Net: Handle cases where data comes down as Long (Epoch)
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { 
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) 
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    // =====================================================
    // 🏫 MEMBERSHIP MAP (Multi-Tenant Hub)
    // Stores schoolId -> Membership object as a JSON String
    // =====================================================

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
        return try {
            gson.fromJson(value, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // =====================================================
    // 🤝 FRIENDSHIP MAP (Seduluran Hub)
    // Stores friendUserId -> FriendConnection object as JSON
    // =====================================================

    @TypeConverter
    fun fromFriendsMap(map: Map<String, FriendConnection>?): String? {
        if (map == null) return null
        val type = object : TypeToken<Map<String, FriendConnection>>() {}.type
        return gson.toJson(map, type)
    }

    @TypeConverter
    fun toFriendsMap(value: String?): Map<String, FriendConnection>? {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = object : TypeToken<Map<String, FriendConnection>>() {}.type
        return try {
            gson.fromJson(value, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // =====================================================
    // 🎫 ENUMS (Status & Sync)
    // =====================================================

    @TypeConverter
    fun fromAccessRequestStatus(status: com.azuratech.azuratime.domain.model.AccessRequestStatus): String = status.name

    @TypeConverter
    fun toAccessRequestStatus(value: String): com.azuratech.azuratime.domain.model.AccessRequestStatus = 
        com.azuratech.azuratime.domain.model.AccessRequestStatus.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(status: com.azuratech.azuratime.domain.model.SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): com.azuratech.azuratime.domain.model.SyncStatus = 
        com.azuratech.azuratime.domain.model.SyncStatus.valueOf(value)
}