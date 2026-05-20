package com.azuratech.azuratime.features.biometric.data.repo

import android.app.Application
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuratime.core.data.local.BiometricCache
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.toProfile
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.biometric.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.biometric.data.remote.BiometricRemoteDataSource
import com.azuratech.azuratime.features.biometric.data.local.BiometricLocalDataSource
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🧬 STUDENT BIOMETRIC REPOSITORY IMPLEMENTATION (v3.2.0-ai-native)
 * Unified single source of truth for biometric data and class assignments.
 */
@Singleton
class StudentBiometricRepositoryImpl @Inject constructor(
    private val application: Application,
    private val localDataSource: BiometricLocalDataSource,
    private val remoteDataSource: BiometricRemoteDataSource,
    private val sessionManager: SessionManager,
    private val studentRepository: StudentRepository,
) : BiometricRepository {
    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    override fun observeEnrollments(): Flow<Result<List<BiometricEnrollmentProfile>>> =
        localDataSource.getAllStudentsFlow(schoolId)
            .map { entities -> entities.map { it.toProfile() } }
            .asLocalResult()

    override suspend fun enrollStudent(studentId: String, embedding: FloatArray): Result<Unit> {
        return try {
            val existing = localDataSource.getStudentFaceByIdentity(studentId, schoolId)
            val updated = existing?.copy(
                embedding = embedding,
                isSynced = false,
                lastUpdated = System.currentTimeMillis(),
            ) ?: StudentBiometricEntity(
                studentId = studentId,
                schoolId = schoolId,
                name = "Unknown",
                embedding = embedding,
                isSynced = false,
                lastUpdated = System.currentTimeMillis(),
            )
            localDataSource.upsertStudentFace(updated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun captureFace(): Result<FloatArray> {
        return Result.Success(FloatArray(512))
    }

    override suspend fun deleteEnrollment(studentId: String): Result<Unit> {
        return try {
            localDataSource.markPendingDeletion(studentId, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun syncBiometrics(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val lastSync = sessionManager.getLastFacesSyncTime()
            val syncResult = remoteDataSource.getBiometricUpdates(schoolId, lastSync)

            if (syncResult is Result.Success) {
                val updatedData = syncResult.data
                if (updatedData.isNotEmpty()) {
                    val uniqueBiometrics = updatedData.filter { it.second }.map { it.first }
                        .groupBy { it.studentId }
                        .map { entry ->
                            val itemsForStudent = entry.value
                            itemsForStudent.maxByOrNull { it.lastUpdated }
                                ?: itemsForStudent.first()
                        }

                    val toDelete = updatedData.filter { !it.second }.map { it.first }

                    if (uniqueBiometrics.isNotEmpty()) localDataSource.upsertAllStudentFaces(uniqueBiometrics)
                    if (toDelete.isNotEmpty()) {
                        toDelete.forEach { localDataSource.deleteStudentFaceById(it.studentId, schoolId) }
                    }

                    BiometricCache.refresh(application, schoolId)
                    sessionManager.saveLastFacesSyncTime(System.currentTimeMillis())
                }
                Result.Success(Unit)
            } else {
                syncResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override fun getAllStudentsFlow(schoolId: String): Flow<Result<List<StudentBiometricEntity>>> =
        localDataSource.getAllStudentsFlow(schoolId).asLocalResult()

    override fun getEnrolledStudentsFlow(schoolId: String): Flow<Result<List<StudentBiometricEntity>>> =
        localDataSource.getAllStudentsForScanningFlow(schoolId).asLocalResult()

    override fun getStudentsInClassFlow(classId: String, schoolId: String): Flow<Result<List<StudentBiometricEntity>>> =
        localDataSource.getStudentsInClassFlow(classId, schoolId).asLocalResult()

    override fun getStudentsWithDetailsFlow(schoolId: String): Flow<Result<List<com.azuratech.azuratime.core.data.local.StudentBiometricDetails>>> =
        localDataSource.getAllStudentsWithDetailsFlow(schoolId).asLocalResult()

    override fun getAllAssignmentsFlow(schoolId: String): Flow<Result<List<StudentClassAssignmentEntity>>> =
        localDataSource.getAllAssignmentsFlow(schoolId).asLocalResult()

    override suspend fun getStudentWithDetails(studentId: String, schoolId: String): Result<com.azuratech.azuratime.core.data.local.StudentBiometricDetails?> = try {
        Result.Success(localDataSource.getStudentWithDetails(studentId, schoolId))
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun getClassIdsForStudent(studentId: String, schoolId: String): Result<List<String>> = try {
        Result.Success(localDataSource.getClassIdsForStudent(studentId, schoolId))
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun insertAssignment(assignment: StudentClassAssignmentEntity): Result<Unit> = try {
        localDataSource.insertAssignment(assignment)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun upsertStudentBiometric(studentBiometric: StudentBiometricEntity): Result<Unit> = try {
        localDataSource.upsertStudentFace(studentBiometric)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun assignStudentToClass(studentId: String, classId: String): Result<Unit> = try {
        localDataSource.insertAssignment(StudentClassAssignmentEntity(studentId, classId, schoolId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun removeStudentFromClass(studentId: String, classId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun removeAllAssignmentsForStudent(studentId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun updateStudentClass(studentId: String, classId: String?): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
        if (classId != null) {
            localDataSource.insertAssignment(StudentClassAssignmentEntity(studentId, classId, schoolId))
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun saveStudentProfile(profile: StudentProfile, photoBytes: ByteArray?): Result<Unit> {
        return studentRepository.saveProfile(profile)
    }

    override suspend fun syncAssignments(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        Result.Success(Unit)
    }
}
