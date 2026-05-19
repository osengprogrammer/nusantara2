package com.azuratech.azuratime.core.domain.sync

import com.azuratech.azuraengine.core.StorageProvider
import com.azuratech.azuraengine.sync.CsvStudentData
import com.azuratech.azuraengine.sync.CsvParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📝 AZURA CSV IMPORT UTILS (v3.2.0-ai-native)
 * Optimized bulk data engine with standardized terminology and flexible parsing.
 */
@Singleton
class CsvImportUtils @Inject constructor(
    private val storageProvider: StorageProvider,
) {

    suspend fun parseCsvToStudentData(uriString: String): List<CsvStudentData> {
        return parseCsvFile(uriString).students
    }

    suspend fun parseCsvFile(uriString: String): CsvParseResult = withContext(Dispatchers.IO) {
        val students = mutableListOf<CsvStudentData>()
        val errors = mutableListOf<String>()
        var totalRows = 0
        var validRows = 0

        try {
            val bytes = storageProvider.read(uriString)
            if (bytes.isEmpty()) {
                return@withContext CsvParseResult(emptyList(), listOf("File kosong atau tidak dapat dibaca"), 0, 0)
            }

            BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use { reader ->
                var line: String?
                var lineNumber = 0
                var headers: List<String>? = null

                while (reader.readLine().also { line = it } != null) {
                    lineNumber++
                    val currentLine = line?.trim() ?: continue
                    if (currentLine.isEmpty()) continue

                    val columns = parseCsvLine(currentLine)
                    if (columns.isEmpty()) continue

                    // 🔥 AI Native: Clean headers (lowercase, no spaces, no BOM)
                    if (lineNumber == 1) {
                        headers = columns.map { it.lowercase().replace(" ", "").replace("_", "").replace("\uFEFF", "") }
                        continue
                    }

                    if (headers == null) {
                        errors.add("Baris $lineNumber: Header tidak ditemukan.")
                        continue
                    }

                    totalRows++

                    try {
                        val student = parseStudentRow(headers, columns)
                        if (student != null) {
                            students.add(student)
                            validRows++
                        } else {
                            errors.add("Baris $lineNumber: Student ID (student_id) wajib diisi.")
                        }
                    } catch (e: Exception) {
                        errors.add("Baris $lineNumber: Error format (${e.message})")
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Gagal membaca file: ${e.message}")
        }

        CsvParseResult(students, errors, totalRows, validRows)
    }

    private fun parseStudentRow(headers: List<String>, columns: List<String>): CsvStudentData? {
        val headerMap = headers.mapIndexed { index, h -> h to index }.toMap()

        fun getValue(aliases: List<String>): String {
            for (alias in aliases) {
                val cleanAlias = alias.lowercase().replace(" ", "").replace("_", "")
                val index = headerMap[cleanAlias] ?: continue
                if (index < columns.size) {
                    return columns[index].trim().removeSurrounding("\"")
                }
            }
            return ""
        }

        // 1. CANONICAL DATA (Strictly prioritized)
        val studentId = getValue(listOf("studentid", "id", "faceid", "noinduk", "nis"))
        val name = getValue(listOf("fullname", "name", "nama", "namalengkap"))

        if (studentId.isEmpty()) return null

        // 2. METADATA
        val metadata = mutableMapOf<String, String>()
        metadata["CLASS"] = getValue(listOf("classname", "classid", "class", "kelas"))
        metadata["ROLE"] = getValue(listOf("role", "jabatan"))
        metadata["GRADE"] = getValue(listOf("grade", "tingkat"))

        return CsvStudentData(
            faceId = studentId, // Mapping canonical student_id to internal faceId property
            name = name,
            photoUrl = getValue(listOf("photourl", "photo", "image", "foto", "urlfoto")),
            rawMetadata = metadata,
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when (char) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString().trim())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    /**
     * 🔥 CANONICAL TEMPLATE: face_id is now student_id
     */
    fun generateSampleCsv(): String {
        return "student_id,full_name,class_name,photo_url\n" +
            "STU-001,Gus Usman,Shift Pagi,https://link-to-photo.com/1.jpg\n" +
            "STU-002,Zohar,Shift Malam,https://link-to-photo.com/2.jpg"
    }
}
