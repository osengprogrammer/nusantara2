package com.azuratech.azuratime.domain.sync

import android.content.Context
import android.os.Environment
import android.util.Log
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.ui.report.DailyAttendance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.time.Duration
import java.time.LocalDate

object ExportUtils {
    
    // 🔥 Fungsi Baru untuk Download Template CSV
    fun exportEmptyTemplate(context: Context, type: String, header: String) {
        try {
            val fileName = "Template_${type}_Azura.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(header + "\n")
            context.showToast("Template berhasil diunduh ke folder aplikasi!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Fungsi utilitas tanggal
    fun calculateDaysBetween(start: LocalDate, end: LocalDate): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(start, end)
    }

    // (Biarkan fungsi export data antum yang lain tetap ada di sini, 
    // pastikan tidak ada pengurangan variabel Long dengan LocalDate)


    /**
     * 🔥 THE REVENUE REPORT: Export Matrix & Payroll ke CSV Excel
     */
    suspend fun exportMatrixToCsv(
        context: Context,
        rows: List<com.azuratech.azuratime.ui.report.MatrixRowModel>,
        dateRange: List<LocalDate>,
        className: String
    ): File? = withContext(Dispatchers.IO) {
        
        val monthStr = dateRange.firstOrNull()?.month?.name ?: "REPORT"
        val fileName = "Azura_Payroll_${className.replace(" ", "_")}_$monthStr.csv"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, fileName)

        try {
            PrintWriter(FileOutputStream(file)).use { writer ->
                
                // 1. HEADER ROW - Dinamis mengikuti Range Tanggal
                val header = mutableListOf("NO", "FACE ID", "NAMA LENGKAP", "KELAS")
                dateRange.forEach { date -> header.add("${date.dayOfMonth}/${date.monthValue}") }
                header.addAll(listOf("REKAP KERJA", "SAKIT", "IZIN", "ALPA", "ESTIMASI GAJI (RP)"))
                
                writer.println(header.joinToString(",") { escapeCsv(it) })

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
                    // Assuming estimatedSalary is formatted string like "Rp 0", strip "Rp " and parse, or just export string
                    val salaryValue = rowModel.estimatedSalary.replace(Regex("[^0-9]"), "")
                    row.add(if(salaryValue.isEmpty()) "0" else salaryValue)

                    writer.println(row.joinToString(",") { escapeCsv(it) })
                }
            }
            Log.d("Azura_Export", "✅ Report generated: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("Azura_Export", "❌ Export failed: ${e.message}")
            null
        }
    }

    /**
     * Export Log Mentah - Audit Trail untuk pengecekan per-tap
     */
    suspend fun exportRawLogsToCsv(context: Context, records: List<CheckInRecordEntity>): File? = withContext(Dispatchers.IO) {
        val fileName = "Azura_RawLogs_${System.currentTimeMillis()}.csv"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, fileName)
        
        try {
            PrintWriter(FileOutputStream(file)).use { writer ->
                writer.println("Face ID,Name,Date,Time,Status,Admin Email")

                records.forEach { record ->
                    val row = listOf(
                        record.faceId,
                        record.name,
                        record.attendanceDate.toString(),
                        record.checkInTime?.toLocalTime()?.toString() ?: "-",
                        record.status,
                        record.userId
                    ).joinToString(",") { escapeCsv(it) }

                    writer.println(row)
                }
            }
            file
        } catch (e: Exception) { null }
    }

    /**
     * 🔥 Export Master Data (Personil, Kelas, Jabatan, dll)
     */
    suspend fun exportMasterDataToCsv(
        context: Context,
        dataType: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File? = withContext(Dispatchers.IO) {
        val fileName = "Azura_Master_${dataType}_${System.currentTimeMillis()}.csv"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, fileName)

        try {
            PrintWriter(FileOutputStream(file)).use { writer ->
                writer.println(headers.joinToString(",") { escapeCsv(it) })
                rows.forEach { rowData ->
                    writer.println(rowData.joinToString(",") { escapeCsv(it) })
                }
            }
            Log.d("Azura_Export", "✅ Master Data exported: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("Azura_Export", "❌ Master Export failed: ${e.message}")
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