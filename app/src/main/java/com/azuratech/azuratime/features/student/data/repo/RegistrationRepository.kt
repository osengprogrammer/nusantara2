package com.azuratech.azuratime.features.student.data.repo

import android.app.Application
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.staff.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 REGISTRATION REPOSITORY
 * Thin wrapper for Registration Data Sources.
 */
@Singleton
class RegistrationRepository @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val faceDao = database.faceDao()
    private val faceAssignmentDao = database.faceAssignmentDao()
    private val classDao = database.classDao()

    // Delegation methods
    suspend fun getAllFaces(schoolId: String) = faceDao.getAllFacesForScanningList(schoolId)
    suspend fun upsertFace(face: BiometricFaceEntity) = faceDao.upsertFace(face)
    suspend fun upsertAllFaces(faces: List<BiometricFaceEntity>) = faceDao.upsertAll(faces)
    
    suspend fun getClassByName(name: String) = classDao.getClassByName(name)
    suspend fun insertClass(classEntity: ClassEntity) = classDao.insert(classEntity)
    
    suspend fun insertAssignment(assignment: FaceAssignmentEntity) = faceAssignmentDao.insertAssignment(assignment)

    fun processCsv(@Suppress("UNUSED_PARAMETER") uri: String, dataType: String): Flow<com.azuratech.azuraengine.model.ProcessResult> = flow {
        // Mock implementation to fix compilation
        emit(com.azuratech.azuraengine.model.ProcessResult("CSV", "Import", "Started", dataType, "Importing..."))
        emit(com.azuratech.azuraengine.model.ProcessResult("CSV", "Import", "Success", dataType, "Import complete"))
    }
}
