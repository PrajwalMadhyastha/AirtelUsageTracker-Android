package com.airtel.usagetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airtel.usagetracker.data.models.WeeklyDigest
import java.time.format.DateTimeFormatter

@Composable
fun WeeklyDigestCard(
    weeklyDigest: WeeklyDigest?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = weeklyDigest != null) {
        weeklyDigest?.let { digest ->
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "This Week at a Glance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Total usage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Used:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = String.format("%.1f GB", digest.totalUsageGb),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Daily average
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Avg:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = String.format("%.1f GB", digest.dailyAverageGb),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Peak day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Peak Day:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${digest.peakDay.format(DateTimeFormatter.ofPattern("MMM dd"))} (${String.format("%.1f GB", digest.peakUsageGb)})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Mini bar chart
                    if (digest.dailyUsages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MiniBarChart(
                            dailyUsages = digest.dailyUsages,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        )
                    }

                    // Comparison to previous week
                    digest.comparisonToPreviousWeek?.let { comparison ->
                        Spacer(modifier = Modifier.height(8.dp))
                        val comparisonText = if (comparison > 0) {
                            "vs Last Week: +${String.format("%.1f", comparison)}%"
                        } else {
                            "vs Last Week: ${String.format("%.1f", comparison)}%"
                        }
                        val comparisonColor = if (comparison > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }

                        Text(
                            text = comparisonText,
                            style = MaterialTheme.typography.bodySmall,
                            color = comparisonColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniBarChart(
    dailyUsages: List<com.airtel.usagetracker.data.models.DailyUsage>,
    modifier: Modifier = Modifier
) {
    val maxUsage = dailyUsages.maxOfOrNull { it.toGigabytes() } ?: 1.0

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        dailyUsages.take(7).forEach { usage ->
            val heightFraction = if (maxUsage > 0) {
                (usage.toGigabytes() / maxUsage).toFloat()
            } else 0f

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction.coerceIn(0.1f, 1f)),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                color = MaterialTheme.colorScheme.primary
            ) {}
        }
    }
}
