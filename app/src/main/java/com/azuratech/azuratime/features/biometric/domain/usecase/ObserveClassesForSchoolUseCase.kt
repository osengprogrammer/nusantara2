package com.azuratech.azuratime.features.biometric.domain.usecase

import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.core.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveClassesForSchoolUseCase @Inject constructor(
    private val schoolRepository: SchoolRepository,
) {
    operator fun invoke(schoolId: String): Flow<Result<List<ClassModel>>> =
        schoolRepository.observeClassesFlow(schoolId)
}
