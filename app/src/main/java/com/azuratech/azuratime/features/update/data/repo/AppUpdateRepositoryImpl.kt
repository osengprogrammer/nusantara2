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
import kotlinx.coroutines.withContext
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
        Log.i("AzuraUpdate", "Repository: Starting update check")
        return try {
            val remoteResult = fetchFromRemoteConfig()
            if (remoteResult is Result.Success) {
                Log.i("AzuraUpdate", "Repository: Update info retrieved from Remote Config")
                remoteResult
            } else {
                Log.i("AzuraUpdate", "Repository: Remote Config failed, falling back to GitHub")
                fetchFromGitHubPages()
            }
        } catch (e: Exception) {
            Log.e("AzuraUpdate", "Repository: Fatal error in check flow", e)
            Result.Failure(AppError.Network("Update check failed: ${e.localizedMessage}"))
        }
    }

    private suspend fun fetchFromRemoteConfig(): Result<AppVersionInfo> = suspendCancellableCoroutine { continuation ->
        Log.d("AzuraUpdate", "Repository: Fetching from Remote Config...")
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val info = AppVersionInfo(
                    versionCode = remoteConfig.getLong("latest_version_code").toInt(),
                    versionName = remoteConfig.getString("latest_version_name"),
                    downloadUrl = remoteConfig.getString("download_url"),
                    releaseNotes = remoteConfig.getString("release_notes"),
                )
                Log.d("AzuraUpdate", "Repository: Remote Config success - Version ${info.versionCode}")
                continuation.resume(Result.Success(info))
            } else {
                Log.w("AzuraUpdate", "Repository: Remote Config fetch was not successful")
                continuation.resume(Result.Failure(AppError.Network("Remote Config Fetch Failed")))
            }
        }
    }

    private suspend fun fetchFromGitHubPages(): Result<AppVersionInfo> = withContext(Dispatchers.IO) {
        Log.d("AzuraUpdate", "Repository: Fetching from GitHub Pages...")
        try {
            val connection = URL(versionUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val info = json.decodeFromString<AppVersionInfo>(response)
                Log.d("AzuraUpdate", "Repository: GitHub Pages success - Version ${info.versionCode}")
                Result.Success(info)
            } else {
                Log.w("AzuraUpdate", "Repository: GitHub Pages HTTP error ${connection.responseCode}")
                Result.Failure(AppError.Network("HTTP Error: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e("AzuraUpdate", "Repository: GitHub Pages exception", e)
            Result.Failure(AppError.Network(e.message))
        }
    }

    override fun downloadApk(url: String, targetFile: File): Flow<Result<Float>> = flow {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val fileLength = connection.contentLength.toLong()
                connection.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        val data = ByteArray(8192)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            output.write(data, 0, count)
                            if (fileLength > 0) {
                                emit(Result.Success(total.toFloat() / fileLength))
                            }
                        }
                    }
                }
                emit(Result.Success(1.0f))
            } else {
                emit(Result.Failure(AppError.Network("HTTP Error: ${connection.responseCode}")))
            }
        } catch (e: Exception) {
            emit(Result.Failure(AppError.Network(e.message)))
        }
    }.flowOn(Dispatchers.IO)
}
