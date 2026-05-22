package com.azuratech.azuratime.features.update.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.update.domain.model.AppVersionInfo
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * 🚀 APP UPDATE REPOSITORY INTERFACE (v3.2.0-ai-native)
 */
interface AppUpdateRepository {
    /**
     * Check for the latest version info from the remote server.
     */
    suspend fun checkForUpdate(): Result<AppVersionInfo>

    /**
     * Download the APK file from the given URL.
     * Emits the download progress (0.0 to 1.0).
     */
    fun downloadApk(url: String, targetFile: File): Flow<Result<Float>>
}
