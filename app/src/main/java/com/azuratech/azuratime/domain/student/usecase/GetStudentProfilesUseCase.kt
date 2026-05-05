package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.student.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to observe all student profiles for the current active school.
 * Delegates to [StudentRepository] as the Single Source of Truth.
 */
class GetStudentProfilesUseCase @Inject constructor(
    private val studentRepository: StudentRepository
) {
    operator fun invoke(): Flow<List<StudentProfile>> {
        return studentRepository.getStudentProfiles()
    }
}
