package com.azuratech.azuratime.features.attendance.data.local

import androidx.room.*
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict

/**
 * Persistence entity for Attendance Conflicts.
 * Stores both local and cloud versions of a check-in record for resolution.
 */
@Entity(
    tableName = "attendance_conflicts",
    indices = [
        Index(value = ["local_schoolId"]),
        Index(value = ["cloud_schoolId"]),
        Index(value = ["local_studentId"]),
        Index(value = ["cloud_studentId"]),
    ],
)
@Suppress("RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED", "RoomWarnings.INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED")
data class AttendanceConflictEntity(
    @PrimaryKey val conflictId: String,
    @Embedded(prefix = "local_") val local: AttendanceRecordEntity,
    @Embedded(prefix = "cloud_") val cloud: AttendanceRecordEntity,
) {
    fun toDomain(): AttendanceConflict {
        return AttendanceConflict(
            conflictId = conflictId,
            local = local.toDomain(),
            cloud = cloud.toDomain(),
        )
    }
}
