package com.azuratech.azuratime.features.biometric.data.local

import com.azuratech.azuratime.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceLocalDataSourceImpl @Inject constructor(
    private val database: AppDatabase
) : FaceLocalDataSource {
    private val faceDao = database.faceDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    override fun getAllFacesFlow(schoolId: String): Flow<List<BiometricFaceEntity>> =
        faceDao.getAllFacesFlow(schoolId)

    override fun getAllFacesWithDetailsFlow(schoolId: String): Flow<List<FaceWithDetails>> =
        faceDao.getAllFacesWithDetailsFlow(schoolId)

    override fun getAllFacesForScanningFlow(schoolId: String): Flow<List<BiometricFaceEntity>> =
        faceDao.getAllFacesForScanning(schoolId)

    override fun getFacesInClassFlow(classId: String, schoolId: String): Flow<List<BiometricFaceEntity>> =
        faceAssignmentDao.getFacesByClass(classId, schoolId)

    override fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>> =
        database.classDao().observeClassesBySchool(schoolId)

    override fun getAllAssignmentsFlow(schoolId: String): Flow<List<FaceAssignmentEntity>> =
        faceAssignmentDao.getAllAssignments(schoolId)

    override suspend fun getFaceWithDetails(faceId: String, schoolId: String): FaceWithDetails? =
        faceDao.getFaceWithDetails(faceId, schoolId)

    override suspend fun getClassIdsForFace(faceId: String, schoolId: String): List<String> =
        faceAssignmentDao.getClassIdsForFace(faceId, schoolId).firstOrNull() ?: emptyList()

    override suspend fun getAllFacesForScanningList(schoolId: String): List<BiometricFaceEntity> =
        faceDao.getAllFacesForScanningList(schoolId)

    override suspend fun getFaceById(faceId: String, schoolId: String): BiometricFaceEntity? =
        faceDao.getFaceById(faceId, schoolId)

    override suspend fun getFaceByStudentId(studentId: String, schoolId: String): BiometricFaceEntity? =
        faceDao.getFaceByStudentId(studentId, schoolId)

    override suspend fun upsertFace(face: BiometricFaceEntity) =
        faceDao.upsertFace(face)

    override suspend fun upsertAll(faces: List<BiometricFaceEntity>) =
        faceDao.upsertAll(faces)

    override suspend fun deleteFace(face: BiometricFaceEntity) =
        faceDao.deleteFace(face)

    override suspend fun deleteFaceById(faceId: String, schoolId: String) =
        faceDao.deleteFaceById(faceId, schoolId)

    override suspend fun insertAssignment(assignment: FaceAssignmentEntity) =
        faceAssignmentDao.insertAssignment(assignment)

    override suspend fun deleteAssignmentsByFace(faceId: String, schoolId: String) =
        faceAssignmentDao.deleteAllByFace(faceId, schoolId)

    override suspend fun markPendingDeletion(faceId: String, schoolId: String) =
        faceDao.markPendingDeletion(faceId, schoolId)
}
