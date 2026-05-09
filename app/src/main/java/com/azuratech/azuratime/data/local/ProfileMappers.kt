package com.azuratech.azuratime.data.local

import com.azuratech.azuratime.domain.model.AuditLogProfile
import com.azuratech.azuratime.domain.model.ExportJobProfile
import com.azuratech.azuratime.domain.model.FaceEnrollmentProfile
import com.azuratech.azuratime.domain.model.ReportSummaryProfile
import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus

/**
 * 🗺️ PROFILE MAPPERS
 * Pure functions to bridge between Room Entities and the StudentProfile Domain Model.
 */

/**
 * Extension to convert AuditLogEntity to AuditLogProfile.
 */
fun AuditLogEntity.toProfile(): AuditLogProfile {
    return AuditLogProfile(
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
 * Extension to convert ReportEntity to ReportSummaryProfile.
 */
fun ReportEntity.toProfile(): ReportSummaryProfile {
    return ReportSummaryProfile(
        reportId = reportId,
        reportName = name,
        dateRange = "${java.time.Instant.ofEpochMilli(startDate)} - ${java.time.Instant.ofEpochMilli(endDate)}",
        metrics = emptyMap(), // Logic to parse metricsJson can be added here
        syncStatus = if (isSynced) SyncStatus.SYNCED else SyncStatus.PENDING_UPDATE
    )
}

/**
 * Extension to convert FaceEntity to FaceEnrollmentProfile.
 */
fun FaceEntity.toProfile(): FaceEnrollmentProfile {
    return FaceEnrollmentProfile(
        faceId = faceId,
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
 * Joins with optional FaceEntity and class list.
 */
fun StudentEntity.toDomain(
    face: FaceEntity? = null, 
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
        faceId = face?.faceId,
        embedding = face?.embedding, // Handled by Converters.kt in Room
        photoUrl = face?.photoUrl,
        syncStatus = status,
        createdAt = createdAt,
        updatedAt = face?.lastUpdated ?: createdAt
    )
}

/**
 * Extension to convert FaceEntity to Domain Profile.
 * Fallback for cases where FaceEntity exists but StudentEntity is missing.
 */
fun FaceEntity.toDomain(
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
        studentId = studentId ?: faceId,
        studentCode = student?.studentCode,
        name = name,
        schoolId = schoolId,
        classIds = finalClassIds,
        faceId = faceId,
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
    face: FaceEntity, 
    student: StudentEntity? = null
): StudentProfile {
    return face.toDomain(student, listOf(classId))
}

/**
 * Convert a Domain StudentProfile back to its constituent Room Entities.
 * Returns a Triple of (StudentEntity, FaceEntity, List<FaceAssignmentEntity>).
 */
fun StudentProfile.toEntities(): Triple<StudentEntity, FaceEntity, List<FaceAssignmentEntity>> {
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

    val faceEntity = FaceEntity(
        faceId = faceId ?: "FACE-$studentId", // Stable deterministic ID fallback
        studentId = studentId,
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
            faceId = faceEntity.faceId,
            classId = classId,
            schoolId = schoolId,
            isSynced = isSynced
        )
    }

    return Triple(studentEntity, faceEntity, assignments)
}
