package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 🚀 GET ACTIVE SESSION USE CASE (v3.4.0)
 * Resolves the currently running session based on system time and day.
 */
class GetActiveSessionUseCase @Inject constructor(
    private val getSessionsByDayUseCase: GetSessionsByDayUseCase,
) {
    operator fun invoke(schoolId: String, dayOfWeek: Int): Flow<Result<SessionWithDetails?>> {
        val currentTime = LocalTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        return getSessionsByDayUseCase(schoolId, dayOfWeek).map { result ->
            when (result) {
                is Result.Success -> {
                    val sessions = result.data
                    val active = sessions.find { session ->
                        val start = LocalTime.parse(session.session.startTime, timeFormatter)
                        val end = LocalTime.parse(session.session.endTime, timeFormatter)
                        !currentTime.isBefore(start) && !currentTime.isAfter(end)
                    }
                    Result.Success(active)
                }
                is Result.Failure -> Result.Failure(result.error)
                is Result.Loading -> Result.Loading
            }
        }
    }
}
