package com.azuratech.azuratime.features.student.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.azuratech.azuratime.core.ui.components.StudentDisplayItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream

/**
 * 📄 BARCODE PDF GENERATOR (v3.2.0-ai-native)
 * Lightweight utility to generate printable PDF sheets of student QR codes.
 */
object BarcodePdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40
    private const val QR_SIZE = 120
    private const val COLUMNS = 3
    private const val ROWS_PER_PAGE = 5

    fun generateBarcodePdf(
        cacheDir: File,
        schoolId: String,
        students: List<StudentDisplayItem>,
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val textPaint = Paint().apply {
            textSize = 10f
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }

        var studentIndex = 0
        var pageNumber = 1

        while (studentIndex < students.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw Header
            canvas.drawText("Student Barcode List - Page $pageNumber", MARGIN.toFloat(), 30f, titlePaint)
            canvas.drawLine(MARGIN.toFloat(), 40f, (PAGE_WIDTH - MARGIN).toFloat(), 40f, paint)

            val columnWidth = (PAGE_WIDTH - 2 * MARGIN) / COLUMNS
            val rowHeight = (PAGE_HEIGHT - 100) / ROWS_PER_PAGE

            for (row in 0 until ROWS_PER_PAGE) {
                for (col in 0 until COLUMNS) {
                    if (studentIndex >= students.size) break

                    val student = students[studentIndex]
                    val x = MARGIN + col * columnWidth + (columnWidth - QR_SIZE) / 2
                    val y = 60 + row * rowHeight

                    // Generate QR
                    val classId = student.profile.classId ?: "UNASSIGNED"
                    val qrContent = "$schoolId|$classId|${student.profile.studentId}"
                    val qrBitmap = generateQrBitmap(qrContent)

                    if (qrBitmap != null) {
                        canvas.drawBitmap(qrBitmap, x.toFloat(), y.toFloat(), paint)
                    }

                    // Draw Student Info
                    val centerX = MARGIN + col * columnWidth + columnWidth / 2
                    canvas.drawText(
                        student.profile.name.take(20),
                        centerX.toFloat(),
                        (y + QR_SIZE + 15).toFloat(),
                        textPaint,
                    )
                    canvas.drawText(
                        "ID: ${student.profile.studentId}",
                        centerX.toFloat(),
                        (y + QR_SIZE + 28).toFloat(),
                        textPaint,
                    )

                    studentIndex++
                }
                if (studentIndex >= students.size) break
            }

            pdfDocument.finishPage(page)
            pageNumber++
        }

        return try {
            val file = File(cacheDir, "student_barcode_list.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun generateQrBitmap(text: String): Bitmap? {
        return try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
