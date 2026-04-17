package com.azuratech.azuratime.ui.core.designsystem

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.azuratech.azuratime.ml.detector.BarcodeAnalyzer // Import analyzer baru kita
import com.azuratech.azuratime.ui.PermissionsHandler
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.guava.await
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CoreBarcodeCamera(
    modifier: Modifier = Modifier,
    analyzer: BarcodeAnalyzer, // 🔥 Berubah menjadi BarcodeAnalyzer
    useFrontCamera: Boolean = false, // 🔥 Default-nya false (Kamera Belakang) karena scan barcode biasanya pakai kamera belakang
    shape: Shape = AzuraShapes.large 
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    PermissionsHandler(permissionState = cameraPermissionState) {
        val executor = remember { Executors.newSingleThreadExecutor() }
        var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
        val previewView = remember { 
            PreviewView(context).apply { 
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            } 
        }

        DisposableEffect(Unit) {
            onDispose {
                cameraProvider?.unbindAll()
                executor.shutdown()
            }
        }

        LaunchedEffect(cameraPermissionState.status, useFrontCamera, analyzer) {
            if (cameraPermissionState.status.isGranted) {
                try {
                    val provider = ProcessCameraProvider.getInstance(context).await()
                    cameraProvider = provider
                    
                    val preview = Preview.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                                .build()
                        )
                        .build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                    val analysis = ImageAnalysis.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                                .build()
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // Output format YUV_420_888 sangat cocok untuk ML Kit Barcode
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) 
                        .build().also {
                            it.setAnalyzer(executor, analyzer)
                        }
                        
                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA 
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val useCases = mutableListOf<UseCase>(preview, analysis)

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner, 
                        cameraSelector, 
                        *useCases.toTypedArray()
                    )
                } catch (e: Exception) {
                    Log.e("CoreBarcodeCamera", "Camera binding failed", e)
                }
            }
        }

        Box(modifier = modifier.clip(shape)) {
            AndroidView(
                factory = { previewView }, 
                modifier = Modifier.fillMaxSize()
            )
            // 🔥 FaceOverlay Dihapus karena kita tidak menggambar kotak wajah di sini
            // Nanti kamu bisa tambahkan garis panduan scan barcode (opsional) di atas Box ini
        }
    }
}