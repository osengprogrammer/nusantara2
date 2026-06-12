package com.azuratech.azuratime.features.attendance.domain.model

import com.azuratech.azuratime.features.session.domain.model.SessionType

/**
 * Pure Domain Model for Check-In Records.
 * Zero dependencies on Android, Room, or Firebase.
 */
data class AttendanceRecord(
    val recordId: String,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val className: String,
    val schoolId: String,
    val timestamp: Long,
    val status: AttendanceStatus,
    val sessionId: String? = null,
    val sessionType: SessionType = SessionType.ACADEMIC, // ✅ Tiering support
    val photoUrl: String? = null,
    val isSynced: Boolean = false,
    val accountEmail: String = "",
)

enum class AttendanceStatus {
    PRESENT,
    LATE,
    SICK,
    EXCUSED,
    ABSENT,
    ;

    fun toCode(): String = when (this) {
        PRESENT -> "H"
        LATE -> "T"
        SICK -> "S"
        EXCUSED -> "I"
        ABSENT -> "A"
    }

    companion object {
        fun fromCode(code: String): AttendanceStatus = when (code.uppercase()) {
            "H" -> PRESENT
            "T" -> LATE
            "S" -> SICK
            "I" -> EXCUSED
            "A" -> ABSENT
            else -> PRESENT
        }
    }
}
