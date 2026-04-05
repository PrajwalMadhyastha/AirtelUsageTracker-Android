package com.prajwal.utilities.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.ui.ReportsViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarTab(viewModel: ReportsViewModel) {
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val calendarData by viewModel.calendarData.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Usage Calendar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateCalendarMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                    }
                    
                    Text(
                        text = calendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { viewModel.navigateCalendarMonth(1) },
                        enabled = calendarMonth.isBefore(YearMonth.now().atDay(1))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Day headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Calendar grid
                val yearMonth = YearMonth.from(calendarMonth)
                val firstDayOfMonth = yearMonth.atDay(1)
                val daysInMonth = yearMonth.lengthOfMonth()
                val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
                
                val calendarDays = mutableListOf<LocalDate?>()
                // Add empty cells for days before the first of the month
                repeat(firstDayOfWeek) { calendarDays.add(null) }
                // Add actual days
                for (day in 1..daysInMonth) {
                    calendarDays.add(yearMonth.atDay(day))
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(calendarDays) { date ->
                        if (date != null) {
                            val usage = calendarData.find { it.date == date }
                            DayCell(date, usage?.toGigabytes()?.toFloat())
                        } else {
                            Box(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("0-5 GB", Color(0xFF4CAF50))
                    LegendItem("5-10 GB", Color(0xFF2196F3))
                    LegendItem("10-15 GB", Color(0xFFFFC107))
                    LegendItem("15+ GB", Color(0xFFF44336))
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, usageGb: Float?) {
    val backgroundColor = when {
        usageGb == null -> Color.Transparent
        usageGb < 5f -> Color(0xFF4CAF50).copy(alpha = 0.3f)
        usageGb < 10f -> Color(0xFF2196F3).copy(alpha = 0.3f)
        usageGb < 15f -> Color(0xFFFFC107).copy(alpha = 0.3f)
        else -> Color(0xFFF44336).copy(alpha = 0.3f)
    }
    
    val isToday = date == LocalDate.now()
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .then(
                if (isToday) Modifier.background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (usageGb != null && usageGb > 0) {
                Text(
                    text = String.format("%.1f", usageGb),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color.copy(alpha = 0.3f), MaterialTheme.shapes.small)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
