package com.azuratech.azuratime.features.biometric.ui.enroll

/**
 * 🧬 BIOMETRIC ENROLLMENT STATUS (v3.2.0-ai-native)
 */
enum class EnrollmentStatus {
    IDLE,
    CAPTURING,
    PROCESSING,
    SUCCESS,
    FAILURE,
}

/**
 * 🧬 BIOMETRIC ENROLLMENT UI STATE (v3.2.0-ai-native)
 */
data class BiometricEnrollmentUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val enrollmentStatus: EnrollmentStatus = EnrollmentStatus.IDLE,
    val error: String? = null,
    val studentId: String? = null,
    val capturedEmbedding: FloatArray? = null, // Using FloatArray to match ML outputs
) {
    // Required to handle FloatArray in data class copy/equals if needed,
    // but for MVI state we often just compare the object.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BiometricEnrollmentUiState
        if (isLoading != other.isLoading) return false
        if (isScanning != other.isScanning) return false
        if (cameraPermissionGranted != other.cameraPermissionGranted) return false
        if (enrollmentStatus != other.enrollmentStatus) return false
        if (error != other.error) return false
        if (studentId != other.studentId) return false
        if (capturedEmbedding != null) {
            if (other.capturedEmbedding == null) return false
            if (!capturedEmbedding.contentEquals(other.capturedEmbedding)) return false
        } else if (other.capturedEmbedding != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + isScanning.hashCode()
        result = 31 * result + cameraPermissionGranted.hashCode()
        result = 31 * result + enrollmentStatus.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (studentId?.hashCode() ?: 0)
        result = 31 * result + (capturedEmbedding?.contentHashCode() ?: 0)
        return result
    }
}
