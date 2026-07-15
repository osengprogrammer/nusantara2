package com.azuratech.azuratime.features.student.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🎓 OBSERVE STUDENT PROFILES USE CASE
 * Wraps StudentRepository.getStudentProfilesFlow() to keep the ViewModel free of direct repository dependencies.
 */
class ObserveStudentProfilesUseCase @Inject constructor(
    private val studentRepository: StudentRepository,
) {
    operator fun invoke(): Flow<Result<List<StudentProfile>>> =
        studentRepository.getStudentProfilesFlow()
}
