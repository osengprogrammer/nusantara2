package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

/**
 * Filter parameters for check-in records.
 */
data class CheckInFilters(
    val name: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val userId: String? = null,
    val classId: String? = null,
    val assignedIds: List<String> = emptyList()
)

/**
 * UseCase to get filtered check-in records for the active school.
 */
class GetCheckInRecordsUseCase @Inject constructor(
    private val localDataSource: CheckInLocalDataSource,
    private val sessionManager: SessionManager
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(filters: CheckInFilters): Flow<Result<List<CheckInRecordEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                localDataSource.getFilteredRecords(
                    nameFilter = filters.name,
                    startDate = filters.startDate,
                    endDate = filters.endDate,
                    userId = filters.userId,
                    classId = filters.classId,
                    assignedIds = filters.assignedIds,
                    schoolId = schoolId
                ).map { Result.Success(it) as Result<List<CheckInRecordEntity>> }
            }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }
}
