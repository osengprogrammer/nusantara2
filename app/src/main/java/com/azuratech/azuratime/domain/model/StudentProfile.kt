package com.azuratech.azuratime.domain.model

/**
 * 🎓 STUDENT PROFILE - THE SINGLE SOURCE OF TRUTH (SSOT)
 *
 * This domain model represents a complete person within the system, combining
 * their identity (StudentEntity), biometric data (FaceEntity), and class 
 * assignments (FaceAssignmentEntity).
 *
 * All UI components and business logic UseCases should use this model to 
 * maintain consistency across the application layers.
 */
data class StudentProfile(
    val studentId: String,
    val studentCode: String? = null,
    val name: String,
    val schoolId: String,
    val classId: String? = null, // Primary class ID
    val faceId: String? = null,
    val embedding: FloatArray? = null, // Handled by Converters.kt in Room
    val photoUrl: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Computed property to check if biometric data is associated with this student.
     */
    val faceExists: Boolean get() = faceId != null && (embedding != null || photoUrl != null)

    /**
     * Helper to create a copy of the profile with an updated sync status and timestamp.
     */
    fun withStatus(newStatus: SyncStatus) = this.copy(
        syncStatus = newStatus, 
        updatedAt = System.currentTimeMillis()
    )

    // Equals and HashCode overridden for FloatArray content comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StudentProfile

        if (studentId != other.studentId) return false
        if (studentCode != other.studentCode) return false
        if (name != other.name) return false
        if (schoolId != other.schoolId) return false
        if (classId != other.classId) return false
        if (faceId != other.faceId) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (photoUrl != other.photoUrl) return false
        if (syncStatus != other.syncStatus) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = studentId.hashCode()
        result = 31 * result + (studentCode?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + schoolId.hashCode()
        result = 31 * result + (classId?.hashCode() ?: 0)
        result = 31 * result + (faceId?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + (photoUrl?.hashCode() ?: 0)
        result = 31 * result + syncStatus.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
