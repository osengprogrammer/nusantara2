package com.azuratech.azuratime.features.biometric.data.local

import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.staff.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
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

    override fun getAllStudentsFlow(schoolId: String): Flow<List<BiometricFaceEntity>> =
        faceDao.getAllStudentsFlow(schoolId)

    override fun getAllStudentsWithDetailsFlow(schoolId: String): Flow<List<FaceWithDetails>> =
        faceDao.getAllStudentsWithDetailsFlow(schoolId)

    override fun getAllStudentsForScanningFlow(schoolId: String): Flow<List<BiometricFaceEntity>> =
        faceDao.getAllStudentsForScanning(schoolId)

    override fun getStudentsInClassFlow(classId: String, schoolId: String): Flow<List<BiometricFaceEntity>> =
        faceAssignmentDao.getStudentsByClass(classId, schoolId)

    override fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>> =
        database.classDao().observeClassesBySchool(schoolId)

    override fun getAllAssignmentsFlow(schoolId: String): Flow<List<FaceAssignmentEntity>> =
        faceAssignmentDao.getAllAssignments(schoolId)

    override suspend fun getStudentWithDetails(studentId: String, schoolId: String): FaceWithDetails? =
        faceDao.getStudentWithDetails(studentId, schoolId)

    override suspend fun getClassIdsForStudent(studentId: String, schoolId: String): List<String> =
        faceAssignmentDao.getClassIdsForStudent(studentId, schoolId).firstOrNull() ?: emptyList()

    override suspend fun getAllStudentsForScanningList(schoolId: String): List<BiometricFaceEntity> =
        faceDao.getAllStudentsForScanningList(schoolId)

    override suspend fun getStudentFaceById(studentId: String, schoolId: String): BiometricFaceEntity? =
        faceDao.getStudentFaceById(studentId, schoolId)

    override suspend fun getStudentFaceByIdentity(studentId: String, schoolId: String): BiometricFaceEntity? =
        faceDao.getStudentFaceByIdentity(studentId, schoolId)

    override suspend fun upsertStudentFace(studentFace: BiometricFaceEntity) =
        faceDao.upsertStudentFace(studentFace)

    override suspend fun upsertAllStudentFaces(studentFaces: List<BiometricFaceEntity>) =
        faceDao.upsertAllStudentFaces(studentFaces)

    override suspend fun deleteStudentFace(studentFace: BiometricFaceEntity) =
        faceDao.deleteStudentFace(studentFace)

    override suspend fun deleteStudentFaceById(studentId: String, schoolId: String) =
        faceDao.deleteStudentFaceById(studentId, schoolId)

    override suspend fun insertAssignment(assignment: FaceAssignmentEntity) =
        faceAssignmentDao.insertAssignment(assignment)

    override suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String) =
        faceAssignmentDao.deleteAllByStudent(studentId, schoolId)

    override suspend fun markPendingDeletion(studentId: String, schoolId: String) =
        faceDao.markPendingDeletion(studentId, schoolId)
}
