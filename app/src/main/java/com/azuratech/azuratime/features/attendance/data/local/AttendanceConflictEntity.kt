package com.azuratech.azuratime.features.attendance.data.local

import androidx.room.*
import androidx.room.RoomWarnings
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict

/**
 * Persistence entity for Attendance Conflicts.
 * Stores both local and cloud versions of a check-in record for resolution.
 */
@Entity(
    tableName = "attendance_conflicts",
    primaryKeys = ["conflictId"],
    indices = [
        Index(value = ["local_schoolId"]),
        Index(value = ["cloud_schoolId"]),
        Index(value = ["local_studentId"]),
        Index(value = ["cloud_studentId"]),
        Index(value = ["local_sessionId"]),
        Index(value = ["cloud_sessionId"]),
    ],
)
@SuppressWarnings(
    RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED,
    RoomWarnings.INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED,
    RoomWarnings.INDEX_FROM_EMBEDDED_FIELD_IS_DROPPED,
)
data class AttendanceConflictEntity(
    val conflictId: String,
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
