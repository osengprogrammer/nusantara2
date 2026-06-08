package com.azuratech.azuratime.features.attendance.domain.repository

import com.azuratech.azuraengine.result.Result

interface BiometricScannerRepository {
    suspend fun getSessionData(email: String, schoolId: String?): Result<Triple<String?, String, String?>>
    suspend fun loadGallery(schoolId: String): Result<List<Pair<String, FloatArray>>>
    suspend fun performMatch(embedding: FloatArray, gallery: List<Pair<String, FloatArray>>): Result<String>
}
