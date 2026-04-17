package com.azuratech.azuratime.domain.sync

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * AZURA CSV IMPORT UTILS
 * Mesin pembaca data massal yang tahan banting terhadap berbagai format Excel/CSV.
 */
object CsvImportUtils {
    
    // 🔥 DATA RAMPING: Hanya data identitas inti dan metadata fleksibel
    data class CsvStudentData(
        val faceId: String,
        val name: String = "",
        val photoUrl: String = "",
        // Metadata mentah (String) dari CSV untuk diproses ke FaceAssignment & FaceSalaryConfig
        val rawMetadata: Map<String, String> = emptyMap() 
    )
    
    data class CsvParseResult(
        val students: List<CsvStudentData>,
        val errors: List<String>,
        val totalRows: Int,
        val validRows: Int
    )

    // 🔥 JEMBATAN KE VIEWMODEL: Memastikan sinkron dengan RegisterViewModel
    suspend fun parseCsvToStudentData(context: Context, uri: Uri): List<CsvStudentData> {
        return parseCsvFile(context, uri).students
    }

    // MESIN UTAMA PARSING CSV
    suspend fun parseCsvFile(context: Context, uri: Uri): CsvParseResult = withContext(Dispatchers.IO) {
        val students = mutableListOf<CsvStudentData>()
        val errors = mutableListOf<String>()
        var totalRows = 0
        var validRows = 0
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var lineNumber = 0
                    var headers: List<String>? = null
                    
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty()) continue
                        
                        val columns = parseCsvLine(currentLine)
                        if (columns.isEmpty()) continue

                        // 🔥 KUNCI PERBAIKAN: Bersihkan spasi, underscore (_), dan BOM
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
                                errors.add("Baris $lineNumber: ID (face_id) wajib diisi.")
                            }
                        } catch (e: Exception) {
                            errors.add("Baris $lineNumber: Error format (${e.message})")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Gagal membaca file: ${e.message}")
        }
        
        CsvParseResult(students, errors, totalRows, validRows)
    }

    // 🔥 PARSE ROW: Super Fleksibel dengan Alias
    private fun parseStudentRow(headers: List<String>, columns: List<String>): CsvStudentData? {
        val headerMap = headers.mapIndexed { index, h -> h to index }.toMap()
        
        // Fungsi pencari kolom cerdas (Abaikan spasi, underscore & huruf besar/kecil)
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
        
        // 1. DATA WAJIB (Sudah mendeteksi face_id dari template)
        val faceId = getValue(listOf("faceid", "id", "noinduk", "nis", "nisn"))
        val name = getValue(listOf("fullname", "name", "nama", "namalengkap"))
        
        // Jika ID kosong, baris ini diabaikan (invalid)
        if (faceId.isEmpty()) return null

        // 2. METADATA FLEKSIBEL (Boleh Kosong)
        val metadata = mutableMapOf<String, String>()
        metadata["CLASS"] = getValue(listOf("classid", "class", "kelas", "shift"))
        metadata["ROLE"] = getValue(listOf("role", "jabatan", "peran"))
        metadata["GRADE"] = getValue(listOf("grade", "tingkat", "departemen"))

        // 💰 3. SENSOR GAJI (Fokus Garmen / Pabrik)
        metadata["BASE_SALARY"] = getValue(listOf("gajipokok", "pokok", "basesalary", "gajibulan"))
        metadata["HOURLY_RATE"] = getValue(listOf("upahperjam", "rate", "gajiperjam", "hourlyrate"))
        metadata["INCENTIVE"] = getValue(listOf("uangmakan", "makan", "insentif", "hadir"))
        metadata["TRANSPORT"] = getValue(listOf("uangtransport", "transport", "transportasi"))

        return CsvStudentData(
            faceId = faceId,
            name = name,
            // 🔥 Tambahan photourl untuk nangkap dari template
            photoUrl = getValue(listOf("photourl", "photo", "image", "foto", "urlfoto")),
            rawMetadata = metadata
        )
    }

    // Pemecah baris CSV yang tahan terhadap koma di dalam tanda kutip ("Zohar, S.Kom")
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
                    } else inQuotes = !inQuotes
                }
                ',' -> {
                    if (inQuotes) current.append(char)
                    else {
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

    // Update Sample CSV agar HRD tahu format terlengkapnya
    fun generateSampleCsv(): String {
        return "face_id,full_name,class_id,photo_url\n" +
               "KRY-001,Gus Usman,Shift Pagi,https://link-foto-gus-usman.com/foto.jpg\n" +
               "KRY-002,Zohar,Shift Malam,https://link-foto-zohar.com/foto.jpg"
    }
}