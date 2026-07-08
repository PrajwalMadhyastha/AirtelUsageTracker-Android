package com.prajwal.utilities.tools.wealthtracker.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.tools.wealthtracker.data.MilestoneCalculator
import com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity
import com.prajwal.utilities.tools.wealthtracker.data.toAssetClasses
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

// Color palette for the 5 asset classes
private val assetColors = listOf(
    Color(0xFF6366F1), // Equity — indigo
    Color(0xFFF59E0B), // Gold — amber
    Color(0xFF10B981), // Debt — emerald
    Color(0xFF94A3B8), // Silver — slate
    Color(0xFFEC4899)  // REITs — pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTab(
    snapshots: List<AssetSnapshotEntity>,          // newest-first (for latest diversification)
    snapshotsChronological: List<AssetSnapshotEntity>  // oldest-first (for growth chart)
) {
    if (snapshots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text("No data yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Add your first snapshot in the Portfolio tab to see reports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val latest = snapshots.first()
    val allAssetClasses = latest.toAssetClasses()

    // Toggle: show Invested or Current value breakdown in the donut
    var showInvested by remember { mutableStateOf(false) }
    val assetClasses = allAssetClasses.filter { if (showInvested) it.invested > 0 else it.current > 0 }
    val totalForPct = if (showInvested) latest.totalInvested else latest.totalCurrent

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Section A: Diversification ───────────────────────────────────────
        item {
            Text(
                "Diversification",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // ── Invested / Current toggle ───────────────────────────────
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !showInvested,
                            onClick = { showInvested = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) { Text("Current Value") }
                        SegmentedButton(
                            selected = showInvested,
                            onClick = { showInvested = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) { Text("Invested") }
                    }

                    // ── Donut chart ──────────────────────────────────────────
                    if (assetClasses.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DonutChart(
                                values = assetClasses.map {
                                    if (showInvested) it.invested.toFloat() else it.current.toFloat()
                                },
                                colors = assetColors.take(assetClasses.size),
                                modifier = Modifier.size(160.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                assetClasses.forEachIndexed { i, asset ->
                                    val pct = if (totalForPct > 0)
                                        ((if (showInvested) asset.invested else asset.current) / totalForPct * 100) else 0.0
                                    val value = if (showInvested) asset.invested else asset.current
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(assetColors[i])
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                "${asset.name} — ${String.format("%.1f", pct)}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                MilestoneCalculator.formatInr(value),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Per-asset gain/loss table
                    assetClasses.forEachIndexed { i, asset ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(assetColors[i])
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(asset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${if (asset.gainLoss >= 0) "+" else ""}${MilestoneCalculator.formatInr(asset.gainLoss)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (asset.gainLoss >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "${String.format("%.1f", asset.gainLossPct)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Section B: Portfolio Growth Chart ─────────────────────────
        item {
            Text(
                "Portfolio Growth",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Invested (dashed) vs Current Value (solid) — like run-rate vs required rate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (snapshotsChronological.size < 2) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Add at least 2 snapshots to see the growth chart.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val investedValues = snapshotsChronological.map { it.totalInvested.toFloat() }
                        val currentValues = snapshotsChronological.map { it.totalCurrent.toFloat() }
                        val dates = snapshotsChronological.map { it.recordedAt }

                        GrowthLineChart(
                            investedSeries = investedValues,
                            currentSeries = currentValues,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Start / end labels
                        val dateFmt = SimpleDateFormat("MMM yy", Locale.getDefault())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dateFmt.format(Date(dates.first())), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFmt.format(Date(dates.last())), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendItem(color = Color(0xFF6366F1), label = "Invested", dashed = true)
                            LegendItem(color = Color(0xFF10B981), label = "Current Value", dashed = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = values.sum().takeIf { it > 0f } ?: 1f
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.18f
        val radius = (size.minDimension / 2f) - strokeWidth / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        var startAngle = -90f

        values.forEachIndexed { i, value ->
            val sweep = (value / total) * 360f
            drawArc(
                color = colors.getOrElse(i) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = sweep - 2f, // small gap between segments
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun GrowthLineChart(
    investedSeries: List<Float>,
    currentSeries: List<Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF6366F1)
    val currentColor = Color(0xFF10B981)

    Canvas(modifier = modifier) {
        if (investedSeries.size < 2) return@Canvas

        val allValues = investedSeries + currentSeries
        val minVal = allValues.minOrNull() ?: 0f
        val maxVal = allValues.maxOrNull()?.takeIf { it > minVal } ?: (minVal + 1f)
        val n = investedSeries.size

        fun xOf(i: Int) = i.toFloat() / (n - 1) * size.width
        fun yOf(v: Float) = size.height - ((v - minVal) / (maxVal - minVal)) * size.height * 0.9f - size.height * 0.05f

        // Draw invested as dashed line
        val dashLength = 12f
        val gapLength = 6f
        val invPath = Path()
        investedSeries.forEachIndexed { i, v ->
            if (i == 0) invPath.moveTo(xOf(i), yOf(v)) else invPath.lineTo(xOf(i), yOf(v))
        }
        // Draw dashed by segmenting
        for (i in 0 until n - 1) {
            val x1 = xOf(i); val y1 = yOf(investedSeries[i])
            val x2 = xOf(i + 1); val y2 = yOf(investedSeries[i + 1])
            val dx = x2 - x1; val dy = y2 - y1
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            var traveled = 0f
            var drawing = true
            while (traveled < dist) {
                val segEnd = min(traveled + if (drawing) dashLength else gapLength, dist)
                val t1 = traveled / dist; val t2 = segEnd / dist
                if (drawing) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.7f),
                        start = Offset(x1 + dx * t1, y1 + dy * t1),
                        end = Offset(x1 + dx * t2, y1 + dy * t2),
                        strokeWidth = 3f, cap = StrokeCap.Round
                    )
                }
                traveled = segEnd
                drawing = !drawing
            }
        }

        // Draw current value as solid line
        val curPath = Path()
        currentSeries.forEachIndexed { i, v ->
            if (i == 0) curPath.moveTo(xOf(i), yOf(v)) else curPath.lineTo(xOf(i), yOf(v))
        }
        drawPath(curPath, color = currentColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Data point dots on current line
        currentSeries.forEachIndexed { i, v ->
            drawCircle(color = currentColor, radius = 5f, center = Offset(xOf(i), yOf(v)))
            drawCircle(color = Color.White, radius = 3f, center = Offset(xOf(i), yOf(v)))
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(24.dp, 3.dp)) {
            if (dashed) {
                drawLine(color, Offset(0f, size.height / 2), Offset(size.width / 2, size.height / 2), strokeWidth = 3f)
            } else {
                drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3f)
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
