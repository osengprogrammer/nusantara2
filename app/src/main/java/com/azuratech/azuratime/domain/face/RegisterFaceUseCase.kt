package com.azuratech.azuratime.domain.face

import android.graphics.Bitmap
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.domain.result.Result
import javax.inject.Inject

class RegisterFaceUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(
        inputId: String,
        classId: String,
        name: String,
        embedding: FloatArray,
        photoBitmap: Bitmap?
    ): Result<RegisterResult> {
        // Map the repository Result<com.azuratech.azuratime.data.repository.RegisterResult> 
        // to domain Result<com.azuratech.azuratime.domain.face.RegisterResult>
        // But wait, if I update FaceRepository to use the domain one, I don't need mapping.
        return repository.registerFace(inputId, classId, name, embedding, photoBitmap)
    }
}
