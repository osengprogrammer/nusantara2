package com.azuratech.azuratime.data.local

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

    override fun getAllFacesFlow(schoolId: String): Flow<List<FaceEntity>> =
        faceDao.getAllFacesFlow(schoolId)

    override fun getAllFacesForScanningFlow(schoolId: String): Flow<List<FaceEntity>> =
        faceDao.getAllFacesForScanning(schoolId)

    override fun getFacesInClassFlow(classId: String, schoolId: String): Flow<List<FaceEntity>> =
        faceAssignmentDao.getFacesByClass(classId, schoolId)

    override fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>> =
        database.classDao().observeClassesBySchool(schoolId)

    override fun getAllAssignmentsFlow(schoolId: String): Flow<List<FaceAssignmentEntity>> =
        faceAssignmentDao.getAllAssignments(schoolId)

    override suspend fun getFaceWithDetails(faceId: String, schoolId: String): FaceWithDetails? =
        faceDao.getFaceWithDetails(faceId, schoolId)

    override suspend fun getClassIdsForFace(faceId: String, schoolId: String): List<String> =
        faceAssignmentDao.getClassIdsForFace(faceId, schoolId).firstOrNull() ?: emptyList()

    override suspend fun getAllFacesForScanningList(schoolId: String): List<FaceEntity> =
        faceDao.getAllFacesForScanningList(schoolId)

    override suspend fun getFaceById(faceId: String, schoolId: String): FaceEntity? =
        faceDao.getFaceById(faceId, schoolId)

    override suspend fun upsertFace(face: FaceEntity) =
        faceDao.upsertFace(face)

    override suspend fun upsertAll(faces: List<FaceEntity>) =
        faceDao.upsertAll(faces)

    override suspend fun deleteFace(face: FaceEntity) =
        faceDao.deleteFace(face)

    override suspend fun deleteFaceById(faceId: String, schoolId: String) =
        faceDao.deleteFaceById(faceId, schoolId)

    override suspend fun insertAssignment(assignment: FaceAssignmentEntity) =
        faceAssignmentDao.insertAssignment(assignment)

    override suspend fun deleteAssignmentsByFace(faceId: String, schoolId: String) =
        faceAssignmentDao.deleteAllByFace(faceId, schoolId)
}
