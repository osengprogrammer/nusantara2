package com.azuratech.azuratime.domain.face

import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.data.local.FaceWithDetails
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFacesWithDetailsUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    operator fun invoke(): Flow<Result<List<FaceWithDetails>>> {
        return repository.allFacesWithDetailsFlow
    }
}
