package com.azuratech.azuratime.features.attendance.domain.usecase

import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 DELETE ATTENDANCE RECORD USE CASE
 * Removes an attendance record by ID and school ID.
 */
class DeleteAttendanceRecordUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
) {
    suspend operator fun invoke(recordId: String, schoolId: String): Result<Unit> =
        attendanceRepository.deleteRecord(recordId, schoolId)
}
