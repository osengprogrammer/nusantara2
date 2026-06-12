package com.azuratech.azuratime.features.edge.domain.model

/**
 * ⚡ EDGE DELEGATE CONFIG (v3.6.0)
 */
enum class MLDelegateType {
    CPU,
    GPU,
    NNAPI,
}

data class EdgeDelegateConfig(
    val type: MLDelegateType,
    val numThreads: Int = 4,
    val isAvailable: Boolean = true,
)
