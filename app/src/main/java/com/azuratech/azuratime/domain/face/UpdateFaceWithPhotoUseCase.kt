package com.azuratech.azuratime.domain.face

import android.graphics.Bitmap
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.domain.result.Result
import javax.inject.Inject

class UpdateFaceWithPhotoUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(
        face: FaceEntity,
        photoBitmap: Bitmap?,
        embedding: FloatArray
    ): Result<Unit> {
        return repository.updateFaceWithPhoto(face, photoBitmap, embedding)
    }
}
