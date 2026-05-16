package com.azuratech.azuratime.core.data.local

import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.features.reporting.domain.model.SchoolAnalyticsSummary
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.student.data.local.StudentEntity
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.reporting.data.local.AuditLogEntity
import com.azuratech.azuratime.features.reporting.data.local.ExportJobEntity
import com.azuratech.azuratime.features.reporting.data.local.ReportEntity
import com.azuratech.azuratime.features.account.data.local.AccessRequestEntity
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile

import com.azuratech.azuratime.features.biometric.data.local.StudentClassAssignmentEntity

/**
 *  PROFILE MAPPERS
 * Pure functions to bridge between Room Entities and Domain Models.
 */

/**
 * Extension to convert AuditLogEntity to SystemAuditTrail.
 */
fun AuditLogEntity.toProfile(): SystemAuditTrail {
    return SystemAuditTrail(
        logId = logId,
        userId = userId,
        action = action,
        timestamp = timestamp,
        details = details,
        syncStatus = if (isSynced) SyncStatus.SYNCED else SyncStatus.PENDING_UPDATE
    )
}

/**
 * Extension to convert ExportJobEntity to ExportJobProfile.
 */
fun ExportJobEntity.toProfile(): ExportJobProfile {
    return ExportJobProfile(
        jobId = jobId,
        fileType = fileType,
        status = status,
        filePath = filePath,
        syncStatus = if (isSynced) SyncStatus.SYNCED else SyncStatus.PENDING_UPDATE
    )
}

/**
 * Extension to convert ReportEntity to SchoolAnalyticsSummary.
 */
fun ReportEntity.toProfile(): SchoolAnalyticsSummary {
    return SchoolAnalyticsSummary(
        reportId = reportId,
        reportName = name,
        dateRange = "${java.time.Instant.ofEpochMilli(startDate)} - ${java.time.Instant.ofEpochMilli(endDate)}",
        metrics = emptyMap(), 
        syncStatus = if (isSynced) SyncStatus.SYNCED else SyncStatus.PENDING_UPDATE
    )
}

/**
 * Extension to convert AccessRequestEntity to AccessRequestProfile.
 */
fun AccessRequestEntity.toProfile(): AccessRequestProfile {
    return AccessRequestProfile(
        requestId = requestId,
        requesterId = requesterId,
        schoolId = schoolId,
        schoolName = schoolName,
        status = status,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Extension to convert StudentBiometricEntity to BiometricEnrollmentProfile.
 */
fun StudentBiometricEntity.toProfile(): BiometricEnrollmentProfile {
    return BiometricEnrollmentProfile(
        studentId = studentId,
        studentName = name,
        photoUri = photoUrl,
        enrollmentDate = lastUpdated,
        syncStatus = if (isSynced) SyncStatus.SYNCED else if (isDeleted) SyncStatus.PENDING_DELETE else SyncStatus.PENDING_UPDATE
    )
}

/**
 * Extension to convert RawStudentProfile (JOIN result) to Domain Profile.
 */
fun RawStudentProfile.toDomain(): StudentProfile {
    val status = when {
        student.isSynced && (faceIsSynced ?: true) -> SyncStatus.SYNCED
        student.isDeleted || (faceIsDeleted ?: false) -> SyncStatus.PENDING_DELETE
        else -> SyncStatus.PENDING_UPDATE
    }

    return StudentProfile(
        studentId = student.studentId,
        studentCode = student.studentCode,
        name = student.name,
        schoolId = student.schoolId,
        classIds = allClassIds,
        faceId = faceId,
        embedding = embedding,
        photoUrl = photoUrl,
        syncStatus = status,
        createdAt = student.createdAt,
        updatedAt = faceLastUpdated ?: student.createdAt
    )
}

/**
 * Extension to convert StudentEntity to Domain Profile.
 * Joins with optional StudentBiometricEntity and class list.
 */
fun StudentEntity.toDomain(
    biometric: StudentBiometricEntity? = null, 
    classIds: List<String> = emptyList()
): StudentProfile {
    val status = when {
        isSynced && (biometric?.isSynced ?: true) -> SyncStatus.SYNCED
        isDeleted || (biometric?.isDeleted ?: false) -> SyncStatus.PENDING_DELETE
        else -> SyncStatus.PENDING_UPDATE
    }

    // Merge primary classId with the provided list
    val finalClassIds = (classIds + listOfNotNull(classId)).distinct()

    return StudentProfile(
        studentId = studentId,
        studentCode = studentCode,
        name = name,
        schoolId = schoolId,
        classIds = finalClassIds,
        faceId = biometric?.studentId,
        embedding = biometric?.embedding, 
        photoUrl = biometric?.photoUrl,
        syncStatus = status,
        createdAt = createdAt,
        updatedAt = biometric?.lastUpdated ?: createdAt
    )
}

/**
 * Extension to convert StudentBiometricEntity to Domain Profile.
 */
fun StudentBiometricEntity.toDomain(
    student: StudentEntity? = null, 
    classIds: List<String> = emptyList()
): StudentProfile {
    val status = when {
        isSynced && (student?.isSynced ?: true) -> SyncStatus.SYNCED
        isDeleted || (student?.isDeleted ?: false) -> SyncStatus.PENDING_DELETE
        else -> SyncStatus.PENDING_UPDATE
    }

    // Merge student's primary classId if available
    val finalClassIds = (classIds + listOfNotNull(student?.classId)).distinct()

    return StudentProfile(
        studentId = studentId,
        studentCode = student?.studentCode,
        name = name,
        schoolId = schoolId,
        classIds = finalClassIds,
        faceId = studentId,
        embedding = embedding,
        photoUrl = photoUrl,
        syncStatus = status,
        createdAt = createdAt,
        updatedAt = lastUpdated
    )
}

/**
 * Extension to convert StudentClassAssignmentEntity to Domain Profile.
 */
fun StudentClassAssignmentEntity.toDomain(
    biometric: StudentBiometricEntity, 
    student: StudentEntity? = null
): StudentProfile {
    return biometric.toDomain(student, listOf(classId))
}

/**
 * Convert a Domain StudentProfile back to its constituent Room Entities.
 */
fun StudentProfile.toEntities(): Triple<StudentEntity, StudentBiometricEntity, List<StudentClassAssignmentEntity>> {
    val isSynced = syncStatus == SyncStatus.SYNCED
    val isDeleted = syncStatus == SyncStatus.PENDING_DELETE

    val studentEntity = StudentEntity(
        studentId = studentId,
        schoolId = schoolId,
        name = name,
        studentCode = studentCode,
        classId = classIds.firstOrNull(), 
        createdAt = createdAt,
        isSynced = isSynced,
        isDeleted = isDeleted
    )

    val biometricEntity = StudentBiometricEntity(
        studentId = faceId ?: studentId, 
        schoolId = schoolId,
        name = name,
        photoUrl = photoUrl,
        embedding = embedding,
        createdAt = createdAt,
        lastUpdated = updatedAt,
        isSynced = isSynced,
        isDeleted = isDeleted
    )
    val assignments = classIds.map { classId ->
        StudentClassAssignmentEntity(
            studentId = biometricEntity.studentId,
            classId = classId,
            schoolId = schoolId,
            isSynced = isSynced
        )
    }

    return Triple(studentEntity, biometricEntity, assignments)
}
