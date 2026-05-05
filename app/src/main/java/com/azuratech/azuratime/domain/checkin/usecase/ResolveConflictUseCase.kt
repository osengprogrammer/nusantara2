package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict
import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import com.azuratech.azuraengine.result.Result
import javax.inject.Inject

/**
 * 🏰 RESOLVE CONFLICT USE CASE
 * Resolves a data collision between Local and Cloud records.
 * 🔥 Refactored for Domain Purity: No database or context dependencies.
 */
class ResolveConflictUseCase @Inject constructor(
    private val checkInRepository: CheckInRepository
) {
    suspend operator fun invoke(conflict: AttendanceConflict, useCloud: Boolean): Result<Unit> {
        return checkInRepository.resolveConflict(conflict.conflictId, useCloud)
    }
}
