package com.azuratech.azuratime.features.school.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 📍 GPS GEOFENCE ENTITY (v3.2.1-ai-native)
 * Stores geofencing configuration for a school to restrict attendance tracking.
 */
@Entity(
    tableName = "gps_geofences",
    indices = [Index(value = ["schoolId"])],
)
data class GpsGeofenceEntity(
    @PrimaryKey
    val id: String,
    val schoolId: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 100,
    val isActive: Boolean = false,
    val syncStatus: String = "SYNCED",
)
