package com.azuratech.azuratime.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 🔥 DB, ML, & Utils
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.FaceCache
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.ml.matcher.NativeSecurityVault
import com.azuratech.azuratime.ml.recognizer.FaceNetConstants
import com.azuratech.azuratime.core.session.SessionManager

// 🔥 Azura Design System
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes

@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = SessionManager.getInstance(context)
    val schoolId = sessionManager.getActiveSchoolId() ?: "NO_SCHOOL"

    var faces by remember { mutableStateOf<List<FaceEntity>>(emptyList()) }
    var cacheData by remember { mutableStateOf<List<Pair<String, FloatArray>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }

    fun loadDebugData() {
        scope.launch {
            loading = true
            try {
                // 1. Load from database (Filtered by active school)
                val dbFaces = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(context).faceDao().getAllFacesForScanningList(schoolId)
                }
                faces = dbFaces

                // 2. Load from cache (Tenant-specific)
                val cacheList = FaceCache.load(context, schoolId)
                cacheData = cacheList

                // 3. Generate Diagnostics Report
                val info = buildString {
                    appendLine("=== SYSTEM STATUS ===")
                    appendLine("Active School ID: $schoolId")
                    appendLine("Total faces in DB (this school): ${dbFaces.size}")
                    appendLine("Total faces in Cache: ${cacheList.size}")
                    appendLine()

                    appendLine("=== BIOMETRIC SAMPLES ===")
                    dbFaces.take(5).forEachIndexed { index, face ->
                        appendLine("${index + 1}. ${face.name}")
                        appendLine("   ID: ${face.faceId}")
                        appendLine("   Embedding: ${face.embedding?.size ?: "MISSING"}")
                        appendLine("   Synced: ${face.isSynced}")
                        appendLine()
                    }

                    appendLine("=== PIPELINE VALIDATION ===")
                    if (dbFaces.size >= 2) {
                        val f1 = dbFaces[0]
                        val f2 = dbFaces[1]
                        if (f1.embedding != null && f2.embedding != null) {
                            val distance = NativeSecurityVault.calculateDistanceNative(f1.embedding, f2.embedding)
                            appendLine("Cosine Distance [${f1.name}] vs [${f2.name}]:")
                            appendLine(">> Result: ${String.format("%.4f", distance)}")
                            appendLine(">> Match Threshold: ${FaceNetConstants.RECOGNITION_THRESHOLD}")
                            appendLine(">> Verdict: ${if (distance < FaceNetConstants.RECOGNITION_THRESHOLD) "MATCH" else "NO MATCH"}")
                        }
                    }
                }
                debugInfo = info

            } catch (e: Exception) {
                debugInfo = "Diagnostic Failure: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(schoolId) {
        loadDebugData()
    }

    AzuraScreen(title = "System Debug", onBack = onNavigateBack) {
        // 🔥 FIXED: Changed to padding(top = AzuraSpacing.md) to prevent double horizontal padding
        Column(modifier = Modifier.fillMaxSize().padding(top = AzuraSpacing.md)) {
            
            // Stats Row
            Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                DebugStatCard("DB Count", faces.size.toString(), Modifier.weight(1f))
                DebugStatCard("Cache Count", cacheData.size.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(AzuraSpacing.md))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                Button(onClick = { loadDebugData() }, enabled = !loading, modifier = Modifier.weight(1f)) {
                    Text("Refresh")
                }
                OutlinedButton(
                    onClick = { 
                        scope.launch { 
                            FaceCache.clear()
                            loadDebugData()
                        } 
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Purge Cache")
                }
            }

            Spacer(Modifier.height(AzuraSpacing.md))

            // Console Output
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = AzuraShapes.medium
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
                    item {
                        Text(
                            text = debugInfo,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebugStatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = AzuraShapes.medium) {
        Column(Modifier.padding(AzuraSpacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}