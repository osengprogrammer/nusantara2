package com.azuratech.azuraengine.model

import kotlinx.serialization.Serializable

@Serializable
data class ProcessResult(
    val faceId: String,
    val name: String,
    val status: String,
    val type: String = "",
    val message: String = ""
)
