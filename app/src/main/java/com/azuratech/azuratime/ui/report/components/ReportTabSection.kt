package com.azuratech.azuratime.ui.report.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun ReportTabSection(
    selectedTabIndex: Int,
    historyCount: Int,
    currentPolicy: String,
    onTabSelected: (Int) -> Unit,
    onPolicySelected: (String) -> Unit
) {
    // 1. Navigation Tabs
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Matrix Rekap", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal)
                }
            }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Detail ($historyCount)")
                }
            }
        )
    }

    // 2. Policy Filter Chips (Visible only on Matrix Tab)
    if (selectedTabIndex == 0) {
        Column(modifier = Modifier.padding(vertical = AzuraSpacing.sm)) {
            Text(
                text = "Metode Perhitungan:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val policies = listOf(
                    "SCHOOL" to "Mode Sekolah",
                    "OFFICE" to "Mode Kantor",
                    "HOURLY" to "Hitung Per Jam"
                )
                
                policies.forEach { (code, label) ->
                    FilterChip(
                        selected = currentPolicy == code,
                        onClick = { onPolicySelected(code) },
                        label = { Text(label) },
                        leadingIcon = if (currentPolicy == code) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}