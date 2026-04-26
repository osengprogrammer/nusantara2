package com.azuratech.azuratime.ui.classes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.classes.usecase.DeleteClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.ImportClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.UpdateClassUseCase
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.util.UiState
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClassViewModelTest {

    @MockK
    lateinit var getClassesUseCase: GetClassesUseCase

    @MockK
    lateinit var updateClassUseCase: UpdateClassUseCase

    @MockK
    lateinit var deleteClassUseCase: DeleteClassUseCase

    @MockK
    lateinit var importClassesUseCase: ImportClassesUseCase

    @MockK
    lateinit var sessionManager: SessionManager

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
        
        // Default behavior for getClassesUseCase
        every { getClassesUseCase(schoolId) } returns MutableStateFlow(Result.Loading)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ClassViewModel(
        SavedStateHandle(mapOf("schoolId" to schoolId, "accountId" to accountId)),
        getClassesUseCase,
        updateClassUseCase,
        deleteClassUseCase,
        importClassesUseCase,
        sessionManager
    )

    @Test
    fun `uiState should emit Loading initially then Success when data is loaded`() = runTest {
        // Arrange
        val classes = listOf(ClassModel(id = "1", schoolId = schoolId, name = "Class A", grade = "", teacherId = null, createdAt = 0L))
        val classesFlow = MutableStateFlow<Result<List<ClassModel>>>(Result.Loading)
        every { getClassesUseCase(schoolId) } returns classesFlow

        viewModel = createViewModel()

        viewModel.uiState.test {
            // Assert Loading (Initial)
            assertEquals(UiState.Loading, awaitItem())

            // Act
            classesFlow.value = Result.Success(classes)

            // Assert Success
            val successItem = awaitItem()
            assertTrue(successItem is UiState.Success)
            assertEquals(classes, (successItem as UiState.Success).data)
        }
    }

    @Test
    fun `uiState should emit Empty when data is empty`() = runTest {
        // Arrange
        val classesFlow = MutableStateFlow<Result<List<ClassModel>>>(Result.Success(emptyList()))
        every { getClassesUseCase(schoolId) } returns classesFlow

        viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem()) // Initial from stateIn
            assertEquals(UiState.Empty, awaitItem())
        }
    }

    @Test
    fun `uiState should emit Error when use case fails`() = runTest {
        // Arrange
        val errorMessage = "Network Error"
        val classesFlow = MutableStateFlow<Result<List<ClassModel>>>(Result.Failure(AppError.Network(errorMessage)))
        every { getClassesUseCase(schoolId) } returns classesFlow

        viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem()) // Initial from stateIn
            val errorItem = awaitItem()
            assertTrue(errorItem is UiState.Error)
            assertEquals(errorMessage, (errorItem as UiState.Error).message)
        }
    }

    @Test
    fun `addClass should call UpdateClassUseCase`() = runTest {
        // Arrange
        coEvery { updateClassUseCase(accountId, schoolId, any()) } returns Result.Success(Unit)
        viewModel = createViewModel()

        // Act
        viewModel.addClass("New Class")
        advanceUntilIdle()

        // Assert
        coVerify { updateClassUseCase(accountId, schoolId, "New Class") }
    }

    @Test
    fun `deleteClass should call DeleteClassUseCase and trigger callback on success`() = runTest {
        // Arrange
        coEvery { deleteClassUseCase(accountId, schoolId, any()) } returns Result.Success(Unit)
        viewModel = createViewModel()
        
        var successCalled = false
        
        // Act
        viewModel.deleteClass("1", onSuccess = { successCalled = true })
        advanceUntilIdle()

        // Assert
        coVerify { deleteClassUseCase(accountId, schoolId, "1") }
        assertTrue(successCalled)
    }
}
