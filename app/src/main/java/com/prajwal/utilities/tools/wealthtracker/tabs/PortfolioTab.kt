package com.prajwal.utilities.tools.wealthtracker.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.tools.wealthtracker.data.MilestoneCalculator
import com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PortfolioTab(
    snapshots: List<AssetSnapshotEntity>,
    prefill: AssetSnapshotEntity?,
    onSave: (AssetSnapshotEntity) -> Unit,
    onDelete: (AssetSnapshotEntity) -> Unit
) {
    var equityInvested by remember { mutableStateOf("") }
    var equityCurrent by remember { mutableStateOf("") }
    var goldInvested by remember { mutableStateOf("") }
    var goldCurrent by remember { mutableStateOf("") }
    var debtInvested by remember { mutableStateOf("") }
    var debtCurrent by remember { mutableStateOf("") }
    var silverInvested by remember { mutableStateOf("") }
    var silverCurrent by remember { mutableStateOf("") }
    var reitsInvested by remember { mutableStateOf("") }
    var reitsCurrent by remember { mutableStateOf("") }
    var snapshotToDelete by remember { mutableStateOf<AssetSnapshotEntity?>(null) }
    var justSaved by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Pre-fill from latest snapshot when it arrives
    LaunchedEffect(prefill) {
        prefill?.let { s ->
            fun fmt(v: Double) = if (v > 0) v.toLong().toString() else ""
            equityInvested = fmt(s.equityInvested)
            equityCurrent = fmt(s.equityCurrent)
            goldInvested = fmt(s.goldInvested)
            goldCurrent = fmt(s.goldCurrent)
            debtInvested = fmt(s.debtInvested)
            debtCurrent = fmt(s.debtCurrent)
            silverInvested = fmt(s.silverInvested)
            silverCurrent = fmt(s.silverCurrent)
            reitsInvested = fmt(s.reitsInvested)
            reitsCurrent = fmt(s.reitsCurrent)
        }
    }

    // Disable Save if form values match the pre-filled (latest) snapshot
    val hasChanged = remember(
        equityInvested, equityCurrent, goldInvested, goldCurrent,
        debtInvested, debtCurrent, silverInvested, silverCurrent,
        reitsInvested, reitsCurrent, prefill
    ) {
        fun toD(s: String) = s.toDoubleOrNull() ?: 0.0
        val p = prefill
        when {
            p == null -> listOf(
                equityInvested, equityCurrent, goldInvested, goldCurrent,
                debtInvested, debtCurrent, silverInvested, silverCurrent,
                reitsInvested, reitsCurrent
            ).any { toD(it) > 0 }
            else ->
                toD(equityInvested) != p.equityInvested ||
                toD(equityCurrent) != p.equityCurrent ||
                toD(goldInvested) != p.goldInvested ||
                toD(goldCurrent) != p.goldCurrent ||
                toD(debtInvested) != p.debtInvested ||
                toD(debtCurrent) != p.debtCurrent ||
                toD(silverInvested) != p.silverInvested ||
                toD(silverCurrent) != p.silverCurrent ||
                toD(reitsInvested) != p.reitsInvested ||
                toD(reitsCurrent) != p.reitsCurrent
        }
    }

    // Live totals (computed outside LazyColumn so they react immediately)
    val totalInv = listOf(equityInvested, goldInvested, debtInvested, silverInvested, reitsInvested)
        .sumOf { it.toDoubleOrNull() ?: 0.0 }
    val totalCur = listOf(equityCurrent, goldCurrent, debtCurrent, silverCurrent, reitsCurrent)
        .sumOf { it.toDoubleOrNull() ?: 0.0 }
    val gain = totalCur - totalInv
    val gainPositive = gain >= 0

    // Delete confirmation dialog
    snapshotToDelete?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { snapshotToDelete = null },
            title = { Text("Delete Snapshot?") },
            text = {
                val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                Text("The snapshot from ${fmt.format(Date(snapshot.recordedAt))} will be permanently removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(snapshot)
                    snapshotToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToDelete = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Live summary card — always visible at the top ─────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (gainPositive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total Invested",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            MilestoneCalculator.formatInr(totalInv),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "P / L",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${if (gainPositive) "+" else "-"}${MilestoneCalculator.formatInr(abs(gain))}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (gainPositive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        if (totalInv > 0) {
                            val pct = (gain / totalInv * 100)
                            Text(
                                "${if (gainPositive) "+" else ""}${String.format("%.1f", pct)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (gainPositive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Current Value",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            MilestoneCalculator.formatInr(totalCur),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Header banner ─────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "Update Portfolio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Enter invested & current values. Each save creates a timestamped snapshot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ── Asset input cards ─────────────────────────────────────────
        item { AssetInputCard("Equity", "Mutual Funds + Stocks", equityInvested, equityCurrent, { equityInvested = it }, { equityCurrent = it }) }
        item { AssetInputCard("Gold", "Physical + Sovereign Bonds + ETFs", goldInvested, goldCurrent, { goldInvested = it }, { goldCurrent = it }) }
        item { AssetInputCard("Debt", "FD + Bonds + Liquid Funds", debtInvested, debtCurrent, { debtInvested = it }, { debtCurrent = it }) }
        item { AssetInputCard("Silver", "Physical + ETFs", silverInvested, silverCurrent, { silverInvested = it }, { silverCurrent = it }) }
        item { AssetInputCard("REITs", "Real Estate Investment Trusts", reitsInvested, reitsCurrent, { reitsInvested = it }, { reitsCurrent = it }) }

        // ── Save button ───────────────────────────────────────────────
        item {
            Button(
                onClick = {
                    onSave(
                        AssetSnapshotEntity(
                            equityInvested = equityInvested.toDoubleOrNull() ?: 0.0,
                            equityCurrent = equityCurrent.toDoubleOrNull() ?: 0.0,
                            goldInvested = goldInvested.toDoubleOrNull() ?: 0.0,
                            goldCurrent = goldCurrent.toDoubleOrNull() ?: 0.0,
                            debtInvested = debtInvested.toDoubleOrNull() ?: 0.0,
                            debtCurrent = debtCurrent.toDoubleOrNull() ?: 0.0,
                            silverInvested = silverInvested.toDoubleOrNull() ?: 0.0,
                            silverCurrent = silverCurrent.toDoubleOrNull() ?: 0.0,
                            reitsInvested = reitsInvested.toDoubleOrNull() ?: 0.0,
                            reitsCurrent = reitsCurrent.toDoubleOrNull() ?: 0.0
                        )
                    )
                    justSaved = true
                    coroutineScope.launch {
                        delay(2500)
                        justSaved = false
                    }
                },
                enabled = hasChanged && !justSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                AnimatedContent(
                    targetState = justSaved,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "save_button_content"
                ) { saved ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (saved) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (saved) "Snapshot Saved!" else "Save Snapshot",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // ── Snapshot history ──────────────────────────────────────────
        if (snapshots.isNotEmpty()) {
            item {
                Text(
                    "Snapshot History (${snapshots.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(snapshots, key = { it.id }) { snapshot ->
                SnapshotHistoryCard(
                    snapshot = snapshot,
                    onDelete = { snapshotToDelete = snapshot }
                )
            }
        }
    }
}

@Composable
private fun AssetInputCard(
    label: String,
    subtitle: String,
    invested: String,
    current: String,
    onInvestedChange: (String) -> Unit,
    onCurrentChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = invested,
                    onValueChange = onInvestedChange,
                    label = { Text("Invested (₹)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("₹") }
                )
                OutlinedTextField(
                    value = current,
                    onValueChange = onCurrentChange,
                    label = { Text("Current (₹)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("₹") }
                )
            }
        }
    }
}

@Composable
private fun SnapshotHistoryCard(
    snapshot: AssetSnapshotEntity,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val gain = snapshot.totalGainLoss
    val gainPositive = gain >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        fmt.format(Date(snapshot.recordedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        MilestoneCalculator.formatInr(snapshot.totalCurrent),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${if (gainPositive) "+" else "-"}${MilestoneCalculator.formatInr(abs(gain))} (${String.format("%.1f", snapshot.totalGainLossPct)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (gainPositive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                val assets = listOf(
                    "Equity" to Pair(snapshot.equityInvested, snapshot.equityCurrent),
                    "Gold" to Pair(snapshot.goldInvested, snapshot.goldCurrent),
                    "Debt" to Pair(snapshot.debtInvested, snapshot.debtCurrent),
                    "Silver" to Pair(snapshot.silverInvested, snapshot.silverCurrent),
                    "REITs" to Pair(snapshot.reitsInvested, snapshot.reitsCurrent)
                )
                assets.forEach { (name, values) ->
                    val (inv, cur) = values
                    if (inv > 0 || cur > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(
                                "${MilestoneCalculator.formatInr(inv)} → ${MilestoneCalculator.formatInr(cur)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
