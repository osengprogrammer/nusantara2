package com.azuratech.azuratime.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 🛰️ LOCATION PROVIDER (v3.2.1-ai-native)
 * Wrapper for FusedLocationProviderClient to fetch current GPS coordinates.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Fetches the current location of the device using the modern CurrentLocationRequest API.
     * Note: Assumes ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<Location> = suspendCancellableCoroutine { continuation ->
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(Result.Success(location))
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            continuation.resume(Result.Success(lastLocation))
                        } else {
                            continuation.resume(
                                Result.Failure(AppError.Unknown("Unable to determine current location. GPS might be disabled.")),
                            )
                        }
                    }.addOnFailureListener { e ->
                        continuation.resume(Result.Failure(AppError.Unknown(e.message)))
                    }
                }
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.Failure(AppError.Unknown(e.message)))
            }
    }
}
