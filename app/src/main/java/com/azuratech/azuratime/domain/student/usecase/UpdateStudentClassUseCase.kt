package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.StudentDao
import com.azuratech.azuratime.data.local.FaceDao
import com.azuratech.azuratime.data.local.FaceAssignmentDao
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateStudentClassUseCase @Inject constructor(
    private val studentDao: StudentDao,
    private val faceDao: FaceDao,
    private val faceAssignmentDao: FaceAssignmentDao,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(studentId: String, newClassId: String, schoolId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedSchoolId = schoolId ?: sessionManager.getActiveSchoolId() ?: return@withContext Result.Failure(AppError.BusinessRule("No active school"))

            // 0. Find faceId associated with this studentId
            val face = faceDao.getFaceByStudentId(studentId, resolvedSchoolId)
            val targetFaceId = face?.faceId ?: "STUDENT_$studentId"

            // 1. Update Student Table (Local)
            studentDao.updateClassId(studentId, resolvedSchoolId, newClassId)

            // 2. Update Face Assignment (Local)
            faceAssignmentDao.updateClassForFace(targetFaceId, newClassId, resolvedSchoolId)

            // 3. Sync to Cloud
            db.collection("schools").document(resolvedSchoolId).collection("students").document(studentId)
                .update("classId", newClassId).await()
            
            // Sync assignment to Cloud (Legacy path)
            val docId = "${targetFaceId}_${newClassId}"
            db.collection("schools").document(resolvedSchoolId).collection("face_assignments").document(docId)
                .set(mapOf(
                    "faceId" to targetFaceId,
                    "classId" to newClassId,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )).await()

            println("✅ Updated face_assignments for $studentId via faceId=$targetFaceId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
