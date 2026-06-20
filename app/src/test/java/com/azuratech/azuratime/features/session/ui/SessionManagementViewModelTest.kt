package com.azuratech.azuratime.features.session.ui

import app.cash.turbine.test
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.CreateSessionUseCase
import com.azuratech.azuratime.features.session.SessionRepository
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.features.template.domain.model.SubjectTemplate
import io.mockk.coEvery
import io.mockk.coVerify
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
class SessionManagementViewModelTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var createSessionUseCase: CreateSessionUseCase
    private lateinit var templateRepository: TemplateRepository
    private lateinit var viewModel: SessionManagementViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        schoolRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        createSessionUseCase = mockk(relaxed = true)
        templateRepository = mockk(relaxed = true)

        every { sessionManager.activeSchoolIdFlow } returns MutableStateFlow("school_123")
        every { sessionManager.currentAccountIdFlow } returns MutableStateFlow("account_123")
        every { sessionManager.getActiveSchoolId() } returns "school_123"

        every { sessionRepository.observeAllSubjectsFlow(any()) } returns flowOf(Result.Success(emptyList()))
        every { sessionRepository.observeAllSessionsFlow(any()) } returns flowOf(Result.Success(emptyList()))
        every { schoolRepository.observeClassesFlow(any()) } returns flowOf(Result.Success(emptyList()))
        every { accountRepository.getAccountFlow(any()) } returns flowOf(Result.Success(mockk(relaxed = true)))
        coEvery { templateRepository.fetchAllGlobalSubjects() } returns Result.Success(emptyList())

        viewModel = SessionManagementViewModel(
            sessionRepository,
            schoolRepository,
            accountRepository,
            sessionManager,
            createSessionUseCase,
            templateRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `UpdateSubject guardrail rejects editing template subjects`() = runTest {
        // GIVEN
        val subject = SubjectEntity(
            subjectId = "template_sub",
            name = "Mathematics",
            description = "Core math",
            schoolId = "school_123",
            isActive = true,
            isSynced = true,
            isFromTemplate = true, // 👈 Template subject
        )

        // WHEN
        viewModel.uiEffectFlow.test {
            viewModel.onEvent(SessionManagementUiEvent.UpdateSubject(subject, "New Math Name", "New Desc"))

            // THEN
            val effect = awaitItem()
            assertEquals(SessionManagementUiEffect.ShowToast("Template subjects cannot be edited."), effect)
            cancelAndConsumeRemainingEvents()
        }

        // Verify saveSubject was never called
        coVerify(exactly = 0) { sessionRepository.saveSubject(any()) }
    }

    @Test
    fun `UpdateSubject allows editing custom subjects`() = runTest {
        // GIVEN
        val subject = SubjectEntity(
            subjectId = "custom_sub",
            name = "Guitar Class",
            description = "Extracurricular music class",
            schoolId = "school_123",
            isActive = true,
            isSynced = false,
            isFromTemplate = false, // 👈 Custom subject
        )
        val expectedUpdatedSubject = subject.copy(
            name = "Acoustic Guitar",
            description = "Intermediate level",
        )
        coEvery { sessionRepository.saveSubject(any()) } returns Result.Success(Unit)

        // WHEN
        viewModel.uiEffectFlow.test {
            viewModel.onEvent(
                SessionManagementUiEvent.UpdateSubject(
                    subject,
                    "Acoustic Guitar",
                    "Intermediate level",
                ),
            )

            // THEN
            val effect = awaitItem()
            assertEquals(SessionManagementUiEffect.ShowToast("Subject updated"), effect)
            cancelAndConsumeRemainingEvents()
        }

        // Verify saveSubject was called with the correct details
        coVerify(exactly = 1) { sessionRepository.saveSubject(expectedUpdatedSubject) }
    }

    @Test
    fun `AddSubject matching template sets isFromTemplate to true`() = runTest {
        // GIVEN
        val template = SubjectTemplate(id = "sub_math", name = "Mathematics", category = "MIPA")
        coEvery { templateRepository.fetchAllGlobalSubjects() } returns Result.Success(listOf(template))

        // Rebuild viewModel to pick up new template setup in init
        viewModel = SessionManagementViewModel(
            sessionRepository,
            schoolRepository,
            accountRepository,
            sessionManager,
            createSessionUseCase,
            templateRepository,
        )

        coEvery { sessionRepository.saveSubject(any()) } returns Result.Success(Unit)

        // WHEN
        viewModel.onEvent(SessionManagementUiEvent.AddSubject("Mathematics", "Study of numbers"))

        // THEN
        coVerify(exactly = 1) {
            sessionRepository.saveSubject(match {
                it.name == "Mathematics" && it.isFromTemplate && it.description == "Study of numbers"
            })
        }
    }

    @Test
    fun `AddSubject not matching template sets isFromTemplate to false`() = runTest {
        // GIVEN
        coEvery { templateRepository.fetchAllGlobalSubjects() } returns Result.Success(emptyList())

        // Rebuild viewModel
        viewModel = SessionManagementViewModel(
            sessionRepository,
            schoolRepository,
            accountRepository,
            sessionManager,
            createSessionUseCase,
            templateRepository,
        )

        coEvery { sessionRepository.saveSubject(any()) } returns Result.Success(Unit)

        // WHEN
        viewModel.onEvent(SessionManagementUiEvent.AddSubject("Quantum Physics", "Advanced Physics"))

        // THEN
        coVerify(exactly = 1) {
            sessionRepository.saveSubject(match {
                it.name == "Quantum Physics" && !it.isFromTemplate && it.description == "Advanced Physics"
            })
        }
    }
}
