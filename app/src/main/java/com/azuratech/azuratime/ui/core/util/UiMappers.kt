package com.azuratech.azuratime.ui.core.util

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

object UiMappers {
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    /**
     * Maps attendance status codes to their human-readable labels.
     */
    fun statusToLabel(status: String): String {
        return when (status) {
            "H" -> "Hadir"
            "S" -> "Sakit"
            "I" -> "Izin"
            "A" -> "Alpa"
            "In" -> "Hadir"
            else -> "Unknown"
        }
    }

    /**
     * Maps attendance status codes to consistent UI colors.
     */
    fun statusToColor(status: String): Color {
        return when (status) {
            "H", "In", "Hadir" -> Color(0xFF2E7D32) // Success Green
            "S" -> Color(0xFFFBC02D) // Warning Yellow
            "I" -> Color(0xFF1976D2) // Info Blue
            "A" -> Color(0xFFD32F2F) // Error Red
            else -> Color.Gray
        }
    }

    /**
     * Maps attendance status codes to light background colors for cells.
     */
    fun statusToBackgroundColor(status: String): Color {
        return when (status) {
            "H", "In", "Hadir" -> Color(0xFFE8F5E9) // Light Green
            "S" -> Color(0xFFFFFDE7) // Light Yellow
            "I" -> Color(0xFFE3F2FD) // Light Blue
            "A" -> Color(0xFFFFEBEE) // Light Red
            else -> Color.Transparent
        }
    }

    /**
     * Formats a timestamp into a readable date string.
     */
    fun formatTimestamp(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }
}
