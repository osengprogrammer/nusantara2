package com.azuratech.azuratime.ui.add

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.RectangleShape
import com.azuratech.azuratime.R
import com.azuratech.azuratime.ml.detector.FaceAnalyzer
import com.azuratech.azuratime.ui.core.designsystem.CoreFaceCamera
import com.azuratech.azuratime.ml.utils.ImageConversionUtils

// Azura Design System Imports
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FaceCaptureScreen(
    mode: CaptureMode,
    onClose: () -> Unit,
    onEmbeddingCaptured: (FloatArray) -> Unit = {},
    onPhotoCaptured: (Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var lastEmbedding by remember { mutableStateOf<FloatArray?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var captureSuccess by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val flashAlpha = remember { Animatable(0f) }
    val checkmarkScale = remember { Animatable(0.5f) }
    
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build() 
    }
    
    var useFrontCamera by remember { mutableStateOf(true) }

    // 🔥 FIXED: Constructor mismatch resolved.
    // bypassLiveness = true because we don't want to force blinks during registration.
    val analyzer = remember(useFrontCamera) {
        FaceAnalyzer(
            isFrontCamera = useFrontCamera,
            bypassLiveness = true, 
            onFaceEmbedding = { _, embedding -> 
                lastEmbedding = embedding 
            },
            onLivenessStatus = { status ->
                Log.d("AzuraCapture", "Liveness Status: $status")
            }
        )
    }

    DisposableEffect(analyzer) {
        onDispose { 
            analyzer.close() 
            capturedBitmap = null 
        }
    }

    LaunchedEffect(showCaptureFeedback) {
        if (showCaptureFeedback) {
            flashAlpha.animateTo(1f, animationSpec = tween(100))
            delay(50)
            flashAlpha.animateTo(0f, animationSpec = tween(300))
            if (captureSuccess) {
                checkmarkScale.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                checkmarkScale.animateTo(1f, tween(300))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        CoreFaceCamera(
            modifier = Modifier.fillMaxSize(),
            analyzer = analyzer,
            useFrontCamera = useFrontCamera,
            imageCapture = if (mode == CaptureMode.PHOTO) imageCapture else null,
            shape = RectangleShape 
        )
        
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha.value)))
        
        if (showCaptureFeedback) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
                ) {
                    if (captureSuccess) {
                        if (mode == CaptureMode.PHOTO && capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured photo",
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(AzuraShapes.medium)
                            )
                        }
                        Box(
                            modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, "Success", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(50.dp).scale(checkmarkScale.value))
                        }
                        Text(
                            text = if (mode == CaptureMode.EMBEDDING) "Embedding Captured!" else "Photo Captured!", 
                            color = Color.White, 
                            style = MaterialTheme.typography.headlineSmall
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp), 
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Text("Processing...", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        
        val isFaceDetected = analyzer.faceBounds.isNotEmpty()
        
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = AzuraSpacing.xl),
            shape = AzuraShapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isFaceDetected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) 
                                 else Color.Black.copy(alpha = 0.6f)
            )
        ) {
            Text(
                text = if (!isFaceDetected) "🔍 Searching for face..." 
                       else if (mode == CaptureMode.EMBEDDING) "✅ Face detected! Tap Capture" 
                       else "✅ Face detected! Tap Photo",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm)
            )
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AzuraSpacing.xl),
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.lg)
        ) {
            Button(
                onClick = onClose, 
                enabled = !isProcessing, 
                shape = AzuraShapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel", modifier = Modifier.padding(horizontal = AzuraSpacing.sm))
            }

            Button(
                onClick = {
                    if (isProcessing) return@Button
                    coroutineScope.launch {
                        isProcessing = true
                        showCaptureFeedback = true
                        captureSuccess = false

                        when(mode) {
                            CaptureMode.EMBEDDING -> {
                                lastEmbedding?.let { embedding ->
                                    delay(800)
                                    onEmbeddingCaptured(embedding)
                                    captureSuccess = true
                                    delay(1000)
                                    isProcessing = false
                                    onClose()
                                } ?: run {
                                    isProcessing = false
                                    showCaptureFeedback = false
                                }
                            }
                            CaptureMode.PHOTO -> {
                                imageCapture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            coroutineScope.launch {
                                                try {
                                                    val finalBitmap = ImageConversionUtils.convertImageProxyToBitmap(
                                                        imageProxy = imageProxy,
                                                        isFrontCamera = useFrontCamera,
                                                        applyMirroring = true 
                                                    )
                                                    
                                                    if (finalBitmap != null) {
                                                        capturedBitmap = finalBitmap
                                                        captureSuccess = true
                                                        delay(1200)
                                                        capturedBitmap = null 
                                                        delay(50) 
                                                        onPhotoCaptured(finalBitmap) 
                                                    } else {
                                                        Log.e("FaceCaptureScreen", "Bitmap conversion returned null")
                                                        captureSuccess = false
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("FaceCaptureScreen", "JPEG Conversion failed", e)
                                                    captureSuccess = false
                                                } finally {
                                                    isProcessing = false
                                                    if (!captureSuccess) showCaptureFeedback = false else onClose()
                                                    imageProxy.close()
                                                }
                                            }
                                        }
                                        override fun onError(e: ImageCaptureException) {
                                            Log.e("FaceCaptureScreen", "Capture failed", e)
                                            isProcessing = false
                                            showCaptureFeedback = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                enabled = !isProcessing && (mode == CaptureMode.PHOTO || lastEmbedding != null),
                shape = AzuraShapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = if (mode == CaptureMode.EMBEDDING) "Capture Embedding" else "Take Photo",
                        modifier = Modifier.padding(horizontal = AzuraSpacing.sm)
                    )
                }
            }
        }
        
        IconButton(
            onClick = onClose, 
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AzuraSpacing.md)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            enabled = !isProcessing
        ) {
            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.padding(AzuraSpacing.xs))
        }
        
        IconButton(
            onClick = { useFrontCamera = !useFrontCamera }, 
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(AzuraSpacing.md)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Cameraswitch, "Switch Camera", tint = Color.White, modifier = Modifier.padding(AzuraSpacing.xs))
        }
        
        if (!showCaptureFeedback && !isFaceDetected) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.face_outline), "Guide", colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.3f)), modifier = Modifier.size(200.dp))
            }
        }
    }
}

enum class CaptureMode { EMBEDDING, PHOTO }