package com.azuratech.azuratime.domain.checkin.model

/**
 * Pure Domain Model for Check-In Records.
 * Zero dependencies on Android, Room, or Firebase.
 */
data class CheckInRecord(
    val recordId: String,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val className: String,
    val schoolId: String,
    val timestamp: Long,
    val status: CheckInStatus,
    val photoUrl: String? = null,
    val isSynced: Boolean = false,
    val teacherEmail: String = ""
)

enum class CheckInStatus {
    PRESENT,
    LATE,
    ABSENT,
    EXCUSED;

    fun toCode(): String = when (this) {
        PRESENT -> "H"
        LATE -> "T"
        ABSENT -> "A"
        EXCUSED -> "S"
    }

    companion object {
        fun fromCode(code: String): CheckInStatus = when (code) {
            "H" -> PRESENT
            "T" -> LATE
            "A" -> ABSENT
            "S" -> EXCUSED
            else -> PRESENT
        }
    }
}
