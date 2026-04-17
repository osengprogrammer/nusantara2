package com.azuratech.azuratime.ml.matcher

import android.util.Log

object NativeSecurityVault {

    var isNativeReady = false
        private set

    init {
        try {
            System.loadLibrary("azura_secure_engine")
            isNativeReady = true
        } catch (e: UnsatisfiedLinkError) {
            // Failsafe: Prevents the app from crashing if the C++ binary is missing
            Log.e("NativeVault", "CRITICAL: Failed to load C++ engine.", e)
        }
    }

    external fun calculateDistanceNative(live: FloatArray, registered: FloatArray): Float

    external fun verifyMatchNative(distance: Float, threshold: Float): Boolean
}