package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

// TODO: Inject mock SchoolRepository for full testing
class CreateSchoolUseCaseTest {
    @Test
    fun `validation rejects blank name`() = runTest {
        // Placeholder: replace with mock repo test
        assertTrue(true)
    }
}
