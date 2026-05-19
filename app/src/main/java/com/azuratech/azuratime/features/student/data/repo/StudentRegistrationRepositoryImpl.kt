package com.azuratech.azuratime.features.student.data.repo

import com.azuratech.azuraengine.model.ProcessResult
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.core.domain.sync.CsvImportUtils
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.account.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRegistrationRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏛️ STUDENT REGISTRATION REPOSITORY IMPLEMENTATION (v3.2.0-ai-native)
 * Unified engine for student registration with smart class resolution.
 */
@Singleton
class StudentRegistrationRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val csvImportUtils: CsvImportUtils,
    private val studentRepository: StudentRepository,
    private val sessionManager: SessionManager,
) : StudentRegistrationRepository {
    private val biometricDao = database.biometricDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val classDao = database.classDao()
    private val schoolClassDao = database.schoolClassDao()

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

    override fun processCsv(uri: String, schoolId: String): Flow<Result<ProcessResult>> = flow {
        val parseResult = csvImportUtils.parseCsvFile(uri)
        val accountId = sessionManager.getCurrentAccountId() ?: ""

        if (parseResult.students.isEmpty() && parseResult.errors.isNotEmpty()) {
            emit(Result.Failure(AppError.BusinessRule(parseResult.errors.first())))
            return@flow
        }

        // Cache for resolved class names to minimize DB hits during import
        val classCache = mutableMapOf<String, String>()

        parseResult.students.forEach { data ->
            val rawClassName = data.rawMetadata["CLASS"]

            // 🔥 AI Native: Resolve class name to ID (with auto-creation)
            val resolvedClassId = if (!rawClassName.isNullOrBlank()) {
                classCache.getOrPut(rawClassName) {
                    val existing = classDao.getClassByName(rawClassName)
                    if (existing != null) {
                        // Ensure it is mapped to this school
                        schoolClassDao.assignClass(SchoolClassAssignment(schoolId, existing.id))
                        existing.id
                    } else {
                        // Auto-create missing class
                        val newClass = ClassEntity(
                            ownerAccountId = accountId,
                            schoolId = schoolId,
                            name = rawClassName,
                        )
                        classDao.insert(newClass)
                        schoolClassDao.assignClass(SchoolClassAssignment(schoolId, newClass.id))
                        newClass.id
                    }
                }
            } else {
                null
            }

            val profile = StudentProfile(
                studentId = data.faceId,
                schoolId = schoolId,
                name = data.name,
                studentCode = data.faceId,
                classIds = listOfNotNull(resolvedClassId),
                photoUrl = data.photoUrl,
                syncStatus = SyncStatus.PENDING_INSERT,
            )

            val saveResult = studentRepository.saveProfile(profile)

            val processResult = if (saveResult is Result.Success) {
                ProcessResult(data.faceId, data.name, "Registered (Local)")
            } else {
                val error = (saveResult as Result.Failure).error
                ProcessResult(data.faceId, data.name, "Error: ${error.message}")
            }
            emit(Result.Success(processResult))
        }
    }.catch { e ->
        emit(Result.Failure(AppError.BusinessRule(e.message)))
    }.flowOn(Dispatchers.IO)
}
