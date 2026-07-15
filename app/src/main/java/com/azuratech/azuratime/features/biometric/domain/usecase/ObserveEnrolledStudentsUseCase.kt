package com.azuratech.azuratime.features.biometric.domain.usecase

import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import com.azuratech.azuratime.core.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEnrolledStudentsUseCase @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(schoolId: String): Flow<Result<List<StudentBiometricEntity>>> =
        biometricRepository.getEnrolledStudentsFlow(schoolId)
}
