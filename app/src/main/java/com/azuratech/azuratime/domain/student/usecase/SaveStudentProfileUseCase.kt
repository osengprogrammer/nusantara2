package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.student.repository.StudentRepository
import javax.inject.Inject

import com.azuratech.azuratime.domain.media.PhotoStorageUtils

/**
 * UseCase to save or update a student profile.
 * Validates required fields and delegates to [StudentRepository].
 */
class SaveStudentProfileUseCase @Inject constructor(
    private val studentRepository: StudentRepository,
    private val photoStorageUtils: PhotoStorageUtils
) {
    suspend operator fun invoke(profile: StudentProfile, photoBytes: ByteArray? = null): Result<Unit> {
        // Validation
        if (profile.name.isBlank()) {
            return Result.Failure(AppError.BusinessRule("Nama siswa tidak boleh kosong"))
        }
        if (profile.schoolId.isBlank()) {
            return Result.Failure(AppError.BusinessRule("School ID tidak boleh kosong"))
        }

        var finalProfile = profile
        
        // Handle photo if provided
        if (photoBytes != null) {
            val faceId = profile.faceId ?: "FACE-${profile.studentId}"
            val photoUrl = photoStorageUtils.saveFacePhoto(photoBytes, faceId)
            finalProfile = profile.copy(photoUrl = photoUrl, faceId = faceId)
        }

        return studentRepository.saveProfile(finalProfile)
    }
}
