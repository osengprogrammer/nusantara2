package com.azuratech.azuratime.features.update.data.repo

import android.util.Log
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.update.domain.model.AppVersionInfo
import com.azuratech.azuratime.features.update.domain.repository.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🚀 APP UPDATE REPOSITORY IMPLEMENTATION (v3.2.0-ai-native)
 * Decoupled from Firebase. Fetches directly from GitHub Pages version.json.
 */
@Singleton
class AppUpdateRepositoryImpl @Inject constructor() : AppUpdateRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val versionUrl = "https://osengprogrammer.github.io/nusantara2/version.json"

    override suspend fun checkForUpdate(): Result<AppVersionInfo> {
        val cacheBusterUrl = "$versionUrl?t=${System.currentTimeMillis()}"
        Log.i("AzuraUpdate", "Repository: Checking for updates at $cacheBusterUrl")

        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL(cacheBusterUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    useCaches = false
                    // Force no caching at the protocol level
                    addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                    addRequestProperty("Pragma", "no-cache")
                    addRequestProperty("Expires", "0")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val content = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("AzuraUpdate", "Repository: Raw JSON received -> $content")

                    val info = json.decodeFromString<AppVersionInfo>(content)
                    Log.d("AzuraUpdate", "Repository: Version check successful. Remote: ${info.versionCode}")
                    Result.Success(info)
                } else {
                    Log.e("AzuraUpdate", "Repository: HTTP Error $responseCode")
                    Result.Failure(AppError.Network("Server returned HTTP $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("AzuraUpdate", "Repository: Network Exception", e)
                Result.Failure(AppError.Network(e.localizedMessage ?: "Network connection failed"))
            }
        }
    }

    override fun downloadApk(url: String, targetFile: File): Flow<Result<Float>> = flow {
        Log.i("AzuraUpdate", "Repository: Starting download from $url")
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "AzuraTime-Updater")
            }

            val responseCode = connection.responseCode
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
                Log.i("AzuraUpdate", "Repository: Download complete")
                emit(Result.Success(1.0f))
            } else {
                Log.e("AzuraUpdate", "Repository: Download HTTP Error $responseCode")
                emit(Result.Failure(AppError.Network("HTTP $responseCode")))
            }
        } catch (e: Exception) {
            Log.e("AzuraUpdate", "Repository: Download failed", e)
            emit(Result.Failure(AppError.Network(e.message)))
        }
    }.flowOn(Dispatchers.IO)
}
