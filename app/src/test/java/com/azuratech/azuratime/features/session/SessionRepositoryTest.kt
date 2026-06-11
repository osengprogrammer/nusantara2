package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionDao
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionRepositoryTest {

    private lateinit var dao: SessionDao
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: SessionRepository

    @Before
    fun setup() {
        dao = mockk()
        sessionManager = mockk()
        repository = SessionRepositoryImpl(dao, sessionManager)
    }

    @Test
    fun `getSessionsByDayFlow returns Success result from DAO`() = runTest {
        // GIVEN
        val schoolId = "school_123"
        val day = 1
        val mockSessions = listOf(
            SessionWithDetails(
                session = ClassSessionEntity(
                    sessionId = "s1",
                    classId = "c1",
                    subjectId = "sub1",
                    supervisorEmail = "test@azura.com",
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "09:00",
                    schoolId = schoolId,
                ),
                subjectName = "Math",
            ),
        )
        every { dao.getSessionsByDayFlow(schoolId, day) } returns flowOf(mockSessions)

        // WHEN
        val result = repository.getSessionsByDayFlow(schoolId, day).first()

        // THEN
        assertTrue(result is Result.Success)
        assertEquals(mockSessions, (result as Result.Success).data)
    }

    @Test
    fun `validateSessionAccess returns true if supervisorEmail matches`() = runTest {
        // GIVEN
        val sessionId = "s1"
        val email = "test@azura.com"
        val mockSession = ClassSessionEntity(
            sessionId = sessionId,
            classId = "c1",
            subjectId = "sub1",
            supervisorEmail = email,
            dayOfWeek = 1,
            startTime = "08:00",
            endTime = "09:00",
            schoolId = "school_123",
        )
        coEvery { dao.getSessionById(sessionId) } returns mockSession
        every { sessionManager.getAccountEmail() } returns email

        // WHEN
        val result = repository.validateSessionAccess(sessionId)

        // THEN
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
    }

    @Test
    fun `validateSessionAccess returns false if supervisorEmail mismatches`() = runTest {
        // GIVEN
        val sessionId = "s1"
        val mockSession = ClassSessionEntity(
            sessionId = sessionId,
            classId = "c1",
            subjectId = "sub1",
            supervisorEmail = "other@azura.com",
            dayOfWeek = 1,
            startTime = "08:00",
            endTime = "09:00",
            schoolId = "school_123",
        )
        coEvery { dao.getSessionById(sessionId) } returns mockSession
        every { sessionManager.getAccountEmail() } returns "test@azura.com"

        // WHEN
        val result = repository.validateSessionAccess(sessionId)

        // THEN
        assertTrue(result is Result.Success)
        assertTrue(!(result as Result.Success).data)
    }
}
