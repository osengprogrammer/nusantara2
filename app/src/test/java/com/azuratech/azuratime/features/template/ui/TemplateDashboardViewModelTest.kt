package com.azuratech.azuratime.features.template.ui

import app.cash.turbine.test
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.features.template.domain.usecase.ApplySchoolTemplateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 🚀 TemplateDashboardViewModelTest.kt (v3.4.0-ai-native)
 * Unit tests for TemplateDashboardViewModel verifying MVI states, events, and effects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var applySchoolTemplateUseCase: ApplySchoolTemplateUseCase
    private lateinit var repository: TemplateRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: TemplateDashboardViewModel

    private val templatesList = listOf(
        SchoolTemplate(
            id = "temp-sd",
            name = "Template Sekolah Dasar",
            category = "SD",
            description = "Template standar untuk SD",
            defaultClassIds = listOf("c1", "c2"),
            defaultSubjectIds = listOf("s1", "s2"),
        ),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        applySchoolTemplateUseCase = mockk()
        repository = mockk()
        sessionManager = mockk()

        // Default stub for init template loading to prevent crashes/unhandled mocks
        coEvery { repository.fetchSchoolTemplates() } returns Result.Success(templatesList)
        coEvery { repository.fetchGlobalClassesByIds(any()) } returns Result.Success(emptyList())
        coEvery { repository.fetchGlobalSubjectsByIds(any()) } returns Result.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization triggers LoadTemplates success and updates state`() = runTest {
        coEvery { repository.fetchSchoolTemplates() } returns Result.Success(templatesList)
        coEvery { repository.fetchGlobalClassesByIds(any()) } returns Result.Success(emptyList())
        coEvery { repository.fetchGlobalSubjectsByIds(any()) } returns Result.Success(emptyList())

        viewModel = TemplateDashboardViewModel(applySchoolTemplateUseCase, repository, sessionManager)

        viewModel.uiStateFlow.test {
            // First item emitted during collection setup/loading
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            // Second item emitted when LoadTemplates onSuccess handles list
            val successState = awaitItem()
            assertEquals(false, successState.isLoading)
            assertEquals(1, successState.templates.size)
            assertEquals(templatesList.first(), successState.templates.first().template)
            assertEquals(null, successState.error)
        }
    }

    @Test
    fun `initialization triggers LoadTemplates failure, updates state and emits snackbar`() = runTest {
        val errorMessage = "Network Connection Timeout"
        coEvery { repository.fetchSchoolTemplates() } returns Result.Failure(AppError.Network(errorMessage))

        viewModel = TemplateDashboardViewModel(applySchoolTemplateUseCase, repository, sessionManager)

        val effects = mutableListOf<TemplateDashboardUiEffect>()
        val effectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffectFlow.collect { effects.add(it) }
        }

        viewModel.uiStateFlow.test {
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            val errorState = awaitItem()
            assertEquals(false, errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
            assertEquals(emptyList<EnrichedSchoolTemplate>(), errorState.templates)
        }

        assertEquals(1, effects.size)
        assertEquals(TemplateDashboardUiEffect.ShowSnackbar("Failed to load templates: $errorMessage"), effects.first())
        effectJob.cancel()
    }

    @Test
    fun `ApplyTemplate success updates state and navigates back`() = runTest {
        val targetTemplate = templatesList.first()

        every { sessionManager.getActiveSchoolId() } returns "school-123"
        every { sessionManager.getCurrentAccountId() } returns "account-456"
        coEvery { applySchoolTemplateUseCase("school-123", "account-456", targetTemplate) } returns Result.Success(Unit)

        viewModel = TemplateDashboardViewModel(applySchoolTemplateUseCase, repository, sessionManager)

        // Skip initial load emissions to focus on ApplyTemplate states
        testScheduler.advanceUntilIdle()

        val effectJob = launch {
            viewModel.uiEffectFlow.test {
                assertEquals(TemplateDashboardUiEffect.ShowToast("Template applied successfully!"), awaitItem())
                assertEquals(TemplateDashboardUiEffect.NavigateBack, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        viewModel.uiStateFlow.test {
            val initialState = awaitItem()
            assertEquals(false, initialState.isApplying)

            viewModel.onEvent(TemplateDashboardUiEvent.ApplyTemplate(targetTemplate))

            // Check transition states
            assertEquals(true, awaitItem().isApplying)
            assertEquals(false, awaitItem().isApplying)
        }

        effectJob.join()
        verify(exactly = 1) { sessionManager.getActiveSchoolId() }
        verify(exactly = 1) { sessionManager.getCurrentAccountId() }
    }

    @Test
    fun `ApplyTemplate failure updates state with error and emits snackbar`() = runTest {
        val targetTemplate = templatesList.first()
        val errorMessage = "SQLite Foreign Key Constraint Violated"

        every { sessionManager.getActiveSchoolId() } returns "school-123"
        every { sessionManager.getCurrentAccountId() } returns "account-456"
        coEvery {
            applySchoolTemplateUseCase("school-123", "account-456", targetTemplate)
        } returns Result.Failure(AppError.LocalDB(errorMessage))

        viewModel = TemplateDashboardViewModel(applySchoolTemplateUseCase, repository, sessionManager)
        testScheduler.advanceUntilIdle()

        val effectJob = launch {
            viewModel.uiEffectFlow.test {
                assertEquals(TemplateDashboardUiEffect.ShowSnackbar("Failed to apply template: $errorMessage"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        viewModel.uiStateFlow.test {
            val initialState = awaitItem()
            assertEquals(false, initialState.isApplying)

            viewModel.onEvent(TemplateDashboardUiEvent.ApplyTemplate(targetTemplate))

            assertEquals(true, awaitItem().isApplying)

            val failureState = awaitItem()
            assertEquals(false, failureState.isApplying)
            assertEquals(errorMessage, failureState.error)
        }

        effectJob.join()
    }

    @Test
    fun `ApplyTemplate with missing session emits warning and does not call UseCase`() = runTest {
        val targetTemplate = templatesList.first()

        every { sessionManager.getActiveSchoolId() } returns null
        every { sessionManager.getCurrentAccountId() } returns "account-456"

        viewModel = TemplateDashboardViewModel(applySchoolTemplateUseCase, repository, sessionManager)
        testScheduler.advanceUntilIdle()

        val effectJob = launch {
            viewModel.uiEffectFlow.test {
                assertEquals(TemplateDashboardUiEffect.ShowSnackbar("Active school or account session not found."), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        viewModel.onEvent(TemplateDashboardUiEvent.ApplyTemplate(targetTemplate))
        testScheduler.advanceUntilIdle()

        effectJob.join()
        coVerify(exactly = 0) { applySchoolTemplateUseCase(any(), any(), any()) }
    }
}
