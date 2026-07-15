package com.azuratech.azuratime.features.biometric.domain.usecase

import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.core.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStudentsWithDetailsUseCase @Inject constructor(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(schoolId: String): Flow<Result<List<StudentBiometricDetails>>> =
        biometricRepository.getStudentsWithDetailsFlow(schoolId)
}
