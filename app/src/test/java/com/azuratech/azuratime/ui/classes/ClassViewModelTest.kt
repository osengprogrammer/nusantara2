package com.azuratech.azuratime.ui.classes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.classes.usecase.*
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.data.repo.UserRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClassViewModelTest {

    @MockK lateinit var schoolRepository: SchoolRepository
    @MockK lateinit var importClassesUseCase: ImportClassesUseCase
    @MockK lateinit var userRepository: UserRepository
    @MockK lateinit var sessionManager: SessionManager

    private lateinit var viewModel: ClassViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val schoolId = "school123"
    private val accountId = "account123"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        every { sessionManager.getActiveSchoolId() } returns schoolId
        every { sessionManager.getCurrentUserId() } returns accountId
        every { sessionManager.activeSchoolIdFlow } returns flowOf(schoolId)
        every { sessionManager.currentUserIdFlow } returns flowOf(accountId)
        every { userRepository.observeUserEntity(any()) } returns flowOf(null)
        
        // Default behaviors
        every { schoolRepository.observeClasses(schoolId) } returns flowOf(Result.Loading)
        every { schoolRepository.observeAllClassesForAccount(accountId) } returns flowOf(Result.Success(emptyList()))
        every { schoolRepository.observeSchools(accountId) } returns flowOf(Result.Success(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ClassViewModel(
        SavedStateHandle(mapOf("schoolId" to schoolId, "accountId" to accountId)),
        schoolRepository,
        importClassesUseCase,
        userRepository,
        sessionManager
    )

    @Test
    fun `uiState should emit Loading initially then Success when data is loaded`() = runTest {
        val classes = listOf(ClassModel(id = "1", schoolId = schoolId, name = "Class A", grade = "", teacherId = null, createdAt = 0L))
        val classesFlow = MutableStateFlow<Result<List<ClassModel>>>(Result.Loading)
        every { schoolRepository.observeClasses(schoolId) } returns classesFlow

        viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            classesFlow.value = Result.Success(classes)
            val successItem = awaitItem()
            assertTrue(successItem is UiState.Success)
            assertEquals(classes, (successItem as UiState.Success).data)
        }
    }

    @Test
    fun `uiState should emit Empty when data is empty`() = runTest {
        val classesFlow = flowOf(Result.Success(emptyList<ClassModel>()))
        every { schoolRepository.observeClasses(schoolId) } returns classesFlow

        viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Empty, awaitItem())
        }
    }

    @Test
    fun `addClass should call schoolRepository saveClass`() = runTest {
        coEvery { schoolRepository.saveClass(accountId, schoolId, any()) } returns Result.Success(Unit)
        viewModel = createViewModel()

        viewModel.addClass("New Class")
        advanceUntilIdle()

        coVerify { schoolRepository.saveClass(accountId, schoolId, match { it.name == "New Class" }) }
    }

    @Test
    fun `deleteClass should call schoolRepository deleteClass and trigger callback on success`() = runTest {
        coEvery { schoolRepository.deleteClass(accountId, schoolId, any()) } returns Result.Success(Unit)
        viewModel = createViewModel()
        
        var successCalled = false
        viewModel.deleteClass("1", onSuccess = { successCalled = true })
        advanceUntilIdle()

        coVerify { schoolRepository.deleteClass(accountId, schoolId, "1") }
        assertTrue(successCalled)
    }
}
