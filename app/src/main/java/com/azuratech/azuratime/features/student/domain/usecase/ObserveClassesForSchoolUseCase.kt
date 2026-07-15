package com.azuratech.azuratime.features.student.domain.usecase

import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🏫 OBSERVE CLASSES FOR SCHOOL USE CASE
 * Wraps SchoolRepository.observeClassesFlow() to keep the ViewModel free of direct repository dependencies.
 */
class ObserveClassesForSchoolUseCase @Inject constructor(
    private val schoolRepository: SchoolRepository,
) {
    operator fun invoke(schoolId: String): Flow<Result<List<ClassModel>>> =
        schoolRepository.observeClassesFlow(schoolId)
}
