package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.domain.model.SessionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class GetActiveTieredSessionUseCaseTest {

    private val getSessionsByDayUseCase: GetSessionsByDayUseCase = mockk()
    private lateinit var useCase: GetActiveTieredSessionUseCase

    private val schoolId = "school_1"
    private val dayOfWeek = 1
    private val classId = "class_10A"
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Before
    fun setup() {
        useCase = GetActiveTieredSessionUseCase(getSessionsByDayUseCase)
    }

    @Test
    fun `when global session exists, it should take priority over class and academic`() = runTest {
        val now = LocalTime.now()
        val startTime = now.minusMinutes(30).format(timeFormatter)
        val endTime = now.plusMinutes(30).format(timeFormatter)

        val globalSession = createSession("s1", SessionType.GLOBAL, null, startTime, endTime)
        val classSession = createSession("s2", SessionType.CLASS_WIDE, classId, startTime, endTime)
        val academicSession = createSession("s3", SessionType.ACADEMIC, classId, startTime, endTime)

        every { getSessionsByDayUseCase(schoolId, dayOfWeek) } returns flowOf(
            Result.Success(listOf(academicSession, classSession, globalSession)),
        )

        val result = useCase(schoolId, dayOfWeek, classId).first()

        assert(result is Result.Success)
        assertEquals("s1", (result as Result.Success).data?.session?.sessionId)
    }

    @Test
    fun `when only class and academic exist, class session should take priority`() = runTest {
        val now = LocalTime.now()
        val startTime = now.minusMinutes(30).format(timeFormatter)
        val endTime = now.plusMinutes(30).format(timeFormatter)

        val classSession = createSession("s2", SessionType.CLASS_WIDE, classId, startTime, endTime)
        val academicSession = createSession("s3", SessionType.ACADEMIC, classId, startTime, endTime)

        every { getSessionsByDayUseCase(schoolId, dayOfWeek) } returns flowOf(
            Result.Success(listOf(academicSession, classSession)),
        )

        val result = useCase(schoolId, dayOfWeek, classId).first()

        assert(result is Result.Success)
        assertEquals("s2", (result as Result.Success).data?.session?.sessionId)
    }

    @Test
    fun `should filter by student classId for CLASS_WIDE and ACADEMIC`() = runTest {
        val now = LocalTime.now()
        val startTime = now.minusMinutes(30).format(timeFormatter)
        val endTime = now.plusMinutes(30).format(timeFormatter)

        val academicSessionOtherClass = createSession("s3", SessionType.ACADEMIC, "other_class", startTime, endTime)
        val academicSessionMyClass = createSession("s4", SessionType.ACADEMIC, classId, startTime, endTime)

        every { getSessionsByDayUseCase(schoolId, dayOfWeek) } returns flowOf(
            Result.Success(listOf(academicSessionOtherClass, academicSessionMyClass)),
        )

        val result = useCase(schoolId, dayOfWeek, classId).first()

        assert(result is Result.Success)
        assertEquals("s4", (result as Result.Success).data?.session?.sessionId)
    }

    private fun createSession(
        id: String,
        type: SessionType,
        cid: String?,
        start: String,
        end: String,
    ) = SessionWithDetails(
        session = ClassSessionEntity(
            sessionId = id,
            classId = cid,
            subjectId = if (type == SessionType.ACADEMIC) "math" else null,
            sessionType = type,
            supervisorEmail = "teacher@test.com",
            dayOfWeek = dayOfWeek,
            startTime = start,
            endTime = end,
            schoolId = schoolId,
            lookupKey = "${type}_$id",
            isActive = true,
            isSynced = false,
        ),
        subjectName = if (type == SessionType.ACADEMIC) "Math" else "Event",
    )
}
