package com.prajwal.utilities.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prajwal.utilities.home.HomeScreen
import com.prajwal.utilities.tools.crickettoss.CricketTossScreen
import com.prajwal.utilities.tools.cricketstats.CricketStatsScreen
import com.prajwal.utilities.tools.passwordmanager.PasswordManagerScreen
import com.prajwal.utilities.tools.wifiusage.OnboardingScreen
import com.prajwal.utilities.tools.wifiusage.ReportsScreen
import com.prajwal.utilities.tools.wifiusage.ReportsViewModelFactory
import com.prajwal.utilities.tools.wifiusage.SettingsScreen
import com.prajwal.utilities.tools.wifiusage.UsageViewModelFactory
import com.prajwal.utilities.tools.wifiusage.WifiUsageScreen
import com.prajwal.utilities.tools.wifiusage.data.UsageRepository
import com.prajwal.utilities.tools.workouttracker.WorkoutTrackerScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val usageRepository = remember { UsageRepository(context) }
    val wifiViewModel: com.prajwal.utilities.tools.wifiusage.UsageViewModel = viewModel(
        factory = UsageViewModelFactory(usageRepository)
    )

    val isOnboardingCompleted by wifiViewModel.isOnboardingCompleted.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ── Home Hub ──────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onToolSelected = { route -> navController.navigate(route) }
            )
        }

        // ── WiFi Usage tool ───────────────────────────────────────────
        composable(Screen.WifiUsageDashboard.route) {
            if (isOnboardingCompleted == false) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        wifiViewModel.completeOnboarding()
                    },
                    viewModel = wifiViewModel
                )
            } else {
                WifiUsageScreen(
                    viewModel = wifiViewModel,
                    onNavigateToSettings = { navController.navigate(Screen.WifiUsageSettings.route) },
                    onNavigateToReports = { navController.navigate(Screen.WifiUsageReports.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.WifiUsageSettings.route) {
            SettingsScreen(
                viewModel = wifiViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WifiUsageReports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Cricket Toss ──────────────────────────────────────────────
        composable(Screen.CricketToss.route) {
            CricketTossScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Cricket Stats ─────────────────────────────────────────────
        composable(Screen.CricketStats.route) {
            CricketStatsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Password Manager ──────────────────────────────────────────
        composable(Screen.PasswordManager.route) {
            PasswordManagerScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Workout Tracker ───────────────────────────────────────────
        composable(Screen.WorkoutTracker.route) {
            WorkoutTrackerScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
