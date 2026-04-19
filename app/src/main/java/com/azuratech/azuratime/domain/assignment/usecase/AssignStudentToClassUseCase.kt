package com.azuratech.azuratime.domain.assignment.usecase

import android.util.Log
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to assign a student (face) to a class.
 * Follows Cloud-Push-First (Legacy Repo Pattern) but can be modernized.
 */
class AssignStudentToClassUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val faceAssignmentDao = database.faceAssignmentDao()

    suspend operator fun invoke(faceId: String, classId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school"))

        val assignment = FaceAssignmentEntity(
            faceId = faceId,
            classId = classId,
            schoolId = schoolId,
            isSynced = false
        )

        try {
            // 1. Push to Cloud
            val docId = "${faceId}_${classId}"
            val data = hashMapOf(
                "faceId" to faceId, 
                "classId" to classId, 
                "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            db.collection("schools").document(schoolId).collection("face_assignments")
                .document(docId).set(data, SetOptions.merge()).await()
            
            // 2. Local Save with synced status
            faceAssignmentDao.insertAssignment(assignment.copy(isSynced = true))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("AssignStudentToClass", "❌ Gagal sync assignment: ${e.message}")
            // Return failure to UI as per legacy repo requirements
            Result.Failure(AppError.Network("Gagal sinkronisasi ke server. Periksa koneksi internet."))
        }
    }
}
