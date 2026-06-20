package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionDao
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.data.remote.SessionRemoteDataSource
import com.azuratech.azuratime.core.data.local.AppDatabase
import androidx.room.withTransaction
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

    private lateinit var database: AppDatabase
    private lateinit var dao: SessionDao
    private lateinit var sessionManager: SessionManager
    private lateinit var remoteDataSource: SessionRemoteDataSource
    private lateinit var syncManager: SyncManager
    private lateinit var repository: SessionRepository

    @Before
    fun setup() {
        database = mockk()
        dao = mockk()
        sessionManager = mockk()
        remoteDataSource = mockk()
        syncManager = mockk()
        repository = SessionRepositoryImpl(database, dao, sessionManager, remoteDataSource, syncManager)
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
                    lookupKey = "",
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
    fun `getActiveSessionOptimized returns result from DAO`() = runTest {
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
                lookupKey = "",
            ),
            subjectName = "Math",
        )
        coEvery { dao.getActiveSessionOptimized(schoolId, email, day, currentTime) } returns mockSession

        // WHEN
        val result = repository.getActiveSessionOptimized(schoolId, email, day, currentTime)

        // THEN
        assertTrue(result is Result.Success)
        assertEquals(mockSession, (result as Result.Success).data)
    }

    @Test
    fun `saveSubject calls upsertSubject inside database transaction`() = runTest {
        // GIVEN
        io.mockk.mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction<Unit>(any()) } coAnswers {
            val block = secondArg<suspend () -> Unit>()
            block.invoke()
        }

        val subject = com.azuratech.azuratime.features.session.data.local.SubjectEntity(
            subjectId = "sub123",
            name = "Math",
            description = "Math Desc",
            schoolId = "school123",
            isActive = true,
            isSynced = false,
            isFromTemplate = false,
        )
        coEvery { dao.getSubjectById("sub123") } returns null
        coEvery { dao.getSubjectByName(any(), any()) } returns null
        coEvery { dao.upsertSubject(any()) } returns Unit
        coEvery { syncManager.enqueueSync() } returns Unit

        // WHEN
        val result = repository.saveSubject(subject)

        // THEN
        assertTrue(result is Result.Success)
        io.mockk.coVerify { dao.getSubjectById("sub123") }
        io.mockk.coVerify { dao.upsertSubject(subject.copy(isSynced = false)) }
        io.mockk.coVerify { syncManager.enqueueSync() }
        io.mockk.unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `saveSubject merges isFromTemplate from existing subject`() = runTest {
        // GIVEN
        io.mockk.mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction<Unit>(any()) } coAnswers {
            val block = secondArg<suspend () -> Unit>()
            block.invoke()
        }

        val subject = com.azuratech.azuratime.features.session.data.local.SubjectEntity(
            subjectId = "sub123",
            name = "Math",
            description = "New Desc",
            schoolId = "school123",
            isActive = true,
            isSynced = false,
            isFromTemplate = false,
        )
        val existingSubject = com.azuratech.azuratime.features.session.data.local.SubjectEntity(
            subjectId = "sub123",
            name = "Math",
            description = "Old Desc",
            schoolId = "school123",
            isActive = true,
            isSynced = true,
            isFromTemplate = true,
        )
        coEvery { dao.getSubjectById("sub123") } returns existingSubject
        coEvery { dao.upsertSubject(any()) } returns Unit
        coEvery { syncManager.enqueueSync() } returns Unit

        // WHEN
        val result = repository.saveSubject(subject)

        // THEN
        assertTrue(result is Result.Success)
        io.mockk.coVerify { dao.getSubjectById("sub123") }
        io.mockk.coVerify { dao.upsertSubject(subject.copy(isFromTemplate = true, isSynced = false)) }
        io.mockk.coVerify { syncManager.enqueueSync() }
        io.mockk.unmockkStatic("androidx.room.RoomDatabaseKt")
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
            lookupKey = "",
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
            lookupKey = "",
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
