package com.azuratech.azuratime.domain.face

import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.domain.result.Result
import javax.inject.Inject

class UpdateEmployeeClassUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(faceId: String, classId: String?): Result<Unit> {
        return repository.updateEmployeeClass(faceId, classId)
    }
}
