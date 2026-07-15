package com.azuratech.azuratime.features.biometric.domain.usecase

import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.core.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEnrollmentsUseCase @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(): Flow<Result<List<BiometricEnrollmentProfile>>> =
        biometricRepository.observeEnrollmentsFlow()
}
