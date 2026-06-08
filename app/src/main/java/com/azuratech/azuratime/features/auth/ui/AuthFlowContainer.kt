package com.azuratech.azuratime.features.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 🔐 AUTH FLOW CONTAINER (v3.2.1-ai-native)
 * Unified container for Welcome and Login to prevent navigation flashes.
 * Shares a single [AuthViewModel] lifecycle.
 */
@Composable
fun AuthFlowContainer(
    onNavigateToDashboard: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState.isWelcomeVisible,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
        },
        label = "AuthFlowTransition",
    ) { isWelcome ->
        if (isWelcome) {
            WelcomeScreen(
                onNavigateToLogin = {
                    viewModel.onEvent(AuthUiEvent.GoToLogin)
                },
            )
        } else {
            LoginScreen(
                onNavigateToDashboard = onNavigateToDashboard,
                viewModel = viewModel,
            )
        }
    }
}
