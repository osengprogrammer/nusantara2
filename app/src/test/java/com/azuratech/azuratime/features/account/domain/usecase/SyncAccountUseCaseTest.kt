package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.SchoolMembership as LocalSchoolMembership
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/*
 * 🚀 SYNC ACCOUNT USECASE TEST (v3.2.0-ai-native)
 * MVI Contract Test: Event -> State + Effect
 * Pattern: MockK + Turbine + Result<T> handling
 */
class SyncAccountUseCaseTest {

    private lateinit var repository: AccountRepository
    private lateinit var syncAccountUseCase: SyncAccountUseCase

    @Before
    fun setup() {
        repository = mockk()
        syncAccountUseCase = SyncAccountUseCase(repository)
    }

    @Test
    fun `when repository returns Success, usecase returns Success`() = runTest {
        // Arrange
        val accountId = "test_id"
        val mockEntity = AccountEntity(
            accountId = accountId,
            email = "test@azura.com",
            name = "Test User",
            photoUrl = "https://photo.url",
            role = "SUPERVISOR",
            activeSchoolId = "school_123",
            memberships = mapOf(
                "school_123" to LocalSchoolMembership(
                    schoolName = "Azura Academy",
                    role = "SUPERVISOR",
                    status = "ACTIVE",
                ),
            ),
        )
        coEvery { repository.syncAccount(accountId) } returns Result.Success(mockEntity)

        // Act
        val result = syncAccountUseCase(accountId)

        // Assert
        assert(result is Result.Success)
        assertEquals(mockEntity, (result as Result.Success).data)
    }

    @Test
    fun `when repository returns Failure, usecase returns Failure`() = runTest {
        // Arrange
        val accountId = "test_id"
        val mockError = AppError.Network("Sync Failed")
        coEvery { repository.syncAccount(accountId) } returns Result.Failure(mockError)

        // Act
        val result = syncAccountUseCase(accountId)

        // Assert
        assert(result is Result.Failure)
        assertEquals(mockError, (result as Result.Failure).error)
    }
}
