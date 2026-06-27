package com.azuratech.azuratime.features.session.ui

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.session.CreateSessionUseCase
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.domain.usecase.GetAssignedSessionsUseCase
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

    private lateinit var getAssignedSessionsUseCase: GetAssignedSessionsUseCase
    private lateinit var sessionManager: SessionManager
    private lateinit var createSessionUseCase: CreateSessionUseCase
    private lateinit var viewModel: SessionPickerViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAssignedSessionsUseCase = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        createSessionUseCase = mockk(relaxed = true)

        every { sessionManager.activeSchoolIdFlow } returns MutableStateFlow("school_123")
        every { sessionManager.currentAccountIdFlow } returns MutableStateFlow("account_123")
        every { sessionManager.getActiveSchoolId() } returns "school_123"

        every { getAssignedSessionsUseCase(any(), any()) } returns flowOf(Result.Success(emptyList()))

        viewModel = SessionPickerViewModel(
            getAssignedSessionsUseCase,
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
        every { getAssignedSessionsUseCase("school_123", "account_123") } returns flowOf(Result.Success(sessions))

        viewModel = SessionPickerViewModel(
            getAssignedSessionsUseCase,
            sessionManager,
            createSessionUseCase,
        )

        // THEN
        val state = viewModel.uiStateFlow.value
        assertEquals(sessions, state.sessions)
    }
}
