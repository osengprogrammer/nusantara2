package com.azuratech.azuratime.features.school.ui.classes

import app.cash.turbine.test
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClassViewModelTest {

    @MockK lateinit var schoolRepository: SchoolRepository

    @MockK lateinit var userRepository: AccountRepository

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
        every { sessionManager.activeSchoolIdFlow } returns MutableStateFlow(schoolId)
        every { sessionManager.currentUserIdFlow } returns MutableStateFlow(accountId)
        every { userRepository.observeAccountEntity(any()) } returns flowOf(null)

        // Default behaviors
        every { schoolRepository.observeClasses(schoolId) } returns flowOf(Result.Success(emptyList()))
        coEvery { schoolRepository.getClasses(schoolId) } returns Result.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ClassViewModel(
        schoolRepository,
        userRepository,
        sessionManager,
    )

    @Test
    fun `initial state should be idle with empty classes`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isLoading)
            assertTrue(initialState.classes.isEmpty())
            assertNull(initialState.error)
        }
    }

    @Test
    fun `LoadClasses event should update state to Success when data is loaded`() = runTest {
        val classes = listOf(ClassModel(id = "1", schoolId = schoolId, name = "Class A", grade = "10", teacherId = null, createdAt = 0L))
        coEvery { schoolRepository.getClasses(schoolId) } returns Result.Success(classes)
        every { schoolRepository.observeClasses(schoolId) } returns flowOf(Result.Success(classes))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(classes, state.classes)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `CreateClass event should call repository saveClass`() = runTest {
        coEvery { schoolRepository.saveClass(accountId, schoolId, any()) } returns Result.Success(Unit)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ClassUiEvent.CreateClass("New Class"))
        advanceUntilIdle()

        coVerify { schoolRepository.saveClass(accountId, schoolId, match { it.name == "New Class" }) }
    }

    @Test
    fun `ConfirmDeleteClass event should call repository deleteClass`() = runTest {
        val classToDelete = ClassModel(id = "1", schoolId = schoolId, name = "Delete Me", grade = "10", teacherId = null, createdAt = 0L)
        coEvery { schoolRepository.deleteClass(accountId, schoolId, "1") } returns Result.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        // 1. Request delete
        viewModel.onEvent(ClassUiEvent.RequestDeleteClass(classToDelete))
        assertEquals(classToDelete, viewModel.uiState.value.classToDelete)

        // 2. Confirm delete
        viewModel.onEvent(ClassUiEvent.ConfirmDeleteClass)
        advanceUntilIdle()

        coVerify { schoolRepository.deleteClass(accountId, schoolId, "1") }
        assertNull(viewModel.uiState.value.classToDelete)
    }
}
