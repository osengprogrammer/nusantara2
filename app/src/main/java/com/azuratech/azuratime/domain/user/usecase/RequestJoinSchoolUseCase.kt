package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.domain.model.MembershipStatus
import com.azuratech.azuratime.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to handle user request to join a school workspace.
 * Follows SSOT: Saves to Room first, sync happens in background.
 */
class RequestJoinSchoolUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        userId: String,
        schoolId: String,
        schoolName: String,
        requestedRole: String = "TEACHER"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Logic handled by repository to ensure atomicity
            userRepository.updateMembership(
                userId = userId,
                schoolId = schoolId,
                schoolName = schoolName,
                status = MembershipStatus.PENDING,
                role = requestedRole
            )
            
            // TODO: Trigger ProfileSyncWorker.enqueue(userId) for background Firestore update
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }
}
