package com.airtel.usagetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airtel.usagetracker.ui.components.FupWarningCard
import com.airtel.usagetracker.ui.components.WeeklyDigestCard
import com.airtel.usagetracker.ui.reports.CalendarTab
import com.airtel.usagetracker.ui.reports.TimelineTab
import com.airtel.usagetracker.ui.reports.TopDaysTab
import com.airtel.usagetracker.ui.reports.TrendsTab

enum class ReportTab(val title: String) {
    TIMELINE("Timeline"),
    CALENDAR("Calendar"),
    TRENDS("Trends"),
    TOP_DAYS("Top Days")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val reportsViewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(context)
    )
    
    var selectedTab by remember { mutableStateOf(ReportTab.TIMELINE) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detailed Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReportTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }
            
            // Tab Content
            when (selectedTab) {
                ReportTab.TIMELINE -> TimelineTab(reportsViewModel)
                ReportTab.CALENDAR -> CalendarTab(reportsViewModel)
                ReportTab.TRENDS -> TrendsTab(reportsViewModel)
                ReportTab.TOP_DAYS -> TopDaysTab(reportsViewModel)
            }
        }
    }
}
