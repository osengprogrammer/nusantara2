package com.azuratech.azuratime.features.biometric.domain.usecase

import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

class SyncBiometricsUseCase @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        biometricRepository.syncBiometrics()
}
