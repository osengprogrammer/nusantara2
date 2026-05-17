package com.azuratech.azuratime.features.attendance.domain.model

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
    val photoUrl: String? = null,
    val isSynced: Boolean = false,
    val accountEmail: String = "",
)

enum class AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    EXCUSED,
    ;

    fun toCode(): String = when (this) {
        PRESENT -> "H"
        LATE -> "T"
        ABSENT -> "A"
        EXCUSED -> "S"
    }

    companion object {
        fun fromCode(code: String): AttendanceStatus = when (code) {
            "H" -> PRESENT
            "T" -> LATE
            "A" -> ABSENT
            "S" -> EXCUSED
            else -> PRESENT
        }
    }
}
