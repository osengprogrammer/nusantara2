package com.azuratech.azuratime.ui

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

// 🔥 Azura Design System Imports
import com.azuratech.azuratime.ui.theme.AzuraSpacing

/**
 * Overlay untuk menggambar kotak wajah di atas Preview Kamera.
 * Dioptimalkan untuk orientasi Portrait, CameraX FILL_CENTER, dan Zero-Copy Mirroring.
 * * Updated: Now uses Azura Design System colors and shapes.
 */
@Composable
fun FaceOverlay(
    faceBounds: List<Rect>,
    imageSize: IntSize,
    @Suppress("UNUSED_PARAMETER") imageRotation: Int = 0,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    paddingFactor: Float = 0.4f // 40% padding untuk membingkai seluruh kepala
) {
    // Jangan menggambar jika data belum siap
    if (faceBounds.isEmpty() || imageSize.width <= 0 || imageSize.height <= 0) return

    // Grab theme colors and standard sizes once
    val primaryColor = MaterialTheme.colorScheme.primary
    val strokeWidth = AzuraSpacing.xs // 4dp stroke for solid visibility

    Canvas(modifier = modifier) {
        // 1. Hitung Skala (Logic FILL_CENTER)
        val scaleX = size.width / imageSize.width.toFloat()
        val scaleY = size.height / imageSize.height.toFloat()
        val scale = max(scaleX, scaleY)

        // 2. Hitung Offset (Pergeseran)
        val offsetX = (size.width - imageSize.width * scale) / 2f
        val offsetY = (size.height - imageSize.height * scale) / 2f

        // Convert dp tokens to pixels for drawing
        val strokeWidthPx = strokeWidth.toPx()
        // Use a standard spacing token for corner radius so it matches buttons/cards
        val cornerRadiusPx = AzuraSpacing.sm.toPx() 

        faceBounds.forEach { rect ->
            // 3. Transformasi Koordinat Mentah ML Kit ke Koordinat Layar
            val mappedLeft = rect.left * scale + offsetX
            val mappedTop = rect.top * scale + offsetY
            val mappedRight = rect.right * scale + offsetX
            val mappedBottom = rect.bottom * scale + offsetY

            // 4. Hitung Lebar, Tinggi, dan Titik Tengah presisi dari wajah
            val width = mappedRight - mappedLeft
            val height = mappedBottom - mappedTop
            var centerX = mappedLeft + width / 2f
            val centerY = mappedTop + height / 2f

            // 5. LOGIKA MIRRORING (Zero-Copy Pipeline)
            if (isFrontCamera) {
                centerX = size.width - centerX
            }

            // 6. Paksa Simetris (Bentuk Kotak Sempurna)
            val longestSide = max(width, height)
            
            // 7. Tambahkan Padding agar menutupi dahi dan dagu
            val finalSideLength = longestSide * (1f + paddingFactor)

            // 8. Hitung posisi koordinat Kiri-Atas (Top-Left) yang baru
            val finalLeft = centerX - (finalSideLength / 2f)
            val finalTop = centerY - (finalSideLength / 2f)

            // 9. Gambar Kotak Azura yang Simetris dan Rounded
            // 🔥 Switched from drawRect to drawRoundRect for premium feel
            drawRoundRect(
                color = primaryColor, // 🔥 Uses theme primary color (Teal)
                topLeft = Offset(finalLeft, finalTop),
                size = Size(finalSideLength, finalSideLength),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx), // 🔥 Rounded corners
                style = Stroke(
                    width = strokeWidthPx // 🔥 Standard system stroke width
                )
            )
        }
    }
}