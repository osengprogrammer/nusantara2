package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CreateSchoolUseCaseTest {
    private val repo = mockk<SchoolRepository>()
    private val useCase = CreateSchoolUseCase(repo)

    @Test
    fun `returns schoolId when name is valid`() = runTest {
        coEvery { repo.createSchool(any(), "Azura Academy", any()) } returns Result.Success("SCH-001")
        val result = useCase("user-123", "Azura Academy")
        assertEquals("SCH-001", (result as Result.Success).data)
    }

    @Test
    fun `fails when name is blank`() = runTest {
        coEvery { repo.createSchool(any(), "", any()) } returns Result.Failure(AppError.BusinessRule("Name blank"))
        val result = useCase("user-123", "")
        assertTrue(result is Result.Failure)
        assertEquals("Name blank", (result as Result.Failure).error.message)
    }
}
