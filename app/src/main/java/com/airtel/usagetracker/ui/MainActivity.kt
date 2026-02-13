package com.airtel.usagetracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airtel.usagetracker.data.ReportsRepository
import com.airtel.usagetracker.data.UsageRepository
import com.airtel.usagetracker.ui.components.FupWarningCard
import com.airtel.usagetracker.ui.components.WeeklyDigestCard
import com.airtel.usagetracker.ui.theme.AirtelUsageTrackerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AirtelUsageTrackerTheme {
                val repository = remember { UsageRepository(applicationContext) }
                val viewModel: UsageViewModel = viewModel(
                    factory = UsageViewModelFactory(repository)
                )
                
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
                
                if (isOnboardingCompleted == null) {
                    // Loading state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AppNavHost(
                        viewModel = viewModel,
                        startDestination = if (isOnboardingCompleted == true) Screen.Dashboard.route else Screen.Onboarding.route
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: UsageViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val usageData by viewModel.usageData.collectAsState()
    val config by viewModel.routerConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scrapingStatus by viewModel.scrapingStatus.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Airtel Usage Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Usage Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "This Cycle's Usage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = String.format("%.2f GB", usageData.toGigabytes()),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "of ${config.fupLimitGb} GB",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val daysRemaining by viewModel.daysRemaining.collectAsState()
                    Text(
                        text = "$daysRemaining days left in cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { (usageData.getPercentage(config.fupLimitGb) / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = String.format("%.1f%%", usageData.getPercentage(config.fupLimitGb)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Last Updated
            if (usageData.lastUpdated.isNotEmpty()) {
                Text(
                    text = "Last updated: ${usageData.lastUpdated}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Refresh Button
            Button(
                onClick = { viewModel.refreshUsageData() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Updating..." else "Refresh Now")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status Message
            if (isLoading) {
                Text(
                    text = scrapingStatus.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reports components
            val context = androidx.compose.ui.platform.LocalContext.current
            val reportsViewModel: ReportsViewModel = viewModel(
                factory = ReportsViewModelFactory(context)
            )
            
            val weeklyDigest by reportsViewModel.weeklyDigest.collectAsState()
            val fupProjection by reportsViewModel.fupProjection.collectAsState()

            // FUP Warning Card (only shown if will exceed)
            FupWarningCard(fupProjection = fupProjection)
            
            if (fupProjection?.willExceed == true) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Weekly Digest Card
            WeeklyDigestCard(weeklyDigest = weeklyDigest)
            
            Spacer(modifier = Modifier.height(16.dp))

            // View Reports Button
            OutlinedButton(
                onClick = onNavigateToReports,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Detailed Reports")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Debug Panel
            DebugPanel(viewModel)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Background updates every ${viewModel.syncIntervalHours.collectAsState(initial = 4).value} hours",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DebugPanel(viewModel: UsageViewModel) {
    val expanded by viewModel.isDebugExpanded.collectAsState()
    val debugInfo by viewModel.debugInfo.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Debug Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.toggleDebugExpanded() }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                if (debugInfo.lastFetchTime.isNotEmpty()) {
                    DebugRow("Last Fetch", debugInfo.lastFetchTime)
                    
                    if (debugInfo.lastError != null) {
                        DebugRow("❌ Error", debugInfo.lastError ?: "")
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scraped from Router:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        DebugRow("  TX (sent)", formatBytes(debugInfo.scrapedTx))
                        DebugRow("  RX (received)", formatBytes(debugInfo.scrapedRx))
                        DebugRow("  Uptime", formatUptime(debugInfo.scrapedUptime))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Previous Values:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        DebugRow("  TX", formatBytes(debugInfo.previousTx))
                        DebugRow("  RX", formatBytes(debugInfo.previousRx))
                        DebugRow("  Uptime", formatUptime(debugInfo.previousUptime))
                        
                        if (debugInfo.rebootDetected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ REBOOT DETECTED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            DebugRow("  No data added", "(cumulative preserved)")
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Delta (this update):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DebugRow("  TX delta", formatBytes(debugInfo.deltaTx))
                            DebugRow("  RX delta", formatBytes(debugInfo.deltaRx))
                            DebugRow("  Total delta", formatBytes(debugInfo.deltaTx + debugInfo.deltaRx))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lifetime Total (All Time):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        DebugRow("  Bytes", debugInfo.cumulativeBytes.toString())
                        DebugRow("  GB", String.format("%.4f GB", debugInfo.cumulativeBytes / (1024.0 * 1024.0 * 1024.0)))
                    }
                } else {
                    Text(
                        text = "No data fetched yet. Tap 'Refresh Now' to fetch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes bytes"
    }
}

fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "${days}d ${hours}h ${minutes}m ${secs}s"
}
