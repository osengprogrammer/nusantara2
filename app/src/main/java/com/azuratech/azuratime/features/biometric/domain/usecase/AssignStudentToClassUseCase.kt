package com.azuratech.azuratime.features.biometric.domain.usecase

import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

class AssignStudentToClassUseCase @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(studentId: String, classId: String): Result<Unit> =
        biometricRepository.assignStudentToClass(studentId, classId)
}
