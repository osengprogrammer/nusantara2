package com.azuratech.azuratime.domain.model

data class ProcessResult(
    val faceId: String,
    val name: String,
    val status: String,
    val type: String = "",        // Tambahan ke-4 agar tidak error
    val message: String = ""      // Tambahan ke-5 untuk pesan error dari repository
)