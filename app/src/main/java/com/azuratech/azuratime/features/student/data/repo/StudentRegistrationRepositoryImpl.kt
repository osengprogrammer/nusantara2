package com.azuratech.azuratime.features.student.data.repo

import com.azuratech.azuraengine.model.ProcessResult
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.core.domain.sync.CsvImportUtils
import com.azuratech.azuratime.core.data.local.SchoolClassAssignment
import com.azuratech.azuratime.core.data.local.ClassEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.core.data.local.StudentClassAssignmentEntity

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

    override fun processCsvFlow(uri: String, schoolId: String): Flow<Result<ProcessResult>> = flow {
        val parseResult = csvImportUtils.parseCsvFile(uri)

        if (parseResult.students.isEmpty() && parseResult.errors.isNotEmpty()) {
            emit(Result.Failure(AppError.BusinessRule(parseResult.errors.first())))
            return@flow
        }

        // Cache for resolved class names to minimize DB hits during import
        val classCache = mutableMapOf<String, String?>()
        val warnings = mutableListOf<String>()

        parseResult.students.forEachIndexed { index, data ->
            val rawClassName = data.rawMetadata["CLASS"]
            val lineNumber = index + 2 // +2 karena header di baris 1

            // 🔥 Name-to-ID Resolution (STRICT - No Auto-Create)
            val resolvedClassId = if (!rawClassName.isNullOrBlank()) {
                if (classCache.containsKey(rawClassName)) {
                    classCache[rawClassName]
                } else {
                    val existing = classDao.getClassByName(rawClassName)
                    if (existing != null && existing.schoolId == schoolId) {
                        // ✅ Valid class
                        classCache[rawClassName] = existing.id
                        schoolClassDao.assignClass(SchoolClassAssignment(schoolId, existing.id))
                        existing.id
                    } else {
                        // ❌ Class not found or invalid - Save warning, DO NOT auto-create
                        warnings.add("Baris $lineNumber: Class '$rawClassName' tidak ditemukan di template. Siswa '${data.name}' disimpan TANPA rombel.")
                        classCache[rawClassName] = null
                        null
                    }
                }
            } else {
                null
            }

            // Create student profile (with or without class)
            val profile = StudentProfile(
                studentId = data.faceId,
                schoolId = schoolId,
                name = data.name,
                studentCode = data.faceId,
                classIds = listOfNotNull(resolvedClassId), // Empty if null
                photoUrl = data.photoUrl,
                syncStatus = SyncStatus.PENDING_INSERT,
            )

            val saveResult = studentRepository.saveProfile(profile)

            // Create assignment ONLY if class is valid
            if (resolvedClassId != null && saveResult is Result.Success) {
                val assignment = StudentClassAssignmentEntity(
                    studentId = data.faceId,
                    classId = resolvedClassId,
                    schoolId = schoolId,
                )
                assignmentDao.insertAssignment(assignment)
            }

            val processResult = if (saveResult is Result.Success) {
                if (resolvedClassId != null) {
                    ProcessResult(data.faceId, data.name, "Registered with class")
                } else {
                    ProcessResult(data.faceId, data.name, "Registered without class (class not found)")
                }
            } else {
                val error = (saveResult as Result.Failure).error
                ProcessResult(data.faceId, data.name, "Error: ${error.message}")
            }
            emit(Result.Success(processResult))
        }

        // Emit warnings at the end (optional - untuk UI feedback)
        if (warnings.isNotEmpty()) {
            println("⚠️ Warnings during import:\n${warnings.joinToString("\n")}")
        }
    }.catch { e ->
        emit(Result.Failure(AppError.BusinessRule(e.message)))
    }.flowOn(Dispatchers.IO)
}
