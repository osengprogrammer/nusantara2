package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.core.domain.model.TeacherAssignment
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.domain.repository.SessionRepository
import com.azuratech.azuratime.core.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 🚀 IMPORT MATRIX USE CASE (v3.4.0-matrix)
 * Domain logic for resolving CSV rows into Matrix Assignments.
 */
class ImportMatrixUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionRepository: SessionRepository,
    private val database: AppDatabase,
) {
    suspend fun resolveRows(
        schoolId: String,
        rows: List<Map<String, String>>,
    ): List<MatrixImportPreview> {
        // 1. Fetch all data for resolution
        val classes = schoolRepository.getClasses(schoolId).getOrNull() ?: emptyList()
        val subjects = sessionRepository.observeAllSubjectsFlow(schoolId).first().getOrNull() ?: emptyList()
        val accounts = database.accountDao().getAllAccountsOnce().filter { it.memberships.containsKey(schoolId) }

        return rows.map { row ->
            val email = row["teacher_email"] ?: ""
            val className = row["class_name"] ?: ""
            val subjectName = row["subject_name"] // Nullable

            val account = accounts.find { it.email.equals(email, ignoreCase = true) }
            val classObj = classes.find { it.name.equals(className, ignoreCase = true) }
            val subjectObj = if (subjectName.isNullOrBlank()) null else subjects.find { it.name.equals(subjectName, ignoreCase = true) }

            val isValid = account != null && classObj != null && (subjectName.isNullOrBlank() || subjectObj != null)

            val status = when {
                account == null -> "Email not found"
                classObj == null -> "Class not found"
                !subjectName.isNullOrBlank() && subjectObj == null -> "Subject not found"
                else -> "Ready"
            }

            MatrixImportPreview(
                teacherEmail = email,
                className = className,
                subjectName = subjectName,
                status = status,
                isSuccess = isValid,
                accountId = account?.accountId,
                classId = classObj?.id,
                subjectId = subjectObj?.subjectId,
            )
        }
    }

    suspend fun commit(schoolId: String, previews: List<MatrixImportPreview>): Result<Unit> {
        val validPreviews = previews.filter { it.isSuccess && it.accountId != null && it.classId != null }
        if (validPreviews.isEmpty()) return Result.Success(Unit)

        // Group by accountId to perform bulk updates per teacher
        val assignmentMap = validPreviews.groupBy { it.accountId!! }.mapValues { (_, previews) ->
            previews.map { TeacherAssignment(it.classId!!, it.subjectId) }
        }

        return accountRepository.bulkUpdateAssignments(schoolId, assignmentMap)
    }
}

data class MatrixImportPreview(
    val teacherEmail: String,
    val className: String,
    val subjectName: String?,
    val status: String,
    val isSuccess: Boolean,
    val accountId: String? = null,
    val classId: String? = null,
    val subjectId: String? = null,
)
