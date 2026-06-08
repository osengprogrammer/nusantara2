package com.azuratech.azuratime.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.ui.designsystem.ZoharChatSheet
import com.azuratech.azuratime.core.ui.navigation.graphs.attendanceGraph
import com.azuratech.azuratime.core.ui.navigation.graphs.dashboardGraph
import com.azuratech.azuratime.core.ui.navigation.graphs.managementGraph
import com.azuratech.azuratime.core.ui.navigation.graphs.reportingGraph
import com.azuratech.azuratime.core.ui.navigation.graphs.accountGraph
import com.azuratech.azuratime.core.util.installApk
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.update.ui.AppUpdateDialog
import com.azuratech.azuratime.features.update.ui.AppUpdateUiEffect
import com.azuratech.azuratime.features.update.ui.AppUpdateUiEvent
import com.azuratech.azuratime.features.update.ui.AppUpdateViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var showZoharChat by remember { mutableStateOf(false) }

    // 🔥 AI Native: In-App Update Integration (v3.2.0-ai-native)
    val updateViewModel: AppUpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        updateViewModel.onEvent(AppUpdateUiEvent.CheckForUpdate)
    }

    LaunchedEffect(Unit) {
        updateViewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is AppUpdateUiEffect.ShowToast -> context.showToast(effect.message)
                is AppUpdateUiEffect.InstallApk -> context.installApk(effect.apkFile)
            }
        }
    }

    AppUpdateDialog(
        state = updateState,
        onEvent = { updateViewModel.onEvent(it) },
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Kontrol UI (BottomBar & FAB)
    val showBottomBar = currentRoute == Screen.Dashboard.route ||
        currentRoute == Screen.AttendanceCapture.route ||
        currentRoute == Screen.BarcodeScan.route

    val showFab = showBottomBar || currentRoute == Screen.AttendanceMatrix.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { showZoharChat = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Zohar Assistant")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNav(navController)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
            ) {
                composable(com.azuratech.azuratime.core.navigation.NavigationRoutes.WELCOME) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        (context as? android.app.Activity)?.recreate()
                    }
                }
                composable(com.azuratech.azuratime.core.navigation.NavigationRoutes.LOGIN) {
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        (context as? android.app.Activity)?.recreate()
                    }
                }
                dashboardGraph(navController)
                attendanceGraph(navController)
                managementGraph(navController)
                reportingGraph(navController)
                accountGraph(navController)
            }

            if (showZoharChat) {
                ZoharChatSheet(
                    viewModel = hiltViewModel(),
                    onDismiss = { showZoharChat = false },
                )
            }
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController) {
    val items = listOf(Screen.Dashboard to "Dashboard", Screen.AttendanceCapture to "Scanner")
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { (screen, label) ->
            NavigationBarItem(
                selected = currentRoute == screen.route || (screen == Screen.AttendanceCapture && currentRoute == Screen.BarcodeScan.route),
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        if (screen == Screen.Dashboard) Icons.Default.Home else Icons.Default.CameraAlt,
                        contentDescription = label,
                    )
                },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun LoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
