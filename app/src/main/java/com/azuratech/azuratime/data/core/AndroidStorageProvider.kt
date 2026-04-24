package com.azuratech.azuratime.data.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.azuratech.azuratime.domain.core.StorageProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Singleton
class AndroidStorageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageProvider {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun read(uriString: String): ByteArray {
        return try {
            when {
                uriString.startsWith("http") -> download(uriString)
                uriString.startsWith("content://") -> readFromContentUri(Uri.parse(uriString))
                uriString.startsWith("data:image") -> decodeBase64(uriString)
                uriString.startsWith("file://") -> readFile(File(Uri.parse(uriString).path ?: uriString))
                else -> readFile(File(uriString))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    override fun save(data: ByteArray, filename: String, directory: String?): String {
        return try {
            val targetDir = when (directory) {
                "Documents" -> context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                "faces" -> File(context.filesDir, "faces").apply { if (!exists()) mkdirs() }
                "cache" -> context.cacheDir
                else -> context.filesDir
            } ?: context.filesDir
            
            val file = File(targetDir, filename)
            FileOutputStream(file).use { it.write(data) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    override fun delete(filename: String): Boolean {
        return try {
            val file = File(filename)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    override fun getDatabasePath(name: String): String {
        return context.getDatabasePath(name).absolutePath
    }

    override fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val source = File(sourcePath)
            val dest = File(destPath)
            if (!source.exists()) return false
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun shareFile(path: String, title: String, mimeType: String) {
        try {
            val file = File(path)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun download(url: String): ByteArray {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().use { response ->
            response.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun readFromContentUri(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    }

    private fun readFile(file: File): ByteArray {
        return if (file.exists()) file.readBytes() else ByteArray(0)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64(dataUrl: String): ByteArray {
        val base64Data = if (dataUrl.contains(",")) dataUrl.substringAfter(",") else dataUrl
        return Base64.decode(base64Data)
    }
}
