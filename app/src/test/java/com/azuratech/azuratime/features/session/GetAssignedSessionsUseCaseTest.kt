package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.SchoolMembership
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.domain.usecase.GetAssignedSessionsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetAssignedSessionsUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var useCase: GetAssignedSessionsUseCase

    @Before
    fun setup() {
        sessionRepository = mockk()
        accountRepository = mockk()
        schoolRepository = mockk()
        useCase = GetAssignedSessionsUseCase(sessionRepository, accountRepository, schoolRepository)
    }

    @Test
    fun `invoke with ADMIN role returns all sessions`() = runTest {
        // GIVEN
        val schoolId = "school_123"
        val accountId = "account_123"
        val sessions = listOf(
            SessionWithDetails(
                session = ClassSessionEntity(
                    sessionId = "s1",
                    classId = "c1",
                    subjectId = "sub1",
                    supervisorEmail = "supervisor@test.com",
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "09:00",
                    schoolId = schoolId,
                    lookupKey = "key1",
                ),
                subjectName = "Physics",
            ),
        )
        val account = Account(
            accountId = accountId,
            email = "admin@test.com",
            name = "Admin User",
            role = AccountRole.USER,
            memberships = mapOf(
                schoolId to SchoolMembership(
                    schoolName = "Test School",
                    role = "ADMIN",
                    assignments = emptyList(),
                ),
            ),
        )

        every { sessionRepository.observeAllSessionsFlow(schoolId) } returns flowOf(Result.Success(sessions))
        every { accountRepository.getAccountFlow(accountId) } returns flowOf(Result.Success(account))
        every { schoolRepository.observeClassesFlow(schoolId) } returns flowOf(Result.Success(emptyList()))

        // WHEN
        val results = useCase(schoolId, accountId).toList()

        // THEN
        assertTrue(results[0] is Result.Success)
        val successResult = results[0] as Result.Success
        assertEquals(1, successResult.data.size)
        assertEquals("s1", successResult.data[0].session.sessionId)
    }

    @Test
    fun `invoke with supervisor role filters sessions and generates adhoc fallback when empty`() = runTest {
        // GIVEN
        val schoolId = "school_123"
        val accountId = "account_123"
        val sessions = listOf(
            SessionWithDetails(
                session = ClassSessionEntity(
                    sessionId = "s1",
                    classId = "c2", // not assigned class
                    subjectId = "sub1",
                    supervisorEmail = "supervisor@test.com",
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "09:00",
                    schoolId = schoolId,
                    lookupKey = "key1",
                ),
                subjectName = "Physics",
            ),
        )
        val account = Account(
            accountId = accountId,
            email = "supervisor@test.com",
            name = "Supervisor User",
            role = AccountRole.USER,
            memberships = mapOf(
                schoolId to SchoolMembership(
                    schoolName = "Test School",
                    role = "SUPERVISOR",
                    assignments = listOf(
                        TeacherAssignment(classId = "c1", subjectId = "sub2"),
                    ),
                ),
            ),
        )
        val classes = listOf(
            com.azuratech.azuraengine.model.ClassModel(
                id = "c1",
                schoolId = schoolId,
                name = "Class 10-A",
                grade = "10",
                accountId = null,
                createdAt = 0L,
            ),
        )

        every { sessionRepository.observeAllSessionsFlow(schoolId) } returns flowOf(Result.Success(sessions))
        every { accountRepository.getAccountFlow(accountId) } returns flowOf(Result.Success(account))
        every { schoolRepository.observeClassesFlow(schoolId) } returns flowOf(Result.Success(classes))

        // WHEN
        val results = useCase(schoolId, accountId).toList()

        // THEN
        assertTrue(results[0] is Result.Success)
        val successResult = results[0] as Result.Success
        // Since filteredSessions is empty (c2 doesn't match c1), it generates fallback ADHOC session from assignments
        assertEquals(1, successResult.data.size)
        val resolvedSession = successResult.data[0]
        assertTrue(resolvedSession.session.sessionId.startsWith("ADHOC_c1_sub2"))
        assertEquals("Class 10-A", resolvedSession.className)
    }
}
