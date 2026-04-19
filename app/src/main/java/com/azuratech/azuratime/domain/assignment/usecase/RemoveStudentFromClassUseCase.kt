package com.azuratech.azuratime.domain.assignment.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to remove a student assignment from a class.
 */
class RemoveStudentFromClassUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val faceAssignmentDao = database.faceAssignmentDao()

    suspend operator fun invoke(faceId: String, classId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school"))

        try {
            // 1. Remove from Cloud
            val docId = "${faceId}_${classId}"
            db.collection("schools").document(schoolId)
              .collection("face_assignments").document(docId).delete().await()

            // 2. Remove from Local
            faceAssignmentDao.deleteSpecificAssignment(faceId, classId, schoolId)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    suspend fun removeAll(faceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school"))

        try {
            val assignmentsSnapshot = db.collection("schools").document(schoolId)
              .collection("face_assignments").whereEqualTo("faceId", faceId).get().await()
              
            for (doc in assignmentsSnapshot.documents) {
                doc.reference.delete().await()
            }

            faceAssignmentDao.deleteAllByFace(faceId, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
