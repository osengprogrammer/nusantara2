package com.azuratech.azuratime.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.core.util.AttendanceService
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 SCANNER REPOSITORY
 * Handles real-time face matching and attendance stamping.
 * 🔥 Strictly UseCase-agnostic for IMS Migration.
 */
@Singleton
class ScannerRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val faceDao = database.faceDao()
    private val userDao = database.userDao()
    private val classDao = database.classDao()

    /**
     * Fetches user context for scanning.
     * @param schoolId Context-validated schoolId passed from ViewModel/UseCase.
     */
    suspend fun getSessionData(email: String, schoolId: String?): Triple<String?, String, String?> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByEmail(email)
        val _classId = user?.activeClassId
        val _schoolId = schoolId ?: user?.activeSchoolId ?: sessionManager.getActiveSchoolId()

        val className = _classId?.let { classDao.getClassById(it)?.name } ?: "General Scan"
        Log.d("AZURA_SCAN", "Session Data: user=$email, school=$_schoolId, class=$_classId")
        Triple(_classId, className, _schoolId)
    }

    suspend fun loadGallery(schoolId: String): List<Pair<String, FloatArray>> = withContext(Dispatchers.IO) {
        Log.d("AZURA_SCAN", "📥 Loading gallery for schoolId: '$schoolId'")
        if (schoolId.isBlank()) {
            Log.e("AZURA_SCAN", "❌ Cannot load gallery: schoolId is empty!")
            return@withContext emptyList()
        }
        val result = FaceCache.load(application, schoolId)
        Log.d("AZURA_SCAN", "✅ Loaded ${result.size} faces from Cache/DB")
        return@withContext result
    }

    suspend fun performMatch(embedding: FloatArray, gallery: List<Pair<String, FloatArray>>): String? = withContext(Dispatchers.Default) {
        if (gallery.isEmpty()) {
            Log.w("AZURA_SCAN", "⚠️ performMatch called with empty gallery")
            return@withContext null
        }
        val matchResult = com.azuratech.azuratime.ml.matcher.FaceEngine.findBestMatch(embedding, gallery)
        return@withContext when (matchResult) {
            is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.Success -> {
                Log.d("AZURA_SCAN", "🎯 Match Found: ${matchResult.name} (dist: ${matchResult.distance})")
                matchResult.name
            }
            is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.DuplicateFound -> matchResult.existingName
            is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.NoMatch -> {
                Log.d("AZURA_SCAN", "Match Failed: No similarity found")
                null
            }
        }
    }
}
