package com.azuratech.azuratime.features.student.data.repo

import androidx.room.withTransaction
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.result.asLocalResult
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.core.data.local.ClassEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.core.data.local.StudentClassAssignmentEntity

import com.azuratech.azuratime.features.student.data.local.StudentEntity
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.azuratech.azuratime.core.sync.SyncManager

/**
 * 🏛️ STUDENT REPOSITORY IMPLEMENTATION
 *
 * Repository is SSOT guardian. Room is primary source. Remote sync is side-effect.
 */
@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val schoolRepository: SchoolRepository,
) : StudentRepository {

    private val studentDao = database.studentDao()
    private val biometricDao = database.biometricDao()
    private val assignmentDao = database.studentClassAssignmentDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getStudentProfilesFlow(): Flow<Result<List<StudentProfile>>> {
        return sessionManager.activeSchoolIdFlow
            .flatMapLatest { schoolId ->
                if (schoolId.isNullOrBlank()) {
                    flowOf(Result.Success(emptyList()))
                } else {
                    studentDao.getStudentProfilesFlow(schoolId)
                        .map { list -> list.map { it.toDomain() } }
                        .asLocalResult()
                }
            }
    }

    override suspend fun getAll(): Result<List<StudentProfile>> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId.isNullOrBlank()) {
                return@withContext Result.Success(emptyList())
            }
            val result = getStudentProfilesFlow().first()
            if (result is Result.Success) {
                Result.Success(result.data)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun getProfileById(studentId: String): Result<StudentProfile?> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Success(null)
            val rawProfile = studentDao.getStudentProfileById(studentId, schoolId)
            Result.Success(rawProfile?.toDomain())
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun saveProfile(profile: StudentProfile): Result<Unit> {
        return try {
            val (student, biometric, assignments) = profile.toEntities()
            database.withTransaction {
                // ✅ FIX: Set isSynced = false to trigger pushPendingProfiles
                studentDao.upsert(student.copy(isSynced = false))
                biometricDao.deleteOtherBiometricsForStudent(student.studentId, biometric.studentId, student.schoolId)
                biometricDao.upsertStudentBiometric(biometric)

                // 🔥 AI Native FIX: Removed assignmentDao.deleteAllByStudentId()
                // We now allow additive assignments to support multi-class logic.
                for (assignment in assignments) {
                    assignmentDao.insertAssignment(assignment)
                }
            }
            syncManager.enqueueSync()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun deleteProfile(studentId: String): Result<Unit> {
        return try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            database.withTransaction {
                val student = studentDao.getById(studentId, schoolId)
                if (student?.isSynced == true) {
                    studentDao.markPendingDeletion(studentId, schoolId)
                    val biometric = biometricDao.getStudentBiometricByIdentity(studentId, schoolId)
                    if (biometric != null) {
                        biometricDao.markPendingDeletion(biometric.studentId, schoolId)
                    }
                } else {
                    val biometric = biometricDao.getStudentBiometricByIdentity(studentId, schoolId)
                    if (biometric != null) {
                        assignmentDao.deleteAllByStudentId(biometric.studentId)
                        biometricDao.deleteStudentBiometricById(biometric.studentId, schoolId)
                    }
                    studentDao.deleteById(studentId, schoolId)
                }
            }
            syncManager.enqueueSync()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateSyncStatus(studentId: String, status: SyncStatus): Result<Unit> {
        return try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            database.withTransaction {
                val student = studentDao.getById(studentId, schoolId)
                if (student != null) {
                    val updatedStudent = when (status) {
                        SyncStatus.SYNCED -> student.copy(isSynced = true, isDeleted = false)
                        SyncStatus.PENDING_DELETE -> student.copy(isSynced = false, isDeleted = true)
                        else -> student.copy(isSynced = false)
                    }
                    studentDao.upsert(updatedStudent)
                }

                val biometric = biometricDao.getStudentBiometricByIdentity(studentId, schoolId)
                if (biometric != null) {
                    val updatedBiometric = when (status) {
                        SyncStatus.SYNCED -> biometric.copy(isSynced = true, isDeleted = false)
                        SyncStatus.PENDING_DELETE -> biometric.copy(isSynced = false, isDeleted = true)
                        else -> biometric.copy(isSynced = false)
                    }
                    biometricDao.upsertStudentBiometric(updatedBiometric)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun pushPendingProfiles(): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        android.util.Log.e("DEBUG_PUSH", "🚨 pushPendingProfiles CALLED!")

        val schoolId = sessionManager.getActiveSchoolId()
        if (schoolId == null) {
            android.util.Log.e("DEBUG_PUSH", "❌ ABORTED: School ID is NULL!")
            return@withContext Result.Failure(AppError.BusinessRule("No School"))
        }
        android.util.Log.d("DEBUG_PUSH", "✅ School ID Found: $schoolId")

        try {
            val unsyncedStudents = studentDao.getUnsyncedStudents(schoolId)
            android.util.Log.d("DEBUG_PUSH", "🔍 Found ${unsyncedStudents.size} unsynced students.")

            for (student in unsyncedStudents) {
                val docRef = firestore.collection("schools").document(schoolId)
                    .collection("students").document(student.studentId)

                if (student.isDeleted) {
                    com.google.android.gms.tasks.Tasks.await(docRef.delete())
                } else {
                    // ✅ FIX: Fetch FULL LIST of classIds from Assignment DAO
                    val currentClassIds = assignmentDao.getClassIdsForStudent(student.studentId, schoolId).first()

                    android.util.Log.d("DEBUG_PUSH", "Pushing Student: ${student.name} | Classes: $currentClassIds")

                    val data = mapOf(
                        "studentId" to student.studentId,
                        "schoolId" to student.schoolId,
                        "name" to student.name,
                        "studentCode" to student.studentCode,
                        "classId" to student.classId, // Legacy support
                        "classIds" to currentClassIds, // ✅ Multi-class SSOT
                        "createdAt" to student.createdAt,
                        "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    )
                    com.google.android.gms.tasks.Tasks.await(docRef.set(data, com.google.firebase.firestore.SetOptions.merge()))
                }
                studentDao.upsert(student.copy(isSynced = true))
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("DEBUG_PUSH", "❌ Error: ${e.message}")
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun autoHealStudentIdentities(schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val biometrics = biometricDao.getAllStudentsForScanningList(schoolId)
            val studentsToCreate = mutableListOf<StudentEntity>()

            for (biometric in biometrics) {
                val targetStudentId = biometric.studentId
                val existing = studentDao.getById(targetStudentId, schoolId)

                if (existing == null) {
                    studentsToCreate.add(
                        StudentEntity(
                            studentId = targetStudentId,
                            schoolId = schoolId,
                            name = biometric.name,
                            createdAt = biometric.createdAt,
                            isSynced = true,
                        ),
                    )
                }
            }

            if (studentsToCreate.isNotEmpty()) {
                studentDao.upsertAll(studentsToCreate)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun pullStudents(schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            // 🔥 AI Native Fix: First, sync all classes to ensure Foreign Key Integrity (Code 787)
            val accountId = sessionManager.getCurrentAccountId() ?: ""
            schoolRepository.syncClasses(accountId, schoolId)

            // 1. Pull Identities from 'students' collection
            val snapshot = firestore.collection("schools").document(schoolId)
                .collection("students").get().let { com.google.android.gms.tasks.Tasks.await(it) }

            val studentData = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("studentId") ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val studentCode = doc.getString("studentCode")
                val classId = doc.getString("classId")

                // ✅ AI Native Fix: Pull classIds array for multi-class support
                @Suppress("UNCHECKED_CAST")
                val classIds = doc.get("classIds") as? List<String> ?: emptyList()
                val finalClassIds = (classIds + listOfNotNull(classId)).distinct()

                println("🔍 SYNC PULL: Student $name ($id) has classes: $finalClassIds")

                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                Triple(
                    StudentEntity(
                        studentId = id,
                        schoolId = schoolId,
                        name = name,
                        studentCode = studentCode,
                        classId = classId,
                        createdAt = createdAt,
                        isSynced = true,
                    ),
                    id,
                    finalClassIds,
                )
            }

            if (studentData.isNotEmpty()) {
                database.withTransaction {
                    // 2. Save Identities
                    for ((entity, _, _) in studentData) {
                        studentDao.upsert(entity)
                    }

                    // 3. Save Assignments with Healing Logic
                    for ((_, studentId, classIds) in studentData) {
                        for (cId in classIds) {
                            try {
                                // 🔥 HEALING: Ensure class exists before assignment
                                if (schoolRepository.getClassById(cId).let { it is Result.Success && it.data == null }) {
                                    schoolRepository.saveClassLocally(
                                        ClassEntity(
                                            id = cId,
                                            ownerAccountId = accountId,
                                            schoolId = schoolId,
                                            name = "Auto-Healed Class",
                                            isSynced = true,
                                        ),
                                    )
                                }

                                assignmentDao.insertAssignment(
                                    StudentClassAssignmentEntity(
                                        studentId = studentId,
                                        classId = cId,
                                        schoolId = schoolId,
                                        isSynced = true,
                                    ),
                                )
                            } catch (e: Exception) {
                                println("⚠️ SYNC HEAL: Failed to assign student $studentId to class $cId. Error: ${e.message}")
                            }
                        }
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
