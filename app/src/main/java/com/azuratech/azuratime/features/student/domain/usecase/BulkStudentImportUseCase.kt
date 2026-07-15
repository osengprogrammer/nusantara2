package com.azuratech.azuratime.features.student.domain.usecase

import androidx.room.withTransaction
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.student.data.local.StudentEntity
import com.azuratech.azuratime.core.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import javax.inject.Inject

/**
 * 🔒 BULK STUDENT IMPORT USE CASE (v3.2.2-ai-native)
 * Encapsulates the bulk student registration process within a single database transaction
 * to guarantee 'all-or-nothing' transactional atomicity and schema consistency.
 */
class BulkStudentImportUseCase @Inject constructor(
    private val database: AppDatabase,
) {
    suspend operator fun invoke(
        students: List<StudentEntity>,
        biometrics: List<StudentBiometricEntity>,
        assignments: List<StudentClassAssignmentEntity>,
    ): Result<Unit> {
        return try {
            database.withTransaction {
                val studentDao = database.studentDao()
                val biometricDao = database.biometricDao()
                val assignmentDao = database.studentClassAssignmentDao()

                // 1. Bulk upsert all students
                studentDao.upsertAll(students)

                // 2. Bulk upsert all associated student biometrics
                biometricDao.upsertAllStudentBiometrics(biometrics)

                // 3. Bulk insert all student class assignments
                assignmentDao.insertAllAssignments(assignments)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
