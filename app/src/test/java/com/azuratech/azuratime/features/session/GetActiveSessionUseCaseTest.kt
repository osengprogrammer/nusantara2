package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetActiveSessionUseCaseTest {

    private lateinit var repository: SessionRepository
    private lateinit var useCase: GetActiveSessionUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetActiveSessionUseCase(repository)
    }

    @Test
    fun `invoke returns Success result from repository`() = runTest {
        // GIVEN
        val schoolId = "school_123"
        val email = "test@azura.com"
        val day = 1
        val currentTime = "08:30"
        val mockSession = SessionWithDetails(
            session = ClassSessionEntity(
                sessionId = "s1",
                classId = "c1",
                subjectId = "sub1",
                supervisorEmail = email,
                dayOfWeek = day,
                startTime = "08:00",
                endTime = "09:00",
                schoolId = schoolId,
                lookupKey = "key1",
            ),
            subjectName = "Math",
        )
        coEvery { repository.getActiveSessionOptimized(schoolId, email, day, currentTime) } returns Result.Success(mockSession)

        // WHEN
        val results = useCase(schoolId, email, day, currentTime).toList()

        // THEN
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals(mockSession, (results[1] as Result.Success).data)
    }

    @Test
    fun `invoke returns null when no active session found`() = runTest {
        // GIVEN
        val schoolId = "school_123"
        val email = "test@azura.com"
        val day = 1
        val currentTime = "12:00"
        coEvery { repository.getActiveSessionOptimized(schoolId, email, day, currentTime) } returns Result.Success(null)

        // WHEN
        val results = useCase(schoolId, email, day, currentTime).toList()

        // THEN
        assertTrue(results[1] is Result.Success)
        assertEquals(null, (results[1] as Result.Success).data)
    }
}
