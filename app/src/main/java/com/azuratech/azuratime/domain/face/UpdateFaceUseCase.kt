package com.azuratech.azuratime.domain.face

import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.domain.result.Result
import javax.inject.Inject

class UpdateFaceUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(face: FaceEntity): Result<Unit> {
        return repository.updateFaceBasic(face)
    }
}
