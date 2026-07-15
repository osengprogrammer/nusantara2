package com.azuratech.azuratime.features.edge.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.edge.domain.model.EdgeDelegateConfig
import com.azuratech.azuratime.features.edge.domain.model.MLDelegateType
import javax.inject.Inject

/**
 * ⚡ CONFIGURE EDGE DELEGATE USE CASE (v3.6.0)
 * Pure logic for detecting hardware capabilities and returning optimal ML delegate.
 */
class ConfigureEdgeDelegateUseCase @Inject constructor() {

    operator fun invoke(preferredType: MLDelegateType? = null): Result<EdgeDelegateConfig> {
        // In a real implementation, this would use Android Build info to check NNAPI/GPU availability.
        // For now, we return a sane default based on preferences.
        val config = when (preferredType) {
            MLDelegateType.GPU -> EdgeDelegateConfig(MLDelegateType.GPU, numThreads = 2)
            MLDelegateType.NNAPI -> EdgeDelegateConfig(MLDelegateType.NNAPI, numThreads = 1)
            else -> EdgeDelegateConfig(MLDelegateType.CPU, numThreads = 4)
        }

        return Result.Success(config)
    }
}
