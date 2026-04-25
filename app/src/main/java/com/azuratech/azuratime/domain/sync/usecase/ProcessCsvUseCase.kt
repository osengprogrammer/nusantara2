package com.azuratech.azuratime.domain.sync.usecase

import android.app.Application
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.domain.media.BulkPhotoProcessor
import com.azuratech.azuratime.domain.media.PhotoManager
import com.azuratech.azuraengine.model.ProcessResult
import com.azuratech.azuratime.domain.sync.CsvImportUtils
import com.azuratech.azuraengine.sync.CsvStudentData
import com.azuratech.azuraengine.core.ImageProcessor
import com.azuratech.azuratime.ml.matcher.NativeSecurityVault
import com.azuratech.azuratime.ml.recognizer.FaceNetConstants
import com.azuratech.azuratime.ml.utils.PhotoProcessingUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * UseCase to process CSV imports for faces and assignments.
 */
class ProcessCsvUseCase @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val sessionManager: SessionManager,
    private val bulkPhotoProcessor: BulkPhotoProcessor,
    private val photoManager: PhotoManager,
    private val csvImportUtils: CsvImportUtils,
    private val imageProcessor: ImageProcessor
) {
    private val faceDao = database.faceDao()
    private val faceAssignmentDao = database.faceAssignmentDao()
    private val classDao = database.classDao()

    suspend operator fun invoke(uriString: String, type: String): Flow<ProcessResult> = flow {
        val schoolId = sessionManager.getActiveSchoolId() ?: return@flow
        
        emit(ProcessResult("", "", "Syncing", type, "Updating Biometric Database..."))
        performFaceDeltaSync(schoolId)

        val parsedData = csvImportUtils.parseCsvToStudentData(uriString)
        val existingFaces = faceDao.getAllFacesForScanningList(schoolId)

        val newlyRegisteredFaces = mutableListOf<FaceEntity>()

        for (student in parsedData) {
            val (result, newFace) = processStudent(student, type, existingFaces, schoolId)
            if (newFace != null) newlyRegisteredFaces.add(newFace)
            emit(result)
        }

        if (newlyRegisteredFaces.isNotEmpty()) {
            emit(ProcessResult("", "", "Syncing", type, "Uploading ${newlyRegisteredFaces.size} new faces to cloud..."))
            try {
                bulkSyncFacesToCloud(schoolId, newlyRegisteredFaces)
                val syncedFaces = newlyRegisteredFaces.map { it.copy(isSynced = true) }
                faceDao.upsertAll(syncedFaces)
            } catch (e: Exception) {
                println("ERROR: [ProcessCsvUseCase] Bulk sync failed: ${e.message}")
            }
        }
    }

    private suspend fun performFaceDeltaSync(schoolId: String) = withContext(Dispatchers.IO) {
        val lastSync = sessionManager.getLastFacesSyncTime()
        val lastTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSync))
        
        val snapshot = db.collection("schools").document(schoolId).collection("master_faces")
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
            val toUpsert = updatedData.filter { it.second }.map { it.first }
            val toDelete = updatedData.filter { !it.second }.map { it.first }

            if (toUpsert.isNotEmpty()) faceDao.upsertAll(toUpsert)
            if (toDelete.isNotEmpty()) {
                toDelete.forEach { faceDao.deleteFaceById(it.faceId, schoolId) }
            }
            sessionManager.saveLastFacesSyncTime(System.currentTimeMillis())
        }
    }

    private suspend fun processStudent(
        student: CsvStudentData,
        dataType: String,
        existingFaces: List<FaceEntity>,
        schoolId: String
    ): Pair<ProcessResult, FaceEntity?> = withContext(Dispatchers.IO) {
        val inputId = student.faceId.uppercase().trim()
        val existingFace = existingFaces.find { it.faceId.substringBefore("--") == inputId }
        val isRegistered = existingFace != null
        val finalFaceId = existingFace?.faceId ?: "${inputId}--${UUID.randomUUID()}"

        when (dataType) {
            "FACES" -> handleFaceRegistration(student, finalFaceId, isRegistered, existingFaces, dataType, schoolId)
            "ASSIGNMENT" -> {
                if (!isRegistered) {
                    Pair(ProcessResult(inputId, student.name, "Error", dataType, "ID '$inputId' belum terdaftar."), null)
                } else {
                    saveStudentAssignments(finalFaceId, student, schoolId)
                    Pair(ProcessResult(inputId, student.name, "Class Updated", dataType), null)
                }
            }
            else -> Pair(ProcessResult(inputId, student.name, "Unsupported Type", dataType), null)
        }
    }

    private suspend fun handleFaceRegistration(
        student: CsvStudentData,
        finalFaceId: String,
        isRegistered: Boolean,
        existingFaces: List<FaceEntity>,
        category: String,
        schoolId: String
    ): Pair<ProcessResult, FaceEntity?> {
        if (isRegistered) {
            saveStudentAssignments(finalFaceId, student, schoolId)
            return Pair(ProcessResult(student.faceId, student.name, "Updated Class", category), null)
        }

        if (student.name.isBlank() || student.photoUrl.isBlank()) {
            return Pair(ProcessResult(student.faceId, student.name, "Error", category, "Nama & Photo URL wajib"), null)
        }
val photoResult = bulkPhotoProcessor.processPhotoSource(student.photoUrl, finalFaceId)
val imageBytesForEmbedding = photoResult.imageBytes
if (!photoResult.success || imageBytesForEmbedding == null) {
    return Pair(ProcessResult(student.faceId, student.name, "Error", category, "Gagal memuat foto"), null)
}

val embeddingResult = imageProcessor.extractFaceEmbedding(imageBytesForEmbedding)
if (embeddingResult == null) {
    return Pair(ProcessResult(student.faceId, student.name, "Error", category, "Wajah tak terdeteksi"), null)
}

val (faceBytes, embedding) = embeddingResult

val isDuplicate = existingFaces.any {
    it.embedding?.let { saved ->
        val dist = NativeSecurityVault.calculateDistanceNative(saved, embedding)
        NativeSecurityVault.verifyMatchNative(dist, FaceNetConstants.DUPLICATE_THRESHOLD)
    } ?: false
}

if (isDuplicate) {
    return Pair(ProcessResult(student.faceId, student.name, "Duplicate Biometric", category), null)
}

val localPhotoPath = photoManager.saveFacePhoto(faceBytes, finalFaceId)
val imageBytes = faceBytes

if (localPhotoPath == null) return Pair(ProcessResult(student.faceId, student.name, "Error", category, "Gagal simpan foto lokal"), null)
        val cloudUrl = uploadFacePhotoToCloud(schoolId, finalFaceId, imageBytes)
        val finalPhotoPath = cloudUrl ?: localPhotoPath

        val faceEntity = FaceEntity(
            faceId = finalFaceId,
            name = student.name,
            photoUrl = finalPhotoPath,
            embedding = embedding,
            schoolId = schoolId,
            isSynced = (cloudUrl != null)
        )
        faceDao.upsertFace(faceEntity)
        saveStudentAssignments(finalFaceId, student, schoolId)

        return Pair(ProcessResult(student.faceId, student.name, "Registered", category), faceEntity)
    }

    private suspend fun saveStudentAssignments(faceId: String, student: CsvStudentData, schoolId: String) {
        val className = student.rawMetadata.entries.find { it.key.equals("CLASS", ignoreCase = true) }?.value
        if (className.isNullOrBlank()) return

        var classEntity = classDao.getClassByName(schoolId, className)
        if (classEntity == null) {
            classEntity = ClassEntity(id = UUID.randomUUID().toString(), name = className, schoolId = schoolId, isSynced = false)
            classDao.insert(classEntity)
            try { syncClassToCloud(schoolId, classEntity) } catch (e: Exception) {}
        }

        val assignment = FaceAssignmentEntity(faceId = faceId, classId = classEntity.id, schoolId = schoolId, isSynced = false)
        faceAssignmentDao.insertAssignment(assignment)
        try { syncFaceAssignmentToCloud(assignment) } catch (e: Exception) {}
    }

    private suspend fun uploadFacePhotoToCloud(schoolId: String, faceId: String, imageBytes: ByteArray): String? {
        val ref = storage.reference.child("schools/$schoolId/faces/$faceId.jpg")
        return try {
            ref.putBytes(imageBytes).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) { null }
    }

    private suspend fun bulkSyncFacesToCloud(schoolId: String, faces: List<FaceEntity>) {
        faces.chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { face ->
                val data = hashMapOf(
                    "faceId" to face.faceId,
                    "name" to face.name,
                    "photoUrl" to face.photoUrl,
                    "embedding" to face.embedding?.toList(),
                    "isActive" to true,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
                batch.set(db.collection("schools").document(schoolId).collection("master_faces").document(face.faceId), data, SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    private suspend fun syncClassToCloud(schoolId: String, classEntity: ClassEntity) {
        val data = hashMapOf("id" to classEntity.id, "name" to classEntity.name, "displayOrder" to classEntity.displayOrder, "lastUpdated" to FieldValue.serverTimestamp())
        db.collection("schools").document(schoolId).collection("classes").document(classEntity.id).set(data, SetOptions.merge()).await()
    }

    private suspend fun syncFaceAssignmentToCloud(assignment: FaceAssignmentEntity) {
        val docId = "${assignment.faceId}_${assignment.classId}"
        val data = hashMapOf("faceId" to assignment.faceId, "classId" to assignment.classId, "lastUpdated" to FieldValue.serverTimestamp())
        db.collection("schools").document(assignment.schoolId).collection("face_assignments").document(docId).set(data, SetOptions.merge()).await()
    }
}
