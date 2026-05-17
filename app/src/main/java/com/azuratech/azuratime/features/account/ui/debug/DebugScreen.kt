package com.azuratech.azuratime.features.account.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 🔥 DB, ML, & Utils
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.data.local.BiometricCache
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.ml.matcher.NativeSecurityVault
import com.azuratech.azuratime.ml.recognizer.FaceNetConstants
import com.azuratech.azuratime.core.session.SessionManager

// 🔥 Azura Design System
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraShapes

@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = SessionManager.getInstance(context)
    val schoolId = sessionManager.getActiveSchoolId() ?: "NO_SCHOOL"

    var biometrics by remember { mutableStateOf<List<StudentBiometricEntity>>(emptyList()) }
    var cacheData by remember { mutableStateOf<List<Pair<String, FloatArray>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }

    fun loadDebugData() {
        scope.launch {
            loading = true
            try {
                // 1. Load from database (Filtered by active school)
                val dbBiometrics = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(context).biometricDao().getAllStudentsForScanningList(schoolId)
                }
                biometrics = dbBiometrics

                // 2. Load from cache (Tenant-specific)
                val cacheList = BiometricCache.load(context, schoolId)
                cacheData = cacheList

                // 3. Generate Diagnostics Report
                val info = buildString {
                    appendLine("=== SYSTEM STATUS ===")
                    appendLine("Active School ID: $schoolId")
                    appendLine("Total students in DB (this school): ${dbBiometrics.size}")
                    appendLine("Total biometrics in Cache: ${cacheList.size}")
                    appendLine()

                    appendLine("=== BIOMETRIC SAMPLES ===")
                    dbBiometrics.take(5).forEachIndexed { index, item ->
                        appendLine("${index + 1}. ${item.name}")
                        appendLine("   ID: ${item.studentId}")
                        appendLine("   Embedding: ${item.embedding?.size ?: "MISSING"}")
                        appendLine("   Synced: ${item.isSynced}")
                        appendLine()
                    }

                    appendLine("=== PIPELINE VALIDATION ===")
                    if (dbBiometrics.size >= 2) {
                        val b1 = dbBiometrics[0]
                        val b2 = dbBiometrics[1]
                        if (b1.embedding != null && b2.embedding != null) {
                            val distance = NativeSecurityVault.calculateDistanceNative(b1.embedding, b2.embedding)
                            appendLine("Cosine Distance [${b1.name}] vs [${b2.name}]:")
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
                DebugStatCard("DB Count", biometrics.size.toString(), Modifier.weight(1f))
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
                            BiometricCache.clear()
                            loadDebugData()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Purge Cache")
                }
            }

            Spacer(Modifier.height(AzuraSpacing.md))

            // Console Output
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = AzuraShapes.medium,
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
                    item {
                        Text(
                            text = debugInfo,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
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
