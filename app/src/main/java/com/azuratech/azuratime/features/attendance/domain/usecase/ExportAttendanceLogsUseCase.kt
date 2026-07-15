package com.azuratech.azuratime.features.attendance.domain.usecase

import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 EXPORT ATTENDANCE LOGS USE CASE
 * Exports the given attendance records to a CSV file and returns the file path.
 */
class ExportAttendanceLogsUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
) {
    suspend operator fun invoke(records: List<AttendanceRecord>): Result<String> =
        attendanceRepository.exportLogs(records)
}
