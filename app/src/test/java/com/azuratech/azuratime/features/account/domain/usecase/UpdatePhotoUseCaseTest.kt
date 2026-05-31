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
 * 🚀 UPDATE PHOTO USECASE TEST (v3.2.0-ai-native)
 * MVI Contract Test: Event -> State + Effect
 * Pattern: MockK + Turbine + Result<T> handling
 */
class UpdatePhotoUseCaseTest {

    private lateinit var repository: AccountRepository
    private lateinit var updatePhotoUseCase: UpdatePhotoUseCase

    @Before
    fun setup() {
        repository = mockk()
        updatePhotoUseCase = UpdatePhotoUseCase(repository)
    }

    @Test
    fun `when repository returns Success, usecase returns Success`() = runTest {
        // Arrange
        val accountId = "test_id"
        val photoUrl = "https://photo.url"
        coEvery { repository.updatePhoto(accountId, photoUrl) } returns Result.Success(Unit)

        // Act
        val result = updatePhotoUseCase(accountId, photoUrl)

        // Assert
        assert(result is Result.Success)
    }

    @Test
    fun `when repository returns Failure, usecase returns Failure`() = runTest {
        // Arrange
        val accountId = "test_id"
        val photoUrl = "https://photo.url"
        val mockError = AppError.Network("Upload Failed")
        coEvery { repository.updatePhoto(accountId, photoUrl) } returns Result.Failure(mockError)

        // Act
        val result = updatePhotoUseCase(accountId, photoUrl)

        // Assert
        assert(result is Result.Failure)
        assertEquals(mockError, (result as Result.Failure).error)
    }
}
