package com.azuratech.azuratime.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class RegisterResult {
    object Success : RegisterResult()
    data class Duplicate(val name: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}

@Singleton
class FaceRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val sessionManager: SessionManager
) {
    private val faceDao = database.faceDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    private fun getTenantRef(schoolId: String) = db.collection("schools").document(schoolId)

    val allFacesWithDetailsFlow: Flow<List<FaceWithDetails>>
        get() = faceDao.getAllFacesWithDetailsFlow(schoolId)

    val facesForScanningFlow: Flow<List<FaceEntity>>
        get() = faceDao.getAllFacesForScanning(schoolId)

    fun getFacesInClassFlow(classId: String): Flow<List<FaceEntity>> =
        faceAssignmentDao.getFacesByClass(classId, schoolId)

    suspend fun getFaceWithDetails(faceId: String): FaceWithDetails? = withContext(Dispatchers.IO) {
        return@withContext faceDao.getFaceWithDetails(faceId, schoolId)
    }

    suspend fun getAssignmentsForFace(faceId: String): List<String> = withContext(Dispatchers.IO) {
        return@withContext faceAssignmentDao.getClassIdsForFace(faceId, schoolId).firstOrNull() ?: emptyList()
    }

    suspend fun performFaceDeltaSync() = withContext(Dispatchers.IO) {
        val lastSync = sessionManager.getLastFacesSyncTime()
        val lastTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSync))
        val snapshot = getTenantRef(schoolId).collection("master_faces")
            .whereGreaterThan("lastUpdated", lastTimestamp).get().await()

        val updatedData = snapshot.documents.mapNotNull { doc ->
            try {
                val embedding = (doc.get("embedding") as? List<*>)?.map { (it as Number).toFloat() }?.toFloatArray()
                val entity = FaceEntity(
                    faceId = doc.id,
                    schoolId = schoolId,
                    name = doc.getString("name") ?: "",
                    embedding = embedding,
                    photoUrl = doc.getString("photoUrl"),
                    isSynced = true
                )
                Pair(entity, doc.getBoolean("isActive") ?: true)
            } catch (e: Exception) { null }
        }

        if (updatedData.isNotEmpty()) {
            val toUpsert = updatedData.filter { pair -> pair.second }.map { pair -> pair.first }
            val toDelete = updatedData.filter { pair -> !pair.second }.map { pair -> pair.first }

            if (toUpsert.isNotEmpty()) faceDao.upsertAll(toUpsert)

            if (toDelete.isNotEmpty()) {
                for (face in toDelete) {
                    faceDao.deleteFaceById(face.faceId, schoolId)
                }
            }

            FaceCache.refresh(application, schoolId)
            sessionManager.saveLastFacesSyncTime(System.currentTimeMillis())
        }
    }

    suspend fun registerFace(
        inputId: String,
        classId: String,
        name: String,
        embedding: FloatArray,
        photoBitmap: Bitmap?
    ): RegisterResult = withContext(Dispatchers.IO) {
        try {
            performFaceDeltaSync()

            val allEnrolled = faceDao.getAllFacesForScanningList(schoolId)
            val currentGallery = allEnrolled.map { it.name to (it.embedding ?: floatArrayOf()) }

            if (currentGallery.isNotEmpty()) {
                val matchResult = com.azuratech.azuratime.ml.matcher.FaceEngine.findBestMatch(
                    inputEmbedding = embedding,
                    gallery = currentGallery,
                    isRegistrationMode = true
                )
                if (matchResult is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.DuplicateFound) {
                    return@withContext RegisterResult.Duplicate(matchResult.existingName)
                }
            }

            val finalFaceId = if (inputId.contains("--")) inputId else "${classId}--${inputId}"
            val existingFace = faceDao.getFaceById(finalFaceId, schoolId)
            if (existingFace != null) return@withContext RegisterResult.Duplicate(existingFace.name)

            var finalPhotoUrl: String? = photoBitmap?.let {
                PhotoStorageUtils.saveFacePhoto(application, it, finalFaceId)
            }

            photoBitmap?.let { bmp ->
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val cloudUrl = uploadFacePhotoToCloud(schoolId, finalFaceId, stream.toByteArray())
                if (cloudUrl != null) finalPhotoUrl = cloudUrl
            }

            val face = FaceEntity(faceId = finalFaceId, name = name, photoUrl = finalPhotoUrl, embedding = embedding, schoolId = schoolId, isSynced = false)
            faceDao.upsertFace(face)

            val assignment = FaceAssignmentEntity(faceId = finalFaceId, classId = classId, schoolId = schoolId, isSynced = false)
            faceAssignmentDao.insertAssignment(assignment)

            try {
                bulkSyncFacesToCloud(schoolId, listOf(face))
                syncFaceAssignmentToCloud(assignment)
                faceDao.upsertFace(face.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e("AZURA_SYNC", "Gagal sync cloud: ${e.message}")
            }

            FaceCache.refresh(application, schoolId)
            return@withContext RegisterResult.Success

        } catch (e: Exception) {
            return@withContext RegisterResult.Error(e.message ?: "Unknown Error")
        }
    }

    suspend fun deleteFace(face: FaceEntity) = withContext(Dispatchers.IO) {
        face.photoUrl?.let { PhotoStorageUtils.deleteFacePhoto(it) }
        faceDao.deleteFace(face)
        faceAssignmentDao.deleteAllByFace(face.faceId, schoolId)
        FaceCache.refresh(application, schoolId)
    }

    suspend fun updateEmployeeClass(faceId: String, newClassId: String?) = withContext(Dispatchers.IO) {
        faceAssignmentDao.deleteAllByFace(faceId, schoolId)
        if (newClassId != null) {
            val assignment = FaceAssignmentEntity(faceId = faceId, classId = newClassId, schoolId = schoolId, isSynced = false)
            faceAssignmentDao.insertAssignment(assignment)
            try { syncFaceAssignmentToCloud(assignment) } catch (e: Exception) {}
        }
    }

    suspend fun updateFaceBasic(face: FaceEntity) = withContext(Dispatchers.IO) {
        val updatedFace = face.copy(lastUpdated = System.currentTimeMillis(), isSynced = false)
        faceDao.upsertFace(updatedFace)
        try {
            bulkSyncFacesToCloud(schoolId, listOf(updatedFace))
            faceDao.upsertFace(updatedFace.copy(isSynced = true))
        } catch (e: Exception) {}
        FaceCache.refresh(application, schoolId)
    }

    suspend fun updateFaceWithPhoto(face: FaceEntity, photoBitmap: Bitmap?, embedding: FloatArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var finalPhotoUrl = photoBitmap?.let { PhotoStorageUtils.saveFacePhoto(application, it, face.faceId) } ?: face.photoUrl
            
            // Upload to cloud if we have a new bitmap
            if (photoBitmap != null) {
                val stream = java.io.ByteArrayOutputStream()
                photoBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val cloudUrl = uploadFacePhotoToCloud(schoolId, face.faceId, stream.toByteArray())
                if (cloudUrl != null) finalPhotoUrl = cloudUrl
            }

            val updatedFace = face.copy(photoUrl = finalPhotoUrl, embedding = embedding, lastUpdated = System.currentTimeMillis(), isSynced = false)
            faceDao.upsertFace(updatedFace)
            try {
                bulkSyncFacesToCloud(schoolId, listOf(updatedFace))
                faceDao.upsertFace(updatedFace.copy(isSynced = true))
            } catch (e: Exception) {}
            FaceCache.refresh(application, schoolId)
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // =====================================================
    // ☁️ CLOUD OPERATIONS (Migrated from FirestoreManager)
    // =====================================================

    suspend fun uploadFacePhotoToCloud(schoolId: String, faceId: String, imageBytes: ByteArray): String? {
        val ref = storage.reference.child("schools/$schoolId/faces/$faceId.jpg")
        return try {
            ref.putBytes(imageBytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d("AZURA_STORAGE", "UPLOAD SUKSES! URL: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("AZURA_STORAGE", "UPLOAD GAGAL: ${e.message}", e)
            null
        }
    }

    suspend fun bulkSyncFacesToCloud(schoolId: String, faces: List<FaceEntity>) {
        if (faces.isEmpty()) return
        faces.chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { face ->
                val safePhotoUrl = if (face.photoUrl?.startsWith("http") == true) face.photoUrl else null
                val data = hashMapOf(
                    "faceId" to face.faceId,
                    "name" to face.name,
                    "photoUrl" to safePhotoUrl,
                    "embedding" to face.embedding?.toList(),
                    "isActive" to true,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
                batch.set(getTenantRef(schoolId).collection("master_faces").document(face.faceId), data, SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    suspend fun syncFaceAssignmentToCloud(assignment: FaceAssignmentEntity) {
        val docId = "${assignment.faceId}_${assignment.classId}"
        val data = hashMapOf("faceId" to assignment.faceId, "classId" to assignment.classId, "lastUpdated" to FieldValue.serverTimestamp())
        getTenantRef(assignment.schoolId).collection("face_assignments").document(docId).set(data, SetOptions.merge()).await()
    }
}