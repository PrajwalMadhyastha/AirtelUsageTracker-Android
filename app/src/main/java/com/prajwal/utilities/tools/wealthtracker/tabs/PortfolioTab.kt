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
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.tools.wealthtracker.data.MilestoneCalculator
import com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity
import com.prajwal.utilities.tools.wealthtracker.data.db.HoldingEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioTab(
    snapshots: List<AssetSnapshotEntity>,
    holdings: List<HoldingEntity>,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onSave: (AssetSnapshotEntity) -> Unit,
    onDelete: (AssetSnapshotEntity) -> Unit
) {
    var snapshotToDelete by remember { mutableStateOf<AssetSnapshotEntity?>(null) }
    var justSaved by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Calculate live totals from holdings
    val eqHoldings = holdings.filter { it.assetClass == "Equity" }
    val gdHoldings = holdings.filter { it.assetClass == "Gold" }
    val dbHoldings = holdings.filter { it.assetClass == "Debt" }
    val slHoldings = holdings.filter { it.assetClass == "Silver" }
    val rtHoldings = holdings.filter { it.assetClass == "REITs" }

    fun calcInv(list: List<HoldingEntity>) = list.sumOf { it.investedAmount }
    fun calcCur(list: List<HoldingEntity>) = list.sumOf { 
        if (it.latestPrice > 0) it.unitsHeld * it.latestPrice else it.investedAmount 
    }

    val eqInv = calcInv(eqHoldings); val eqCur = calcCur(eqHoldings)
    val gdInv = calcInv(gdHoldings); val gdCur = calcCur(gdHoldings)
    val dbInv = calcInv(dbHoldings); val dbCur = calcCur(dbHoldings)
    val slInv = calcInv(slHoldings); val slCur = calcCur(slHoldings)
    val rtInv = calcInv(rtHoldings); val rtCur = calcCur(rtHoldings)

    val totalInv = eqInv + gdInv + dbInv + slInv + rtInv
    val totalCur = eqCur + gdCur + dbCur + slCur + rtCur
    val gain = totalCur - totalInv
    val gainPositive = gain >= 0

    val dailyGain = holdings.sumOf { if (it.previousClosePrice > 0) it.unitsHeld * (it.latestPrice - it.previousClosePrice) else 0.0 }
    val dailyGainPositive = dailyGain >= 0

    // Only enable save if we have some investment and haven't just saved
    val canSave = totalInv > 0 && !justSaved

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

    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onSyncNow()
        }
    }

    LaunchedEffect(isSyncing) {
        if (isSyncing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Live summary card ─────────────
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
                            MilestoneCalculator.formatInrExact(totalInv),
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
                            "${if (gainPositive) "+" else "-"}${MilestoneCalculator.formatInrExact(abs(gain))}",
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
                            "Live Value",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            MilestoneCalculator.formatInrExact(totalCur),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Daily P/L card ─────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (dailyGainPositive)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today's P/L",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${if (dailyGainPositive) "+" else "-"}${MilestoneCalculator.formatInrExact(abs(dailyGain), showDecimals = true)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (dailyGainPositive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
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
                        "Portfolio Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Your portfolio is automatically calculated from your Holdings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ── Asset summary cards ───────────────────────────────────────
        if (eqInv > 0 || eqCur > 0) item { AssetSummaryCard("Equity", eqInv, eqCur) }
        if (gdInv > 0 || gdCur > 0) item { AssetSummaryCard("Gold", gdInv, gdCur) }
        if (dbInv > 0 || dbCur > 0) item { AssetSummaryCard("Debt", dbInv, dbCur) }
        if (slInv > 0 || slCur > 0) item { AssetSummaryCard("Silver", slInv, slCur) }
        if (rtInv > 0 || rtCur > 0) item { AssetSummaryCard("REITs", rtInv, rtCur) }

        // ── Save button ───────────────────────────────────────────────
        item {
            Button(
                onClick = {
                    onSave(
                        AssetSnapshotEntity(
                            equityInvested = eqInv, equityCurrent = eqCur,
                            goldInvested = gdInv, goldCurrent = gdCur,
                            debtInvested = dbInv, debtCurrent = dbCur,
                            silverInvested = slInv, silverCurrent = slCur,
                            reitsInvested = rtInv, reitsCurrent = rtCur
                        )
                    )
                    justSaved = true
                    coroutineScope.launch {
                        delay(2500)
                        justSaved = false
                    }
                },
                enabled = canSave,
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
                            text = if (saved) "Snapshot Saved!" else "Save Live Snapshot",
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

    if (pullToRefreshState.progress > 0f || pullToRefreshState.isRefreshing) {
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
}

@Composable
private fun AssetSummaryCard(
    label: String,
    invested: Double,
    current: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MilestoneCalculator.formatInrExact(invested), style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Live Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MilestoneCalculator.formatInrExact(current), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
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
                        MilestoneCalculator.formatInrExact(snapshot.totalCurrent),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${if (gainPositive) "+" else "-"}${MilestoneCalculator.formatInrExact(abs(gain))} (${String.format("%.1f", snapshot.totalGainLossPct)}%)",
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
                                "${MilestoneCalculator.formatInrExact(inv)} → ${MilestoneCalculator.formatInrExact(cur)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
