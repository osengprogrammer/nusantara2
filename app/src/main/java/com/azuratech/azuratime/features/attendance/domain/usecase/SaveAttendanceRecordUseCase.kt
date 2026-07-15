package com.azuratech.azuratime.features.attendance.domain.usecase

import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 SAVE ATTENDANCE RECORD USE CASE
 * Persists an attendance record via the repository.
 */
class SaveAttendanceRecordUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
) {
    suspend operator fun invoke(record: AttendanceRecord, sessionId: String? = null): Result<Unit> =
        attendanceRepository.saveRecord(record, sessionId)
}
