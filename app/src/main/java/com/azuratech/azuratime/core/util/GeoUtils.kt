package com.azuratech.azuratime.core.util

import kotlin.math.*

/**
 * 🌍 GEO UTILS (v3.2.1-ai-native)
 * Provides mathematical functions for geographical calculations.
 */
object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the distance between two points on Earth using the Haversine formula.
     * Returns distance in meters.
     */
    fun calculateDistanceInMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }
}
