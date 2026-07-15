package com.azuratech.azuratime.features.attendance.domain.usecase

import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 PROCESS ATTENDANCE USE CASE
 * Delegates attendance processing (check-in) to the repository.
 */
class ProcessAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
) {
    suspend operator fun invoke(params: ProcessAttendanceParams): Result<AttendanceResult> =
        attendanceRepository.processAttendance(params)
}
