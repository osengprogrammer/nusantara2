package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.model.ClassModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 📚 UseCase to fetch available classes from master catalog.
 * Currently returns a predefined list of standard classes.
 */
class GetAvailableClassesUseCase @Inject constructor() {
    
    private val standardClasses = listOf(
        "10-IPA-1", "10-IPA-2", "10-IPA-3",
        "10-IPS-1", "10-IPS-2", "10-IPS-3",
        "11-IPA-1", "11-IPA-2", "11-IPA-3",
        "11-IPS-1", "11-IPS-2", "11-IPS-3",
        "12-IPA-1", "12-IPA-2", "12-IPA-3",
        "12-IPS-1", "12-IPS-2", "12-IPS-3"
    )

    operator fun invoke(): Flow<List<String>> {
        println("📚 DEBUG: Fetched ${standardClasses.size} available classes")
        return flowOf(standardClasses)
    }
}
