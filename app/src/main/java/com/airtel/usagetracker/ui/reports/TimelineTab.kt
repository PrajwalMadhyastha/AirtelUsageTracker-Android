package com.airtel.usagetracker.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airtel.usagetracker.ui.ReportsViewModel
import com.airtel.usagetracker.ui.components.FupWarningCard
import com.airtel.usagetracker.ui.components.WeeklyDigestCard

@Composable
fun TimelineTab(viewModel: ReportsViewModel) {
    val weeklyDigest by viewModel.weeklyDigest.collectAsState()
    val fupProjection by viewModel.fupProjection.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Usage Timeline",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // FUP Warning Card
        FupWarningCard(fupProjection = fupProjection)
        
        if (fupProjection?.willExceed == true) {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Weekly Digest Card
        WeeklyDigestCard(weeklyDigest = weeklyDigest)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Placeholder for chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📈",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Usage Timeline Chart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Interactive chart showing daily usage trends will be displayed here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
