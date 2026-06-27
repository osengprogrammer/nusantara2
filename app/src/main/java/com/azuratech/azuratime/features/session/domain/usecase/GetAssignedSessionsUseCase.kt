package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.SessionRepository
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.domain.model.SessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject

/**
 * 🚀 GET ASSIGNED SESSIONS USE CASE (v3.7.0)
 * Encapsulates the role-based filtering and ad-hoc fallback generation from Matrix assignments.
 * Strictly checks access boundaries in compliance with ARCHITECTURE.md.
 */
class GetAssignedSessionsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val accountRepository: AccountRepository,
    private val schoolRepository: SchoolRepository,
) {
    /**
     * Executes the loading and authorization filtering.
     *
     * @param schoolId The active school workspace ID.
     * @param accountId The logged-in Account ID.
     * @return Flow emitting results of the combined & filtered sessions list.
     */
    operator fun invoke(schoolId: String, accountId: String): Flow<Result<List<SessionWithDetails>>> {
        return combine(
            sessionRepository.observeAllSessionsFlow(schoolId),
            accountRepository.getAccountFlow(accountId),
            schoolRepository.observeClassesFlow(schoolId),
        ) { sessionsResult, accountResult, classesResult ->
            if (sessionsResult is Result.Loading || accountResult is Result.Loading || classesResult is Result.Loading) {
                return@combine Result.Loading
            }

            if (sessionsResult is Result.Failure) return@combine Result.Failure(sessionsResult.error)
            if (accountResult is Result.Failure) return@combine Result.Failure(accountResult.error)
            if (classesResult is Result.Failure) return@combine Result.Failure(classesResult.error)

            val sessions = (sessionsResult as Result.Success).data
            val account = (accountResult as Result.Success).data
            val classes = (classesResult as Result.Success).data

            val membership = account.memberships[schoolId]
            val role = membership?.role?.let { AccountRole.fromString(it) } ?: account.role
            val assignments = membership?.assignments ?: emptyList()

            // 1. Filter sessions based on Matrix role access
            val filteredSessions = if (role == AccountRole.ADMIN || role == AccountRole.SUPER_ADMIN) {
                sessions
            } else {
                sessions.filter { sessionDetails ->
                    val session = sessionDetails.session
                    assignments.any { assignment ->
                        val classMatches = assignment.classId == session.classId
                        val subjectMatches = assignment.subjectId == null || assignment.subjectId == session.subjectId
                        classMatches && subjectMatches
                    }
                }
            }

            // 2. Populate Class Name for regular sessions
            filteredSessions.forEach { sessionDetails ->
                sessionDetails.className = classes.find { it.id == sessionDetails.session.classId }?.name
            }

            // 3. Fallback to generating Ad-hoc sessions from assignments if scheduled sessions are empty
            val finalSessions = if (filteredSessions.isEmpty() && assignments.isNotEmpty()) {
                assignments.map { assignment ->
                    val classObj = classes.find { it.id == assignment.classId }
                    SessionWithDetails(
                        session = ClassSessionEntity(
                            sessionId = "ADHOC_${assignment.classId}_${assignment.subjectId ?: "ALL"}",
                            classId = assignment.classId,
                            subjectId = assignment.subjectId,
                            sessionType = if (assignment.subjectId != null) SessionType.ACADEMIC else SessionType.CLASS_WIDE,
                            supervisorEmail = account.email,
                            dayOfWeek = 0,
                            startTime = "00:00",
                            endTime = "23:59",
                            schoolId = schoolId,
                            lookupKey = "ADHOC_${UUID.randomUUID()}",
                        ),
                        subjectName = "Matrix Assignment",
                    ).apply {
                        className = classObj?.name ?: "Unknown Class"
                    }
                }
            } else {
                filteredSessions
            }

            Result.Success(finalSessions)
        }
    }
}
