package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun TeacherTasksGrid(
    navController: NavController,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
    ) {
        // ======================================================
        // 🔥 Row 1: QUICK ACTIONS
        // ======================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
        ) {
            DashboardActionCard(
                title = "Scan Barcode", 
                icon = Icons.Default.QrCodeScanner, 
                color = Color(0xFFE53935), 
                onClick = { navController.navigate(Screen.BarcodeScan.route) }, 
                modifier = Modifier.weight(1f)
            )
            DashboardActionCard(
                title = "Input Manual", 
                icon = Icons.Default.EditCalendar, 
                color = Color(0xFF0288D1), 
                onClick = { navController.navigate(Screen.ManualAttendance.createRoute("", "")) }, 
                modifier = Modifier.weight(1f)
            )
        }

        // ======================================================
        // 🔥 Row 2: Scanner Wajah & Cetak Barcode
        // ======================================================
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
        ) {
            DashboardActionCard("Scanner Wajah", Icons.Default.CameraAlt, MaterialTheme.colorScheme.primary,
                { navController.navigate(Screen.CheckIn.route) }, Modifier.weight(1f))

            DashboardActionCard("Cetak Barcode", Icons.Default.QrCode, Color(0xFF673AB7), 
                { navController.navigate(Screen.FaceListBarcode.route) }, Modifier.weight(1f))
        }

        // ======================================================
        // 🔥 Row 3: Admin Only Management
        // ======================================================
        if (isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                DashboardActionCard("Manajemen Kelas", Icons.Default.Groups, MaterialTheme.colorScheme.primary,
                    { navController.navigate(Screen.ClassList.route) }, Modifier.weight(1f))
                DashboardActionCard("Manajemen Siswa", Icons.Default.People, MaterialTheme.colorScheme.secondary,
                    { navController.navigate(Screen.Manage.route) }, Modifier.weight(1f))
            }
        }

        // ======================================================
        // 🔥 Row 4: Analytics
        // ======================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
        ) {
            DashboardActionCard("Laporan Matriks", Icons.Default.GridOn, Color(0xFF2E7D32), {
                navController.navigate(Screen.AttendanceMatrix.route)
            }, Modifier.weight(1f))
        }

        // ======================================================
        // 🔥 Row 5: Admin Tools
        // ======================================================
        if (isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                DashboardActionCard("Registrasi Baru", Icons.Default.PersonAdd, Color(0xFFF57C00),
                    { navController.navigate(Screen.RegistrationMenu.route) }, Modifier.weight(1f))
                DashboardActionCard("Debug System", Icons.Default.BugReport, Color.Gray,
                    { navController.navigate(Screen.Debug.route) }, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardActionCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = AzuraShapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row {
            Box(Modifier.width(6.dp).fillMaxHeight().background(color))
            Column(Modifier.padding(AzuraSpacing.md), verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}