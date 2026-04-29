package com.azuratech.azuratime.domain.sync.usecase

import com.azuratech.azuratime.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to check if local database has meaningful data.
 * Used for auto-sync detection after reinstall.
 */
class GetLocalDataCountUseCase @Inject constructor(
    private val database: AppDatabase
) {
    suspend operator fun invoke(accountId: String): Int = withContext(Dispatchers.IO) {
        val schoolCount = database.schoolClassDao().getSchoolCountByAccount(accountId)
        val classCount = database.schoolClassDao().getClassCountByAccount(accountId)
        schoolCount + classCount
    }
}
