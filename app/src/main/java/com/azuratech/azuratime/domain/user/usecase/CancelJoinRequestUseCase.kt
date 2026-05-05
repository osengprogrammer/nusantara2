package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.domain.model.MembershipStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to handle user cancelling a pending join request.
 */
class CancelJoinRequestUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        userId: String,
        schoolId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Logic handled by repository: marks as LEFT or removes from map
            userRepository.updateMembership(
                userId = userId,
                schoolId = schoolId,
                schoolName = "", // Name not needed for removal
                status = MembershipStatus.LEFT,
                role = ""
            )
            
            // TODO: Trigger ProfileSyncWorker.enqueue(userId) for background Firestore update
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }
}
