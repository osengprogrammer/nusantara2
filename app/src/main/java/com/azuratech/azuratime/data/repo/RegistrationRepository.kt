package com.azuratech.azuratime.data.repo

import android.app.Application
import com.azuratech.azuratime.data.local.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    suspend fun upsertFace(face: FaceEntity) = faceDao.upsertFace(face)
    suspend fun upsertAllFaces(faces: List<FaceEntity>) = faceDao.upsertAll(faces)
    
    suspend fun getClassByName(name: String) = classDao.getClassByName(name)
    suspend fun insertClass(classEntity: ClassEntity) = classDao.insert(classEntity)
    
    suspend fun insertAssignment(assignment: FaceAssignmentEntity) = faceAssignmentDao.insertAssignment(assignment)
}
