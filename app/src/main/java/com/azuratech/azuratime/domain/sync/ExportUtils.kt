package com.azuratech.azuratime.domain.sync

import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuraengine.core.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportUtils @Inject constructor(
    private val storageProvider: StorageProvider
) {
    
    // 🔥 Fungsi Baru untuk Download Template CSV
    fun exportEmptyTemplate(type: String, header: String): String? {
        return try {
            val fileName = "Template_${type}_Azura.csv"
            val data = (header + "\n").toByteArray()
            storageProvider.save(data, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Fungsi utilitas tanggal
    fun calculateDaysBetween(start: LocalDate, end: LocalDate): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(start, end)
    }

    /**
     * 🔥 THE REVENUE REPORT: Export Matrix & Payroll ke CSV Excel
     */
    suspend fun exportMatrixToCsv(
        rows: List<com.azuratech.azuratime.ui.report.MatrixRowModel>,
        dateRange: List<LocalDate>,
        className: String
    ): String? = withContext(Dispatchers.IO) {
        
        val monthStr = dateRange.firstOrNull()?.month?.name ?: "REPORT"
        val fileName = "Azura_Payroll_${className.replace(" ", "_")}_$monthStr.csv"

        try {
            val stringBuilder = StringBuilder()
            
            // 1. HEADER ROW - Dinamis mengikuti Range Tanggal
            val header = mutableListOf("NO", "FACE ID", "NAMA LENGKAP", "KELAS")
            dateRange.forEach { date -> header.add("${date.dayOfMonth}/${date.monthValue}") }
            header.addAll(listOf("REKAP KERJA", "SAKIT", "IZIN", "ALPA", "ESTIMASI GAJI (RP)"))
            
            stringBuilder.appendLine(header.joinToString(",") { escapeCsv(it) })

            // 2. DATA ROWS
            rows.forEachIndexed { index, rowModel ->
                val row = mutableListOf<String>()
                row.add((index + 1).toString())
                row.add(rowModel.studentId) // Format: STUDENTID--UUID
                row.add(rowModel.studentName)
                row.add(rowModel.studentClass)

                rowModel.cells.forEach { cell ->
                    val cleanStatus = cell.text.replace("\n", " ").replace("⚠️", "ERR")
                    row.add(cleanStatus)
                }

                row.add(rowModel.totalHours.ifEmpty { "${rowModel.summaryH} Hadir" })
                row.add(rowModel.summaryS)
                row.add(rowModel.summaryI)
                row.add(rowModel.summaryA)

                // Payroll Calculation (Tanpa Desimal agar Excel-Friendly)
                val salaryValue = rowModel.estimatedSalary.replace(Regex("[^0-9]"), "")
                row.add(if(salaryValue.isEmpty()) "0" else salaryValue)

                stringBuilder.appendLine(row.joinToString(",") { escapeCsv(it) })
            }
            
            val result = storageProvider.save(stringBuilder.toString().toByteArray(), fileName, "Documents")
            println("[Azura_Export] ✅ Report generated: $result")
            result
        } catch (e: Exception) {
            println("ERROR: [Azura_Export] ❌ Export failed: ${e.message}")
            null
        }
    }

    /**
     * Export Log Mentah - Audit Trail untuk pengecekan per-tap
     */
    suspend fun exportRawLogsToCsv(records: List<CheckInRecord>): String? = withContext(Dispatchers.IO) {
        val fileName = "Azura_RawLogs_${System.currentTimeMillis()}.csv"
        
        try {
            val stringBuilder = StringBuilder()
            stringBuilder.appendLine("Face ID,Name,Date,Time,Status,Admin Email")

            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

            records.forEach { record ->
                val dateTime = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(record.timestamp),
                    java.time.ZoneId.systemDefault()
                )
                val row = listOf(
                    record.studentId,
                    record.studentName,
                    dateTime.format(dateFormatter),
                    dateTime.format(timeFormatter),
                    record.status.toCode(),
                    record.teacherEmail
                ).joinToString(",") { escapeCsv(it) }

                stringBuilder.appendLine(row)
            }
            storageProvider.save(stringBuilder.toString().toByteArray(), fileName, "Documents")
        } catch (e: Exception) { null }
    }

    /**
     * 🔥 Export Master Data (Personil, Kelas, Jabatan, dll)
     */
    suspend fun exportMasterDataToCsv(
        dataType: String,
        headers: List<String>,
        rows: List<List<String>>
    ): String? = withContext(Dispatchers.IO) {
        val fileName = "Azura_Master_${dataType}_${System.currentTimeMillis()}.csv"

        try {
            val stringBuilder = StringBuilder()
            stringBuilder.appendLine(headers.joinToString(",") { escapeCsv(it) })
            rows.forEach { rowData ->
                stringBuilder.appendLine(rowData.joinToString(",") { escapeCsv(it) })
            }
            val result = storageProvider.save(stringBuilder.toString().toByteArray(), fileName, "Documents")
            println("[Azura_Export] ✅ Master Data exported: $result")
            result
        } catch (e: Exception) {
            println("ERROR: [Azura_Export] ❌ Master Export failed: ${e.message}")
            null
        }
    }

    private fun escapeCsv(value: String): String {
        val cleanValue = value.replace("\n", " ").replace("\r", " ")
        return if (cleanValue.contains(",") || cleanValue.contains("\"")) {
            "\"${cleanValue.replace("\"", "\"\"")}\""
        } else {
            cleanValue
        }
    }
}
