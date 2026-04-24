package com.azuratech.azuratime.domain.assignment.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to synchronize face-class assignments from Firestore.
 */
class SyncAssignmentsUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val faceAssignmentDao = database.faceAssignmentDao()

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)

        val assignments = mutableListOf<FaceAssignmentEntity>()

        try {
            // 1. Fetch from Tenant
            val tenantSnapshot = db.collection("schools").document(schoolId)
                .collection("face_assignments").get().await()
            
            assignments.addAll(tenantSnapshot.documents.mapNotNull { doc ->
                val faceId = doc.getString("faceId") ?: doc.id.split("_").firstOrNull()
                val classId = doc.getString("classId") ?: doc.id.split("_").getOrNull(1)
                
                if (faceId != null && classId != null) {
                    val correctedFaceId = if (faceId.contains("--")) faceId else "${classId}--${faceId}"
                    FaceAssignmentEntity(correctedFaceId, classId, schoolId, true)
                } else null
            })

            // 2. Fallback to Root
            try {
                val rootSnapshot = db.collection("face_assignments").get().await()
                rootSnapshot.documents.forEach { doc ->
                    val faceId = doc.getString("faceId") ?: doc.id.split("_").firstOrNull()
                    val classId = doc.getString("classId") ?: doc.id.split("_").getOrNull(1)
                    
                    if (faceId != null && classId != null) {
                        val correctedFaceId = if (faceId.contains("--")) faceId else "${classId}--${faceId}"
                        if (assignments.none { it.faceId == correctedFaceId && it.classId == classId }) {
                            assignments.add(FaceAssignmentEntity(correctedFaceId, classId, schoolId, true))
                        }
                    }
                }
            } catch (e: Exception) {
                println("[SyncAssignments] Root fetch failed: ${e.message}")
            }

            // 3. Auto-Healing
            if (assignments.isEmpty()) {
                val allFaces = database.faceDao().getAllFacesForScanningList(schoolId)
                for (face in allFaces) {
                    if (face.faceId.contains("--")) {
                        val reconstructedClassId = face.faceId.split("--").firstOrNull()
                        if (reconstructedClassId != null) {
                            assignments.add(FaceAssignmentEntity(face.faceId, reconstructedClassId, schoolId, true))
                        }
                    }
                }
            }

            // 4. Save to Room
            assignments.forEach { 
                try { faceAssignmentDao.insertAssignment(it) } catch (e: Exception) {}
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
