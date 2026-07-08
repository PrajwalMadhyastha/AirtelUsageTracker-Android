package com.prajwal.utilities.tools.wealthtracker.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.tools.wealthtracker.data.CalculatorSettings
import com.prajwal.utilities.tools.wealthtracker.data.MilestoneCalculator
import com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity
import kotlin.math.roundToInt

private val milestoneTargets = listOf(
    Triple("1 Cr", 1_00_00_000.0, Color(0xFF6366F1)),
    Triple("5 Cr", 5_00_00_000.0, Color(0xFFF59E0B)),
    Triple("10 Cr", 10_00_00_000.0, Color(0xFFEC4899))
)

@Composable
fun MilestonesTab(
    latestSnapshot: AssetSnapshotEntity?,
    settings: CalculatorSettings,
    onMonthlyInvestmentChange: (Double) -> Unit,
    onStepupChange: (Double) -> Unit,
    onReturnChange: (Double) -> Unit
) {
    val currentPortfolioValue = latestSnapshot?.totalCurrent ?: 0.0

    // Local text state for the three input fields
    var monthlyText by remember(settings.monthlyInvestment) {
        mutableStateOf(settings.monthlyInvestment.toLong().toString())
    }
    var stepupText by remember(settings.annualStepupPercent) {
        mutableStateOf(settings.annualStepupPercent.toString())
    }
    var returnText by remember(settings.expectedReturnPercent) {
        mutableStateOf(settings.expectedReturnPercent.toString())
    }

    // Recalculate milestones whenever settings change
    val results = remember(currentPortfolioValue, settings) {
        MilestoneCalculator.calculate(
            currentPortfolioValue = currentPortfolioValue,
            monthlySip = settings.monthlyInvestment,
            annualStepupPct = settings.annualStepupPercent,
            annualReturnPct = settings.expectedReturnPercent
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Current portfolio value ────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Portfolio", style = MaterialTheme.typography.labelMedium)
                        Text(
                            MilestoneCalculator.formatInr(currentPortfolioValue),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (latestSnapshot == null) {
                        Text(
                            "No snapshot yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Calculator settings ───────────────────────────────────────
        item {
            Text(
                "Calculator Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingField(
                        label = "Monthly SIP",
                        value = monthlyText,
                        suffix = "₹/month",
                        onValueChange = { monthlyText = it },
                        onDone = { monthlyText.toDoubleOrNull()?.let(onMonthlyInvestmentChange) }
                    )
                    SettingField(
                        label = "Annual Step-Up",
                        value = stepupText,
                        suffix = "% per year",
                        onValueChange = { stepupText = it },
                        onDone = { stepupText.toDoubleOrNull()?.let(onStepupChange) }
                    )
                    SettingField(
                        label = "Expected Annual Return",
                        value = returnText,
                        suffix = "% p.a.",
                        onValueChange = { returnText = it },
                        onDone = { returnText.toDoubleOrNull()?.let(onReturnChange) }
                    )
                }
            }
        }

        // ── Milestone cards ───────────────────────────────────────────
        item {
            Text(
                "Milestones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                milestoneTargets.forEachIndexed { i, (label, target, color) ->
                    val result = results.getOrNull(i)
                    MilestoneCard(
                        modifier = Modifier.weight(1f),
                        label = label,
                        color = color,
                        currentValue = currentPortfolioValue,
                        targetValue = target,
                        result = result
                    )
                }
            }
        }

        // ── Projection chart ─────────────────────────────────────────
        item {
            Text(
                "Projection Chart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "Projected portfolio value over time with milestone markers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val projectionPoints = results.firstOrNull()?.projectionPoints ?: emptyList()
                    if (projectionPoints.size >= 2) {
                        ProjectionChart(
                            points = projectionPoints.map { it.portfolioValue.toFloat() },
                            milestones = milestoneTargets.map { (label, value, color) ->
                                Triple(label, value.toFloat(), color)
                            },
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${projectionPoints.last().year} yrs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("Configure settings above to see projection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    suffix: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(suffix, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                it.toDoubleOrNull()?.let { v -> onDone() }
            },
            modifier = Modifier.width(130.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MilestoneCard(
    modifier: Modifier = Modifier,
    label: String,
    color: Color,
    currentValue: Double,
    targetValue: Double,
    result: MilestoneCalculator.MilestoneResult?
) {
    val alreadyReached = currentValue >= targetValue

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "₹$label",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (alreadyReached) {
                Text("✅ Done!", style = MaterialTheme.typography.labelMedium, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            } else if (result != null && result.monthsToReach > 0) {
                val years = result.monthsToReach / 12
                val months = result.monthsToReach % 12
                Text(
                    buildString {
                        if (years > 0) append("${years}y ")
                        if (months > 0) append("${months}m")
                    }.trim(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "to reach",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(">30 yrs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("to reach", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProjectionChart(
    points: List<Float>,
    milestones: List<Triple<String, Float, Color>>,
    modifier: Modifier = Modifier
) {
    val lineColor = Color(0xFF6366F1)

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val paddingTop = 24f
        val paddingBottom = 24f
        val paddingStart = 0f

        val chartHeight = size.height - paddingTop - paddingBottom
        val chartWidth = size.width - paddingStart

        val maxVal = (points.maxOrNull() ?: 1f).coerceAtLeast(milestones.maxOfOrNull { it.second } ?: 1f)
        val minVal = 0f

        fun xOf(i: Int) = paddingStart + i.toFloat() / (points.size - 1) * chartWidth
        fun yOf(v: Float) = paddingTop + chartHeight - ((v - minVal) / (maxVal - minVal)) * chartHeight

        // Draw milestone horizontal dashed lines
        milestones.forEach { (label, value, color) ->
            if (value <= maxVal * 1.1f) {
                val y = yOf(value)
                drawLine(
                    color = color.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            }
        }

        // Draw projection curve
        val path = Path()
        points.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(xOf(i), yOf(v)) else path.lineTo(xOf(i), yOf(v))
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Fill under curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(xOf(points.size - 1), size.height)
            lineTo(xOf(0), size.height)
            close()
        }
        drawPath(fillPath, color = lineColor.copy(alpha = 0.08f))
    }
}
