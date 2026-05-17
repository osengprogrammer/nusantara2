package com.azuratech.azuratime.features.student.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.account.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.student.domain.repository.StudentRegistrationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRegistrationRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : StudentRegistrationRepository {
    private val biometricDao = database.biometricDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val classDao = database.classDao()

    override suspend fun getAllBiometrics(schoolId: String): Result<List<StudentBiometricEntity>> {
        return try {
            Result.Success(biometricDao.getAllStudentsForScanningList(schoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun upsertBiometric(biometric: StudentBiometricEntity): Result<Unit> {
        return try {
            biometricDao.upsertStudentBiometric(biometric)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun upsertAllBiometrics(biometrics: List<StudentBiometricEntity>): Result<Unit> {
        return try {
            biometricDao.upsertAllStudentBiometrics(biometrics)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun getClassByName(name: String): Result<ClassEntity?> {
        return try {
            Result.Success(classDao.getClassByName(name))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun insertClass(classEntity: ClassEntity): Result<Unit> {
        return try {
            classDao.insert(classEntity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun insertAssignment(assignment: StudentClassAssignmentEntity): Result<Unit> {
        return try {
            assignmentDao.insertAssignment(assignment)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun processCsv(uri: String, dataType: String): Flow<com.azuratech.azuraengine.model.ProcessResult> = flow {
        emit(com.azuratech.azuraengine.model.ProcessResult("CSV", "Import", "Started", dataType, "Importing..."))
        emit(com.azuratech.azuraengine.model.ProcessResult("CSV", "Import", "Success", dataType, "Import complete"))
    }
}
