package com.airtel.usagetracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airtel.usagetracker.data.models.ExportFormat
import com.airtel.usagetracker.ui.components.FupWarningCard
import com.airtel.usagetracker.ui.components.WeeklyDigestCard
import com.airtel.usagetracker.ui.reports.CalendarTab
import com.airtel.usagetracker.ui.reports.TimelineTab
import com.airtel.usagetracker.ui.reports.TopDaysTab
import com.airtel.usagetracker.ui.reports.TrendsTab
import java.time.LocalDate

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
    val context = LocalContext.current
    val reportsViewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(context)
    )
    
    var selectedTab by remember { mutableStateOf(ReportTab.TIMELINE) }
    
    // Export functionality
    var showExportMenu by remember { mutableStateOf(false) }
    
    // CSV Launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { reportsViewModel.exportData(ExportFormat.CSV, it, context.contentResolver) }
    }

    // JSON Launcher
    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { reportsViewModel.exportData(ExportFormat.JSON, it, context.contentResolver) }
    }
    
    // PDF Launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { reportsViewModel.exportData(ExportFormat.PDF, it, context.contentResolver) }
    }

    // Handle export request
    fun launchExport(format: ExportFormat) {
        val dateStr = LocalDate.now().toString()
        
        when (format) {
            ExportFormat.CSV -> csvLauncher.launch("airtel_usage_$dateStr.csv")
            ExportFormat.JSON -> jsonLauncher.launch("airtel_usage_$dateStr.json")
            ExportFormat.PDF -> pdfLauncher.launch("airtel_usage_$dateStr.pdf")
            else -> {}
        }
    }
    
    // Show toast for error/success messages
    val errorMessage by reportsViewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            if (it == "Export successful" || it.startsWith("Export failed")) {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                if (it == "Export successful") reportsViewModel.clearError()
            }
        }
    }
    
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
                actions = {
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Export"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as CSV") },
                                onClick = {
                                    showExportMenu = false
                                    launchExport(ExportFormat.CSV)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as JSON") },
                                onClick = {
                                    showExportMenu = false
                                    launchExport(ExportFormat.JSON)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showExportMenu = false
                                    launchExport(ExportFormat.PDF)
                                }
                            )
                        }
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
            
            // Cycle Filter
            val availableCycles by reportsViewModel.availableCycles.collectAsState()
            val selectedCycle by reportsViewModel.selectedCycle.collectAsState()
            
            if (availableCycles.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Filter by Billing Cycle",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedCycle?.let {
                                    "${it.cycleStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))} - ${it.cycleEnd.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                                } ?: "All History",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text("Change")
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All History") },
                                    onClick = {
                                        reportsViewModel.selectCycle(null)
                                        expanded = false
                                    }
                                )
                                HorizontalDivider()
                                availableCycles.forEach { cycle ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${cycle.cycleStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))} - ${cycle.cycleEnd.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "${String.format("%.1f", cycle.totalUsageGb)} GB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            reportsViewModel.selectCycle(cycle)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
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
