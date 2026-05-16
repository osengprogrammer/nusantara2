package com.azuratech.azuratime.core.data.local

import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.features.reporting.domain.model.SchoolAnalyticsSummary
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.student.data.local.StudentEntity
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import com.azuratech.azuratime.features.reporting.data.local.AuditLogEntity
import com.azuratech.azuratime.features.reporting.data.local.ExportJobEntity
import com.azuratech.azuratime.features.reporting.data.local.ReportEntity

import com.azuratech.azuratime.features.biometric.data.local.FaceAssignmentEntity

/**
 *  PROFILE MAPPERS
 * Pure functions to bridge between Room Entities and the StudentProfile Domain Model.
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
        metrics = emptyMap(), // Logic to parse metricsJson can be added here
        syncStatus = if (isSynced) SyncStatus.SYNCED else SyncStatus.PENDING_UPDATE
    )
}

/**
 * Extension to convert BiometricFaceEntity to BiometricEnrollmentProfile.
 */
fun BiometricFaceEntity.toProfile(): BiometricEnrollmentProfile {
    return BiometricEnrollmentProfile(
        faceId = studentId,
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
 * Joins with optional BiometricFaceEntity and class list.
 */
fun StudentEntity.toDomain(
    face: BiometricFaceEntity? = null, 
    classIds: List<String> = emptyList()
): StudentProfile {
    val status = when {
        isSynced && (face?.isSynced ?: true) -> SyncStatus.SYNCED
        isDeleted || (face?.isDeleted ?: false) -> SyncStatus.PENDING_DELETE
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
        faceId = face?.studentId,
        embedding = face?.embedding, // Handled by Converters.kt in Room
        photoUrl = face?.photoUrl,
        syncStatus = status,
        createdAt = createdAt,
        updatedAt = face?.lastUpdated ?: createdAt
    )
}

/**
 * Extension to convert BiometricFaceEntity to Domain Profile.
 * Fallback for cases where BiometricFaceEntity exists but StudentEntity is missing.
 */
fun BiometricFaceEntity.toDomain(
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
 * Extension to convert FaceAssignmentEntity to Domain Profile.
 * requires associated Face and optional Student.
 */
fun FaceAssignmentEntity.toDomain(
    face: BiometricFaceEntity, 
    student: StudentEntity? = null
): StudentProfile {
    return face.toDomain(student, listOf(classId))
}

/**
 * Convert a Domain StudentProfile back to its constituent Room Entities.
 * Returns a Triple of (StudentEntity, BiometricFaceEntity, List<FaceAssignmentEntity>).
 */
fun StudentProfile.toEntities(): Triple<StudentEntity, BiometricFaceEntity, List<FaceAssignmentEntity>> {
    val isSynced = syncStatus == SyncStatus.SYNCED
    val isDeleted = syncStatus == SyncStatus.PENDING_DELETE

    val studentEntity = StudentEntity(
        studentId = studentId,
        schoolId = schoolId,
        name = name,
        studentCode = studentCode,
        classId = classIds.firstOrNull(), // Store first as primary for legacy compat
        createdAt = createdAt,
        isSynced = isSynced,
        isDeleted = isDeleted
    )

    val faceEntity = BiometricFaceEntity(
        studentId = faceId ?: studentId, // 🔥 AI Friendly: Default to studentId
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
        FaceAssignmentEntity(
            studentId = faceEntity.studentId,
            classId = classId,
            schoolId = schoolId,
            isSynced = isSynced
        )
    }

    return Triple(studentEntity, faceEntity, assignments)
}
