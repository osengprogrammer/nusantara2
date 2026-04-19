package com.azuratech.azuratime.domain.sync.usecase

import android.util.Log
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to refresh all master data (Classes, Faces, Assignments) from cloud.
 */
class SyncMasterDataUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val faceDao = database.faceDao()
    private val classDao = database.classDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    suspend operator fun invoke(): Int = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext 0
        try {
            Log.d("AZURA_SYNC", "🔄 Refreshing Master Data for school: $schoolId")

            val cloudClasses = db.collection("schools").document(schoolId).collection("classes").get().await()
            cloudClasses.documents.map { doc ->
                ClassEntity(
                    id = doc.id,
                    schoolId = schoolId,
                    name = doc.getString("name") ?: "",
                    displayOrder = doc.getLong("displayOrder")?.toInt() ?: 0,
                    isSynced = true
                )
            }.forEach { classDao.insert(it) }

            val cloudFaces = db.collection("schools").document(schoolId).collection("master_faces").get().await()
            cloudFaces.documents.map { doc ->
                FaceEntity(faceId = doc.id, schoolId = schoolId, name = doc.getString("name") ?: "")
            }.forEach { face ->
                faceDao.upsertFace(face)
            }

            val cloudAssignments = db.collection("schools").document(schoolId).collection("face_assignments").get().await()
            cloudAssignments.documents.mapNotNull { doc ->
                val faceId = doc.getString("faceId") ?: return@mapNotNull null
                val classId = doc.getString("classId") ?: return@mapNotNull null
                FaceAssignmentEntity(faceId = faceId, classId = classId, schoolId = schoolId, isSynced = true)
            }.forEach { faceAssignmentDao.insertAssignment(it) }

            Log.d("AZURA_SYNC", "✅ Master Data Sync Complete")
            return@withContext cloudFaces.size()
        } catch (e: Exception) {
            Log.e("AZURA_SYNC", "🚨 Sync Master Data Failed: ${e.message}")
            throw e
        }
    }
}
