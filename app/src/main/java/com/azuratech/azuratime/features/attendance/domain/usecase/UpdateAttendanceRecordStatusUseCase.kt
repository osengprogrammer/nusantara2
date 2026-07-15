package com.azuratech.azuratime.features.attendance.domain.usecase

import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 UPDATE ATTENDANCE RECORD STATUS USE CASE
 * Updates the status (PRESENT, LATE, ABSENT, etc.) of an attendance record.
 */
class UpdateAttendanceRecordStatusUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
) {
    suspend operator fun invoke(recordId: String, status: AttendanceStatus, schoolId: String): Result<Unit> =
        attendanceRepository.updateRecordStatus(recordId, status, schoolId)
}
