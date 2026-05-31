package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/*
 * 🚀 UPDATE DISPLAY NAME USECASE TEST (v3.2.0-ai-native)
 * MVI Contract Test: Event -> State + Effect
 * Pattern: MockK + Turbine + Result<T> handling
 */
class UpdateDisplayNameUseCaseTest {

    private lateinit var repository: AccountRepository
    private lateinit var updateDisplayNameUseCase: UpdateDisplayNameUseCase

    @Before
    fun setup() {
        repository = mockk()
        updateDisplayNameUseCase = UpdateDisplayNameUseCase(repository)
    }

    @Test
    fun `when repository returns Success, usecase returns Success`() = runTest {
        // Arrange
        val accountId = "test_id"
        val newName = "New Name"
        coEvery { repository.updateDisplayName(accountId, newName) } returns Result.Success(Unit)

        // Act
        val result = updateDisplayNameUseCase(accountId, newName)

        // Assert
        assert(result is Result.Success)
    }

    @Test
    fun `when repository returns Failure, usecase returns Failure`() = runTest {
        // Arrange
        val accountId = "test_id"
        val newName = "New Name"
        val mockError = AppError.LocalDB("Update Failed")
        coEvery { repository.updateDisplayName(accountId, newName) } returns Result.Failure(mockError)

        // Act
        val result = updateDisplayNameUseCase(accountId, newName)

        // Assert
        assert(result is Result.Failure)
        assertEquals(mockError, (result as Result.Failure).error)
    }
}
