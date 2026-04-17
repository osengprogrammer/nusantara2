package com.azuratech.azuratime.ui.add

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.azuratech.azuratime.data.local.FaceWithDetails
import com.azuratech.azuratime.ui.core.designsystem.FaceAvatar
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun StudentBarcodeCard(
    detail: FaceWithDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AzuraShapes.medium,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaceAvatar(photoPath = detail.face.photoUrl, size = 56)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.face.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(detail.className ?: "Belum ada kelas", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Icon(Icons.Default.QrCode, contentDescription = "Show QR", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BarcodeExpandedDialog(
    detail: FaceWithDetails,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var barcodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(detail.face.faceId) {
        barcodeBitmap = generateQrCode(detail.face.faceId)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(detail.face.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(detail.className ?: "No Class", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))

                if (barcodeBitmap != null) {
                    Image(
                        bitmap = barcodeBitmap!!.asImageBitmap(),
                        contentDescription = "Barcode",
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(280.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        barcodeBitmap?.let {
                            shareBarcode(context, it, detail.face.name)
                        }
                    },
                    enabled = barcodeBitmap != null,
                    shape = AzuraShapes.medium
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan Kartu")
                }
            }
        }
    }
}

private suspend fun generateQrCode(text: String): Bitmap? = withContext(Dispatchers.Default) {
    try {
        val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512, null)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareBarcode(context: Context, bitmap: Bitmap, studentName: String) {
    val file = File(context.cacheDir, "barcodes")
    file.mkdirs()
    val imageFile = File(file, "barcode_${studentName.replace(" ", "_")}.png")
    val fOut = FileOutputStream(imageFile)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
    fOut.flush()
    fOut.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Kartu QR untuk $studentName")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan via"))
}
