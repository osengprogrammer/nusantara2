package com.azuratech.azuratime.core.domain.media

/**
 * Result of processing a single photo from any source.
 */
data class PhotoProcessResult(
    val success: Boolean,
    val imageBytes: ByteArray? = null,
    val error: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PhotoProcessResult
        if (success != other.success) return false
        if (error != other.error) return false
        return imageBytes.contentEquals(other.imageBytes)
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}
