package com.azuratech.azuraengine.media

import kotlinx.serialization.Serializable

@Serializable
data class PhotoProcessResult(
    val success: Boolean,
    val imageBytes: ByteArray? = null,
    val error: String? = null
)
