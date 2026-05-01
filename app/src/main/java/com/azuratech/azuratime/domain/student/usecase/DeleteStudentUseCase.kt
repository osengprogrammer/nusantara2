package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteStudentUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(studentId: String, faceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Failure(AppError.BusinessRule("No active school"))

            // 1. Local cascading delete
            database.faceAssignmentDao().deleteAssignmentsForFace(faceId, schoolId)
            database.faceDao().deleteFaceById(faceId, schoolId)
            database.studentDao().deleteById(studentId, schoolId)

            // 2. Soft delete in Firestore (master_faces)
            db.collection("schools").document(schoolId).collection("master_faces").document(faceId)
                .set(mapOf(
                    "isActive" to false,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ), SetOptions.merge()).await()

            // 3. Delete from students collection
            db.collection("schools").document(schoolId).collection("students").document(studentId).delete().await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
