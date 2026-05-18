package com.azuratech.azuratime.features.attendance.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.data.local.BiometricCache
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.attendance.domain.repository.BiometricScannerRepository
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.ml.matcher.FaceEngine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricScannerRepositoryImpl @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore,
) : BiometricScannerRepository {
    private val biometricDao = database.biometricDao()
    private val classDao = database.classDao()
    private val accountDao = database.accountDao()

    override suspend fun getSessionData(email: String, schoolId: String?): Result<Triple<String?, String, String?>> = withContext(Dispatchers.IO) {
        try {
            val account = email.let { accountDao.getAccountByEmail(it) }
            val classId = account?.activeClassId
            val resolvedSchoolId = schoolId ?: account?.activeSchoolId

            val className = if (classId != null) {
                classDao.getClassById(classId)?.name ?: "Umum"
            } else {
                "Umum"
            }

            Result.Success(Triple(classId, className, resolvedSchoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun loadGallery(schoolId: String): Result<List<Pair<String, FloatArray>>> = withContext(Dispatchers.IO) {
        try {
            Log.d("AZURA_SCAN", "🔄 Loading biometric gallery for school: $schoolId")
            val result = BiometricCache.load(application, schoolId)
            Log.d("AZURA_SCAN", "✅ Loaded ${result.size} biometrics from Cache/DB")
            Result.Success(result)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun performMatch(embedding: FloatArray, gallery: List<Pair<String, FloatArray>>): Result<String?> = withContext(Dispatchers.Default) {
        try {
            if (gallery.isEmpty()) return@withContext Result.Success(null)

            // 🔥 FIX: Use centralized FaceEngine with stricter thresholds (0.35f)
            val matchResult = FaceEngine.findBestMatch(embedding, gallery)

            val studentId = when (matchResult) {
                is FaceEngine.MatchResult.Success -> matchResult.name
                else -> null
            }

            Result.Success(studentId)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
