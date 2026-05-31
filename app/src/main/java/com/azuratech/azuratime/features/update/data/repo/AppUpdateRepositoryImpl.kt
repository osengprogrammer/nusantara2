package com.azuratech.azuratime.features.update.data.repo

import android.util.Log
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.update.domain.model.AppVersionInfo
import com.azuratech.azuratime.features.update.domain.repository.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🚀 APP UPDATE REPOSITORY IMPLEMENTATION (v3.2.0-ai-native)
 * Fetches version info from GitHub Pages version.json.
 */
@Singleton
class AppUpdateRepositoryImpl @Inject constructor() : AppUpdateRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val versionUrl = "https://osengprogrammer.github.io/nusantara2/version.json"

    override suspend fun checkForUpdate(): Result<AppVersionInfo> {
        Log.i("AzuraUpdate", "Repository: Fetching version.json from $versionUrl")
        return with(Dispatchers.IO) {
            try {
                val connection = URL(versionUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val content = connection.inputStream.bufferedReader().use { it.readText() }
                    val info = json.decodeFromString<AppVersionInfo>(content)
                    Log.d("AzuraUpdate", "Repository: Data received -> Code: ${info.versionCode}, Name: ${info.versionName}")
                    Result.Success(info)
                } else {
                    Log.e("AzuraUpdate", "Repository: Failed to fetch version.json, HTTP $responseCode")
                    Result.Failure(AppError.Network("HTTP Error: $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("AzuraUpdate", "Repository: Exception during update check", e)
                Result.Failure(AppError.Network(e.localizedMessage ?: "Unknown error"))
            }
        }
    }

    override fun downloadApk(url: String, targetFile: File): Flow<Result<Float>> = flow {
        Log.i("AzuraUpdate", "Repository: Starting download from $url")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "AzuraTime-Updater")

            val responseCode = connection.responseCode
            Log.d("AzuraUpdate", "Repository: Download HTTP Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val fileLength = connection.contentLength.toLong()
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
                                if (progress - lastProgress > 0.01f) {
                                    emit(Result.Success(progress))
                                    lastProgress = progress
                                }
                            } else {
                                emit(Result.Success(0.03f))
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
