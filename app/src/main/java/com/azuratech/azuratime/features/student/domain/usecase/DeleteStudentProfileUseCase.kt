package com.azuratech.azuratime.features.student.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import javax.inject.Inject

/**
 * 🎓 DELETE STUDENT PROFILE USE CASE
 * Wraps StudentRepository.deleteProfile() to keep the ViewModel free of direct repository dependencies.
 */
class DeleteStudentProfileUseCase @Inject constructor(
    private val studentRepository: StudentRepository,
) {
    suspend operator fun invoke(studentId: String): Result<Unit> =
        studentRepository.deleteProfile(studentId)
}
