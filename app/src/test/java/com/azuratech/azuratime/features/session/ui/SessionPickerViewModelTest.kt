package com.azuratech.azuratime.features.session.ui

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.session.GetSessionsByDayUseCase
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SessionPickerViewModelTest {

    private lateinit var getSessionsByDayUseCase: GetSessionsByDayUseCase
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: SessionPickerViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getSessionsByDayUseCase = mockk()
        sessionManager = mockk()

        every { sessionManager.getActiveSchoolId() } returns "school_123"
        every { sessionManager.getAccountEmail() } returns "supervisor@test.com"

        // Initial state for init block
        every { getSessionsByDayUseCase(any(), any()) } returns flowOf(Result.Success(emptyList()))

        viewModel = SessionPickerViewModel(getSessionsByDayUseCase, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `LoadSessions success updates UiState with sessions`() = runTest {
        // GIVEN
        val sessions = listOf(
            SessionWithDetails(
                session = ClassSessionEntity(
                    sessionId = "s1",
                    classId = "c1",
                    subjectId = "sub1",
                    supervisorEmail = "supervisor@test.com",
                    dayOfWeek = LocalDate.now().dayOfWeek.value,
                    startTime = "08:00",
                    endTime = "09:00",
                    schoolId = "school_123",
                    lookupKey = "",
                ),
                subjectName = "Physics",
            ),
        )
        every { getSessionsByDayUseCase("school_123", any()) } returns flowOf(Result.Success(sessions))

        // WHEN
        viewModel.onEvent(SessionPickerUiEvent.LoadSessions("school_123"))
        advanceUntilIdle()

        // THEN
        val state = viewModel.uiStateFlow.value
        assertEquals(sessions, state.sessions)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `SelectSession emits NavigateToScanner effect`() = runTest {
        // GIVEN
        val effects = mutableListOf<SessionPickerUiEffect>()
        val job = launch {
            viewModel.uiEffectFlow.toList(effects)
        }

        // WHEN
        viewModel.onEvent(SessionPickerUiEvent.SelectSession("s1"))
        advanceUntilIdle()

        // THEN
        assertTrue(effects.any { it is SessionPickerUiEffect.NavigateToScanner && it.sessionId == "s1" })
        job.cancel()
    }
}
