package com.azuratech.azuratime.features.biometric.data.local

import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.features.school.data.local.ClassEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricLocalDataSourceImpl @Inject constructor(
    private val database: AppDatabase
) : BiometricLocalDataSource {
    private val biometricDao = database.biometricDao()
    private val assignmentDao = database.studentClassAssignmentDao()

    override fun getAllStudentsFlow(schoolId: String): Flow<List<StudentBiometricEntity>> =
        biometricDao.getAllStudentsFlow(schoolId)

    override fun getAllStudentsWithDetailsFlow(schoolId: String): Flow<List<StudentBiometricDetails>> =
        biometricDao.getAllStudentsWithDetailsFlow(schoolId)

    override fun getAllStudentsForScanningFlow(schoolId: String): Flow<List<StudentBiometricEntity>> =
        biometricDao.getAllStudentsForScanning(schoolId)

    override fun getStudentsInClassFlow(classId: String, schoolId: String): Flow<List<StudentBiometricEntity>> =
        assignmentDao.getStudentsByClass(classId, schoolId)

    override fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>> =
        database.classDao().observeClassesBySchool(schoolId)

    override fun getAllAssignmentsFlow(schoolId: String): Flow<List<StudentClassAssignmentEntity>> =
        assignmentDao.getAllAssignments(schoolId)

    override suspend fun getStudentWithDetails(studentId: String, schoolId: String): StudentBiometricDetails? =
        biometricDao.getStudentWithDetails(studentId, schoolId)

    override suspend fun getClassIdsForStudent(studentId: String, schoolId: String): List<String> =
        assignmentDao.getClassIdsForStudent(studentId, schoolId).firstOrNull() ?: emptyList()

    override suspend fun getAllStudentsForScanningList(schoolId: String): List<StudentBiometricEntity> =
        biometricDao.getAllStudentsForScanningList(schoolId)

    override suspend fun getStudentFaceById(studentId: String, schoolId: String): StudentBiometricEntity? =
        biometricDao.getStudentBiometricById(studentId, schoolId)

    override suspend fun getStudentFaceByIdentity(studentId: String, schoolId: String): StudentBiometricEntity? =
        biometricDao.getStudentBiometricByIdentity(studentId, schoolId)

    override suspend fun upsertStudentFace(studentFace: StudentBiometricEntity) =
        biometricDao.upsertStudentBiometric(studentFace)

    override suspend fun upsertAllStudentFaces(studentFaces: List<StudentBiometricEntity>) =
        biometricDao.upsertAllStudentBiometrics(studentFaces)

    override suspend fun deleteStudentFaceById(studentId: String, schoolId: String) =
        biometricDao.deleteStudentBiometricById(studentId, schoolId)

    override suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String) =
        assignmentDao.deleteAllByStudent(studentId, schoolId)

    override suspend fun markPendingDeletion(studentId: String, schoolId: String) =
        biometricDao.markPendingDeletion(studentId, schoolId)

    override suspend fun insertAssignment(assignment: StudentClassAssignmentEntity) =
        assignmentDao.insertAssignment(assignment)
}
