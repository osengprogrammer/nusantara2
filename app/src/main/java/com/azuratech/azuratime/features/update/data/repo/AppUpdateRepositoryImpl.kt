package com.azuratech.azuratime.features.update.data.repo

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
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
 */
@Singleton
class AppUpdateRepositoryImpl @Inject constructor() : AppUpdateRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val versionUrl = "https://osengprogrammer.github.io/nusantara2/version.json"

    override suspend fun checkForUpdate(): Result<AppVersionInfo> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(versionUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val info = json.decodeFromString<AppVersionInfo>(response)
                Result.Success(info)
            } else {
                Result.Failure(AppError.Network("HTTP Error: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
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
