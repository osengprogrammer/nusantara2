package com.azuratech.azuratime.features.session.ui

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.session.SessionRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.CreateSessionUseCase
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionPickerViewModelTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var createSessionUseCase: CreateSessionUseCase
    private lateinit var viewModel: SessionPickerViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        schoolRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        createSessionUseCase = mockk(relaxed = true)

        every { sessionManager.activeSchoolIdFlow } returns MutableStateFlow("school_123")
        every { sessionManager.currentAccountIdFlow } returns MutableStateFlow("account_123")
        every { sessionManager.getActiveSchoolId() } returns "school_123"

        every { sessionRepository.observeAllSessionsFlow(any()) } returns flowOf(Result.Success(emptyList()))
        every { accountRepository.getAccountFlow(any()) } returns flowOf(Result.Success(mockk(relaxed = true)))
        every { schoolRepository.observeClassesFlow(any()) } returns flowOf(Result.Success(emptyList()))

        viewModel = SessionPickerViewModel(
            sessionRepository,
            accountRepository,
            schoolRepository,
            sessionManager,
            createSessionUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization observes sessions`() = runTest {
        // GIVEN
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
                    schoolId = "school_123",
                    lookupKey = "key1",
                ),
                subjectName = "Physics",
            ),
        )
        val mockAccount = mockk<com.azuratech.azuratime.features.account.domain.model.Account>(relaxed = true) {
            every { role } returns com.azuratech.azuratime.core.domain.model.AccountRole.ADMIN
            every { memberships } returns emptyMap()
        }
        every { accountRepository.getAccountFlow(any()) } returns flowOf(Result.Success(mockAccount))
        every { sessionRepository.observeAllSessionsFlow("school_123") } returns flowOf(Result.Success(sessions))

        // Trigger observation again if needed, but init already does it.
        // In this test, we might need to recreate the ViewModel to pick up the new mock behavior
        // because init is called in setup.

        viewModel = SessionPickerViewModel(
            sessionRepository,
            accountRepository,
            schoolRepository,
            sessionManager,
            createSessionUseCase,
        )

        // THEN
        val state = viewModel.uiStateFlow.value
        assertEquals(sessions, state.sessions)
    }
}
