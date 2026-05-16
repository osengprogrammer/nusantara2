package com.azuratech.azuratime.features.biometric.data.remote

import android.util.Log
import com.azuratech.azuratime.features.biometric.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : FaceRemoteDataSource {

    private fun getTenantRef(schoolId: String) = db.collection("schools").document(schoolId)

    override suspend fun getFaceUpdates(schoolId: String, lastSync: Long): Result<List<Pair<BiometricFaceEntity, Boolean>>> {
        return try {
            val lastTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSync))
            
            val snapshot = getTenantRef(schoolId).collection("master_faces")
                .whereGreaterThan("lastUpdated", lastTimestamp).get().await()

            val updatedData = snapshot.documents.mapNotNull { doc ->
                try {
                    val embedding = (doc.get("embedding") as? List<*>)?.map { (it as Number).toFloat() }?.toFloatArray()
                    val entity = BiometricFaceEntity(
                        studentId = doc.id,
                        schoolId = schoolId,
                        name = doc.getString("name") ?: "",
                        embedding = embedding,
                        photoUrl = doc.getString("photoUrl"),
                        isSynced = true
                    )
                    Pair(entity, doc.getBoolean("isActive") ?: true)
                } catch (e: Exception) { null }
            }.toMutableList()

            Result.Success(updatedData)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun uploadFacePhoto(schoolId: String, studentId: String, imageBytes: ByteArray): Result<String?> {
        return try {
            val ref = storage.reference.child("schools/$schoolId/faces/$studentId.jpg")
            ref.putBytes(imageBytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun bulkSyncFaces(schoolId: String, students: List<BiometricFaceEntity>): Result<Unit> {
        return try {
            if (students.isEmpty()) return Result.Success(Unit)
            students.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { student ->
                    val safePhotoUrl = if (student.photoUrl?.startsWith("http") == true) student.photoUrl else null
                    val data = hashMapOf(
                        "faceId" to student.studentId,
                        "name" to student.name,
                        "photoUrl" to safePhotoUrl,
                        "embedding" to student.embedding?.toList(),
                        "isActive" to true,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                    batch.set(getTenantRef(schoolId).collection("master_faces").document(student.studentId), data, SetOptions.merge())
                }
                batch.commit().await()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncFaceAssignment(assignment: FaceAssignmentEntity): Result<Unit> {
        return try {
            val docId = "${assignment.studentId}_${assignment.classId}"
            val data = hashMapOf(
                "faceId" to assignment.studentId,
                "classId" to assignment.classId,
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            getTenantRef(assignment.schoolId).collection("face_assignments").document(docId).set(data, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteStudent(studentId: String, schoolId: String, classIds: List<String>): Result<Unit> {
        return try {
            val softDeleteData = mapOf(
                "isActive" to false,
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            getTenantRef(schoolId).collection("master_faces").document(studentId)
                .set(softDeleteData, SetOptions.merge()).await()
            
            val tenantCollections = listOf("master_faces", "faces")
            for (coll in tenantCollections) {
                try {
                    getTenantRef(schoolId).collection(coll).document(studentId).delete().await()
                } catch (e: Exception) {
                    Log.e("FaceRemoteDataSource", "Gagal hapus di Cloud (Tenant $coll): ${e.message}")
                }
            }

            try {
                classIds.forEach { classId ->
                    val docId = "${studentId}_$classId"
                    getTenantRef(schoolId).collection("face_assignments").document(docId).delete().await()
                }
                
                val assignmentSnapshot = getTenantRef(schoolId).collection("face_assignments")
                    .whereEqualTo("faceId", studentId).get().await()
                for (doc in assignmentSnapshot.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.e("FaceRemoteDataSource", "Gagal hapus assignments di Cloud: ${e.message}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
