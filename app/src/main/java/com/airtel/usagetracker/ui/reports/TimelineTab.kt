package com.airtel.usagetracker.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airtel.usagetracker.data.models.TimePeriod
import com.airtel.usagetracker.ui.ReportsViewModel
import com.airtel.usagetracker.ui.components.FupWarningCard
import com.airtel.usagetracker.ui.components.WeeklyDigestCard
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTab(viewModel: ReportsViewModel) {
    val weeklyDigest by viewModel.weeklyDigest.collectAsState()
    val fupProjection by viewModel.fupProjection.collectAsState()
    val timelineData by viewModel.timelineData.collectAsState()
    val selectedPeriod by viewModel.selectedTimePeriod.collectAsState()
    val selectedCycle by viewModel.selectedCycle.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Usage Timeline",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        
        // Time Period Selector (2 rows) - Only show if no cycle is selected globally
        if (selectedCycle == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row: Today, Week, Month
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TimePeriod.TODAY, TimePeriod.WEEK, TimePeriod.MONTH).forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { viewModel.selectTimePeriod(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
                
                // Second row: Cycle, All Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TimePeriod.CYCLE, TimePeriod.ALL_TIME).forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { viewModel.selectTimePeriod(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Usage Data List
        if (timelineData.isNotEmpty()) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(timelineData.reversed()) { daily ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = daily.date.format(DateTimeFormatter.ofPattern("MMM dd")),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format("%.2f GB", daily.toGigabytes()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available for selected period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // FUP Warning Card
        FupWarningCard(fupProjection = fupProjection)
        
        if (fupProjection?.willExceed == true) {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Weekly Digest Card
        WeeklyDigestCard(weeklyDigest = weeklyDigest)
    }
}
