package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.domain.model.SessionType
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 🚀 GET ACTIVE TIERED SESSION USE CASE (v3.7.0-base)
 * Deterministic resolution hierarchy: GLOBAL > CLASS_WIDE > ACADEMIC.
 * Ensures the most relevant session is selected for attendance.
 */
class GetActiveTieredSessionUseCase @Inject constructor(
    private val getSessionsByDayUseCase: GetSessionsByDayUseCase,
) {
    operator fun invoke(
        schoolId: String,
        dayOfWeek: Int,
        studentClassId: String? = null,
    ): Flow<Result<SessionWithDetails?>> {
        val currentTime = LocalTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        return getSessionsByDayUseCase(schoolId, dayOfWeek).map { result ->
            when (result) {
                is Result.Success -> {
                    val sessions = result.data

                    // 1. Filter sessions running right now
                    val activeSessions = sessions.filter { session ->
                        try {
                            val start = LocalTime.parse(session.session.startTime, timeFormatter)
                            val end = LocalTime.parse(session.session.endTime, timeFormatter)
                            !currentTime.isBefore(start) && !currentTime.isAfter(end)
                        } catch (e: Exception) {
                            false
                        }
                    }

                    // 2. Resolve Hierarchy: GLOBAL > CLASS_WIDE > ACADEMIC
                    val resolved = activeSessions
                        .filter { session ->
                            when (session.session.sessionType) {
                                SessionType.GLOBAL -> true
                                SessionType.CLASS_WIDE -> session.session.classId == studentClassId
                                SessionType.ACADEMIC -> session.session.classId == studentClassId
                            }
                        }
                        .sortedWith(
                            compareBy<SessionWithDetails> { it.session.sessionType.priority } // Custom priority
                                .thenBy { it.session.startTime } // Earliest start first
                                .thenBy { it.session.sessionId }, // Final tie-breaker
                        )
                        .firstOrNull()

                    Result.Success(resolved)
                }
                is Result.Failure -> Result.Failure(result.error)
                is Result.Loading -> Result.Loading
            }
        }
    }

    // Helper extension for hierarchy weighting
    private val SessionType.priority: Int
        get() = when (this) {
            SessionType.GLOBAL -> 1
            SessionType.CLASS_WIDE -> 2
            SessionType.ACADEMIC -> 3
        }
}
