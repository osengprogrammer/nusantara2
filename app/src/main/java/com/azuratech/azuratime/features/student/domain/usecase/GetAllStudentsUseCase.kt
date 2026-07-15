package com.azuratech.azuratime.features.student.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import javax.inject.Inject

/**
 * 🎓 GET ALL STUDENTS USE CASE
 * Wraps StudentRepository.getAll() to keep the ViewModel free of direct repository dependencies.
 */
class GetAllStudentsUseCase @Inject constructor(
    private val studentRepository: StudentRepository,
) {
    suspend operator fun invoke(): Result<List<StudentProfile>> =
        studentRepository.getAll()
}
