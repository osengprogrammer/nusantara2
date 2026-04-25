package com.azuratech.azuratime.ui.classes

import app.cash.turbine.test
import com.azuratech.azuratime.data.local.ClassEntity
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

    private lateinit var viewModel: ClassViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default behavior for getClassesUseCase
        every { getClassesUseCase() } returns MutableStateFlow(Result.Loading)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState should emit Loading initially then Success when data is loaded`() = runTest {
        // Arrange
        val classes = listOf(ClassEntity(id = "1", name = "Class A"))
        val classesFlow = MutableStateFlow<Result<List<ClassEntity>>>(Result.Loading)
        every { getClassesUseCase() } returns classesFlow

        viewModel = ClassViewModel(
            getClassesUseCase,
            updateClassUseCase,
            deleteClassUseCase,
            importClassesUseCase
        )

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
        val classesFlow = MutableStateFlow<Result<List<ClassEntity>>>(Result.Success(emptyList()))
        every { getClassesUseCase() } returns classesFlow

        viewModel = ClassViewModel(
            getClassesUseCase,
            updateClassUseCase,
            deleteClassUseCase,
            importClassesUseCase
        )

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem()) // Initial from stateIn
            assertEquals(UiState.Empty, awaitItem())
        }
    }

    @Test
    fun `uiState should emit Error when use case fails`() = runTest {
        // Arrange
        val errorMessage = "Network Error"
        val classesFlow = MutableStateFlow<Result<List<ClassEntity>>>(Result.Failure(AppError.Network(errorMessage)))
        every { getClassesUseCase() } returns classesFlow

        viewModel = ClassViewModel(
            getClassesUseCase,
            updateClassUseCase,
            deleteClassUseCase,
            importClassesUseCase
        )

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
        coEvery { updateClassUseCase(any(), any()) } returns Result.Success(Unit)
        viewModel = ClassViewModel(
            getClassesUseCase,
            updateClassUseCase,
            deleteClassUseCase,
            importClassesUseCase
        )

        // Act
        viewModel.addClass("New Class")
        advanceUntilIdle()

        // Assert
        coVerify { updateClassUseCase("New Class", null) }
    }

    @Test
    fun `deleteClass should call DeleteClassUseCase and trigger callback on success`() = runTest {
        // Arrange
        val classEntity = ClassEntity(id = "1", name = "Class A")
        coEvery { deleteClassUseCase(any()) } returns Result.Success(Unit)
        viewModel = ClassViewModel(
            getClassesUseCase,
            updateClassUseCase,
            deleteClassUseCase,
            importClassesUseCase
        )
        
        var successCalled = false
        
        // Act
        viewModel.deleteClass(classEntity, onSuccess = { successCalled = true })
        advanceUntilIdle()

        // Assert
        coVerify { deleteClassUseCase("1") }
        assertTrue(successCalled)
    }
}
