package com.azuratech.azuratime.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.ui.checkin.AttendanceService
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

sealed class CheckInResult {
    data class Success(val name: String, val greeting: String) : CheckInResult()
    data class AlreadyCheckedIn(val name: String) : CheckInResult()
    object Unregistered : CheckInResult()
    data class Rejected(val name: String, val reason: String) : CheckInResult()
}

/**
 * 🏰 SCANNER REPOSITORY
 * Handles real-time face matching and attendance stamping.
 * 🔥 Sudah menggunakan Hilt Dependency Injection.
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
    private val checkInRecordDao = database.checkInRecordDao()
    private val classDao = database.classDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""
    private val checkInTimestamps = mutableMapOf<String, Long>()

    suspend fun getSessionData(email: String): Pair<String?, String> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByEmail(email)
        val _classId = user?.activeClassId

        val className = _classId?.let { classDao.getClassById(it)?.name } ?: "General Scan"
        Pair(_classId, className)
    }

    suspend fun loadGallery(): List<Pair<String, FloatArray>> = withContext(Dispatchers.IO) {
        return@withContext FaceCache.load(application, schoolId)
    }

    suspend fun performMatch(embedding: FloatArray, gallery: List<Pair<String, FloatArray>>): String? = withContext(Dispatchers.Default) {
        val matchResult = com.azuratech.azuratime.ml.matcher.FaceEngine.findBestMatch(embedding, gallery)
        return@withContext when (matchResult) {
            is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.Success -> matchResult.name
            is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.DuplicateFound -> matchResult.existingName
            is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.NoMatch -> null
        }
    }

    suspend fun processCheckIn(faceId: String, teacherEmail: String, classId: String?, className: String): CheckInResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastCheckIn = checkInTimestamps[faceId] ?: 0L

        if (now - lastCheckIn > 60_000) {
            val face = faceDao.getFaceById(faceId, schoolId) ?: return@withContext CheckInResult.Unregistered

            if (classId != null) {
                val studentClasses = faceAssignmentDao.getClassIdsForFace(faceId, schoolId).firstOrNull() ?: emptyList()
                if (!studentClasses.contains(classId)) {
                    return@withContext CheckInResult.Rejected(face.name, "Bukan kelas ini.")
                }
            }

            checkInTimestamps[faceId] = now

            val record = AttendanceService.createRecord(face, teacherEmail, classId ?: "", className).copy(schoolId = schoolId)

            checkInRecordDao.insert(record)
            try {
                syncRecordToCloud(record)
            } catch (e: Exception) {
                Log.e("ScannerRepo", "Mode Offline - Akan disinkronkan nanti")
            }

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greet = when (hour) { in 5..11 -> "Selamat pagi"; in 12..17 -> "Selamat siang"; else -> "Selamat malam" }
            return@withContext CheckInResult.Success(face.name, greet)
        } else {
            val face = faceDao.getFaceById(faceId, schoolId)
            return@withContext CheckInResult.AlreadyCheckedIn(face?.name ?: "User")
        }
    }

    private suspend fun syncRecordToCloud(record: CheckInRecordEntity) {
        val batch = db.batch()
        val data = record.toFirestoreMap()

        batch.set(db.collection("schools").document(record.schoolId).collection("checkin_records").document(record.id), data, com.google.firebase.firestore.SetOptions.merge())
        batch.set(db.collection("attendance_logs").document(record.id), data, com.google.firebase.firestore.SetOptions.merge())

        batch.commit().await()
    }
}
