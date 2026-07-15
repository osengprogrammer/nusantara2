package com.azuratech.azuratime.features.attendance.domain.usecase

import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 UPDATE ATTENDANCE RECORD DETAILS USE CASE
 * Updates the class assignment for an attendance record.
 */
class UpdateAttendanceRecordDetailsUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
) {
    suspend operator fun invoke(recordId: String, classId: String, className: String): Result<Unit> =
        attendanceRepository.updateRecord(recordId, classId, className)
}
