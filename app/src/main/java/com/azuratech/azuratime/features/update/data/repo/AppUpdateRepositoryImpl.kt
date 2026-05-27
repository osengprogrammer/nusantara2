package com.azuratech.azuratime.features.update.data.repo

import android.util.Log
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.update.domain.model.AppVersionInfo
import com.azuratech.azuratime.features.update.domain.repository.AppUpdateRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 🚀 APP UPDATE REPOSITORY IMPLEMENTATION (v3.2.0-ai-native)
 */
@Singleton
class AppUpdateRepositoryImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : AppUpdateRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val versionUrl = "https://osengprogrammer.github.io/nusantara2/version.json"

    override suspend fun checkForUpdate(): Result<AppVersionInfo> {
        Log.i("AzuraUpdate", "Repository: Starting update check via Firebase Remote Config")
        return try {
            fetchFromRemoteConfig()
        } catch (e: Exception) {
            Log.e("AzuraUpdate", "Repository: Fatal error in Firebase check", e)
            Result.Failure(AppError.Network("Firebase update check failed: ${e.localizedMessage}"))
        }
    }

    private suspend fun fetchFromRemoteConfig(): Result<AppVersionInfo> = suspendCancellableCoroutine { continuation ->
        Log.d("AzuraUpdate", "Repository: Fetching from Remote Config...")

        // 🔥 Force refresh: Set interval to 0 for testing
        val configSettings = com.google.firebase.remoteconfig.remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            Log.d("AzuraUpdate", "Repository: Fetch/Activate task completed. Successful: ${task.isSuccessful}")
            if (task.isSuccessful) {
                try {
                    val vCode = remoteConfig.getLong("latest_version_code").toInt()
                    val vName = remoteConfig.getString("latest_version_name")
                    val dUrl = remoteConfig.getString("download_url")

                    Log.d("AzuraUpdate", "Repository: DATA RECEIVED -> Code: $vCode, Name: $vName, URL: $dUrl")
                    Log.d("AzuraUpdate", "Repository: ALL KEYS -> ${remoteConfig.all.keys}")

                    val info = AppVersionInfo(
                        versionCode = vCode,
                        versionName = vName,
                        downloadUrl = dUrl,
                        releaseNotes = remoteConfig.getString("release_notes"),
                    )
                    Log.d("AzuraUpdate", "Repository: Remote Config success - Version ${info.versionCode}")

                    if (info.versionCode == 0) {
                        continuation.resume(Result.Failure(AppError.Network("Remote Config returned empty version (0). Check your keys.")))
                    } else {
                        continuation.resume(Result.Success(info))
                    }
                } catch (e: Exception) {
                    Log.e("AzuraUpdate", "Repository: Error parsing Remote Config data", e)
                    continuation.resume(Result.Failure(AppError.Network("Parsing error: ${e.message}")))
                }
            } else {
                Log.w("AzuraUpdate", "Repository: Remote Config fetch was not successful")
                continuation.resume(Result.Failure(AppError.Network("Remote Config Fetch Failed")))
            }
        }
    }

    // GitHub fallback removed as requested.

    override fun downloadApk(url: String, targetFile: File): Flow<Result<Float>> = flow {
        Log.i("AzuraUpdate", "Repository: Starting download from $url")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000 // Increased timeout

            val responseCode = connection.responseCode
            Log.d("AzuraUpdate", "Repository: Download HTTP Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val fileLength = connection.contentLength.toLong()
                Log.d("AzuraUpdate", "Repository: File length: $fileLength")

                connection.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        val data = ByteArray(8192)
                        var total: Long = 0
                        var count: Int
                        var lastProgress = 0f

                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            output.write(data, 0, count)

                            if (fileLength > 0) {
                                val progress = total.toFloat() / fileLength
                                // Only emit if progress changed significantly (>1%) to avoid UI congestion
                                if (progress - lastProgress > 0.01f) {
                                    Log.v("AzuraUpdate", "Repository: Download progress: ${(progress * 100).toInt()}%")
                                    emit(Result.Success(progress))
                                    lastProgress = progress
                                }
                            } else {
                                // Fallback for unknown length: emit a dummy indeterminate progress or just log
                                Log.v("AzuraUpdate", "Repository: Downloaded $total bytes (unknown total)")
                                emit(Result.Success(0.03f)) // Keep it at 3% to show activity if unknown
                            }
                        }
                    }
                }
                Log.i("AzuraUpdate", "Repository: Download completed successfully")
                emit(Result.Success(1.0f))
            } else {
                Log.e("AzuraUpdate", "Repository: Download failed with HTTP $responseCode")
                emit(Result.Failure(AppError.Network("HTTP Error: $responseCode")))
            }
        } catch (e: Exception) {
            Log.e("AzuraUpdate", "Repository: Download exception", e)
            emit(Result.Failure(AppError.Network(e.message)))
        }
    }.flowOn(Dispatchers.IO)
}
