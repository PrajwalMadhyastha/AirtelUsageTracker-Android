package com.airtel.usagetracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
}

@Composable
fun AppNavHost(
    viewModel: UsageViewModel,
    navController: NavHostController = rememberNavController()
) {
    // Check if onboarding is completed to decide start destination
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState(initial = false)
    // We might need a loading state here if reading from DataStore is async and takes time,
    // but for now we'll assume the initial false is acceptable or handled by a splash screen if needed.
    // However, since we're injecting current state, let's just default to "dashboard" if we can't determine, 
    // BUT actually better to default to onboarding if uncertain.
    // A better approach is to load this state in MainActivity before calling AppNavHost, 
    // but for simplicity let's handle it here or let the startDestination be dynamic?
    // Navigation Compose doesn't support dynamic startDestination easily without state.
    // Let's rely on the ViewModel to provide the initial state or a "Loading" state.
    
    // For this implementation, we will check the state in MainActivity and pass the start destination.
    // But since we are here, let's define the graph.
    
    val startDest = if (isOnboardingCompleted) Screen.Dashboard.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
