package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.domain.student.repository.StudentRepository
import javax.inject.Inject

/**
 * UseCase to delete a student profile.
 * Marks the profile for deletion locally and triggers a background sync.
 */
class DeleteStudentProfileUseCase @Inject constructor(
    private val studentRepository: StudentRepository
) {
    suspend operator fun invoke(studentId: String): Result<Unit> {
        return studentRepository.deleteProfile(studentId)
    }
}
