package com.azuratech.azuratime.domain.face

import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFacesInClassUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    operator fun invoke(classId: String): Flow<Result<List<FaceEntity>>> {
        return repository.getFacesInClassFlow(classId)
    }
}
