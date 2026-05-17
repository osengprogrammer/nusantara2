package com.azuratech.azuratime.features.attendance.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.ml.matcher.NativeSecurityVault
import com.azuratech.azuratime.core.data.local.BiometricCache
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricScannerRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore,
) {
    private val biometricDao = database.biometricDao()
    private val classDao = database.classDao()
    private val accountDao = database.accountDao()

    suspend fun getSessionData(email: String, schoolId: String?): Triple<String?, String, String?> = withContext(Dispatchers.IO) {
        val account = email.let { accountDao.getAccountByEmail(it) }
        val classId = account?.activeClassId
        val resolvedSchoolId = schoolId ?: account?.activeSchoolId

        val className = if (classId != null) {
            classDao.getClassById(classId)?.name ?: "Umum"
        } else {
            "Umum"
        }

        Triple(classId, className, resolvedSchoolId)
    }

    suspend fun loadGallery(schoolId: String): List<Pair<String, FloatArray>> = withContext(Dispatchers.IO) {
        Log.d("AZURA_SCAN", "🔄 Loading biometric gallery for school: $schoolId")
        val result = BiometricCache.load(application, schoolId)
        Log.d("AZURA_SCAN", "✅ Loaded ${result.size} biometrics from Cache/DB")
        return@withContext result
    }

    suspend fun performMatch(embedding: FloatArray, gallery: List<Pair<String, FloatArray>>): String? = withContext(Dispatchers.Default) {
        if (gallery.isEmpty()) return@withContext null

        var bestMatchId: String? = null
        var minDistance = 0.8f // FaceNet Threshold

        for (item in gallery) {
            val distance = NativeSecurityVault.calculateDistanceNative(embedding, item.second)
            if (distance < minDistance) {
                minDistance = distance
                bestMatchId = item.first
            }
        }

        return@withContext if (minDistance < 0.8f) bestMatchId else null
    }
}
