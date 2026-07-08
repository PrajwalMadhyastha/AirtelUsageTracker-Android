package com.prajwal.utilities.tools.wealthtracker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.prajwal.utilities.tools.wealthtracker.tabs.MilestonesTab
import com.prajwal.utilities.tools.wealthtracker.tabs.PortfolioTab
import com.prajwal.utilities.tools.wealthtracker.tabs.ReportsTab

private enum class WealthTab(val label: String, val icon: ImageVector) {
    PORTFOLIO("Portfolio", Icons.Default.Wallet),
    REPORTS("Reports", Icons.Default.BarChart),
    MILESTONES("Milestones", Icons.Default.EmojiEvents)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthTrackerScreen(
    viewModel: WealthTrackerViewModel,
    onNavigateBack: () -> Unit
) {
    val snapshots by viewModel.snapshots.collectAsState()
    val snapshotsChronological by viewModel.snapshotsChronological.collectAsState()
    val calculatorSettings by viewModel.calculatorSettings.collectAsState()
    val prefillSnapshot by viewModel.prefillSnapshot.collectAsState()
    var selectedTab by remember { mutableStateOf(WealthTab.PORTFOLIO) }

    // Load latest snapshot for pre-fill on first composition
    LaunchedEffect(Unit) { viewModel.loadPrefill() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wealth Tracker", fontWeight = FontWeight.Bold)
                        Text(
                            "Your personal portfolio hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                WealthTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier) }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                WealthTab.PORTFOLIO -> PortfolioTab(
                    snapshots = snapshots,
                    prefill = prefillSnapshot,
                    onSave = { snapshot ->
                        viewModel.saveSnapshot(snapshot)
                        viewModel.loadPrefill()
                    },
                    onDelete = viewModel::deleteSnapshot
                )

                WealthTab.REPORTS -> ReportsTab(
                    snapshots = snapshots,
                    snapshotsChronological = snapshotsChronological
                )

                WealthTab.MILESTONES -> MilestonesTab(
                    latestSnapshot = snapshots.firstOrNull(),
                    settings = calculatorSettings,
                    onMonthlyInvestmentChange = viewModel::updateMonthlyInvestment,
                    onStepupChange = viewModel::updateAnnualStepup,
                    onReturnChange = viewModel::updateExpectedReturn
                )
            }
        }
    }
}
