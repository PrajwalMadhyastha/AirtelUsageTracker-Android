package com.prajwal.utilities.tools.wealthtracker.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.prajwal.utilities.tools.wealthtracker.data.MilestoneCalculator
import com.prajwal.utilities.tools.wealthtracker.data.db.HoldingEntity
import com.prajwal.utilities.tools.wealthtracker.data.SortOption
import com.prajwal.utilities.tools.wealthtracker.data.network.AssetSearchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HoldingsTab(
    holdings: List<HoldingEntity>,
    isSyncing: Boolean,
    searchResults: List<AssetSearchResult>,
    sortOption: SortOption,
    sortAscending: Boolean,
    onSortOptionChanged: (SortOption) -> Unit,
    onSearchQueryChanged: (String, Boolean) -> Unit,
    onAddHolding: (HoldingEntity) -> Unit,
    onUpdateHolding: (HoldingEntity) -> Unit,
    onDeleteHolding: (HoldingEntity) -> Unit,
    onSyncNow: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var holdingToEdit by remember { mutableStateOf<HoldingEntity?>(null) }
    var holdingToTopUp by remember { mutableStateOf<HoldingEntity?>(null) }
    // FIX #18: Guard deletion behind a confirmation dialog (mirrors snapshot deletion in PortfolioTab)
    var holdingToDelete by remember { mutableStateOf<HoldingEntity?>(null) }

    holdingToDelete?.let { h ->
        AlertDialog(
            onDismissRequest = { holdingToDelete = null },
            title = { Text("Delete Holding?") },
            text = {
                Text(
                    "\"${h.name.ifBlank { h.identifier }}\" and all its data will be permanently removed. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteHolding(h)
                    holdingToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { holdingToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (holdingToTopUp != null) {
        TopUpDialog(
            holding = holdingToTopUp!!,
            onDismiss = { holdingToTopUp = null },
            onSave = { updatedHolding ->
                onUpdateHolding(updatedHolding)
                holdingToTopUp = null
            }
        )
    }

    if (showAddDialog || holdingToEdit != null) {
        AddHoldingDialog(
            holdingToEdit = holdingToEdit,
            searchResults = searchResults,
            onSearchQueryChanged = onSearchQueryChanged,
            onDismiss = { showAddDialog = false; holdingToEdit = null },
            onSave = {
                if (holdingToEdit != null) onUpdateHolding(it) else onAddHolding(it)
                showAddDialog = false
                holdingToEdit = null
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Sync Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Holdings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            val sortOptionsList = SortOption.entries.filter { it != SortOption.DEFAULT }
                            sortOptionsList.forEach { option ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                option.label, 
                                                fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == sortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (option == sortOption) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = "Sort Direction",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSortOptionChanged(option)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Now")
                            Spacer(Modifier.width(8.dp))
                            Text("Sync Live")
                        }
                    }
                }
            }

            // Category Filter Row
            val filters = listOf("All", "Stocks", "Mutual Funds", "Gold", "Debt", "Silver", "REITs")
            var selectedFilter by remember { mutableStateOf("All") }
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            val filteredHoldings = holdings.filter {
                when (selectedFilter) {
                    "All" -> true
                    "Stocks" -> it.instrumentType == "Stock"
                    "Mutual Funds" -> it.instrumentType == "MF"
                    else -> it.assetClass == selectedFilter
                }
            }

            if (filteredHoldings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (holdings.isEmpty()) "No holdings added yet. Tap + to add." else "No holdings match this filter.", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    val grouped = filteredHoldings.groupBy { it.assetClass }
                    grouped.forEach { (assetClass, classHoldings) ->
                        item {
                            val hasDayPL = classHoldings.any { it.previousClosePrice > 0 }
                            val categoryDayGain = if (hasDayPL) {
                                classHoldings.sumOf { holding ->
                                    if (holding.previousClosePrice > 0) holding.unitsHeld * (holding.latestPrice - holding.previousClosePrice) else 0.0
                                }
                            } else 0.0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = assetClass,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (hasDayPL) {
                                    val sign = if (categoryDayGain >= 0) "+" else "-"
                                    val color = if (categoryDayGain >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Day's P/L: ",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$sign${MilestoneCalculator.formatInrExact(kotlin.math.abs(categoryDayGain))}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = color,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        val sortedClassHoldings = when (sortOption) {
                            SortOption.DEFAULT -> classHoldings
                            SortOption.ALPHABETICAL -> if (sortAscending) classHoldings.sortedBy { it.name.lowercase() } else classHoldings.sortedByDescending { it.name.lowercase() }
                            SortOption.DAILY_PCT -> classHoldings.let { list ->
                                val sel: (HoldingEntity) -> Double = { h -> if (h.previousClosePrice > 0) (h.latestPrice - h.previousClosePrice) / h.previousClosePrice else 0.0 }
                                if (sortAscending) list.sortedBy(sel) else list.sortedByDescending(sel)
                            }
                            SortOption.LTP -> if (sortAscending) classHoldings.sortedBy { it.latestPrice } else classHoldings.sortedByDescending { it.latestPrice }
                            SortOption.PL_ABSOLUTE -> classHoldings.let { list ->
                                val sel: (HoldingEntity) -> Double = { h -> (h.unitsHeld * h.latestPrice) - h.investedAmount }
                                if (sortAscending) list.sortedBy(sel) else list.sortedByDescending(sel)
                            }
                            SortOption.PL_PERCENT -> classHoldings.let { list ->
                                val sel: (HoldingEntity) -> Double = { h -> if (h.investedAmount > 0) ((h.unitsHeld * h.latestPrice) - h.investedAmount) / h.investedAmount else 0.0 }
                                if (sortAscending) list.sortedBy(sel) else list.sortedByDescending(sel)
                            }
                            SortOption.INVESTED -> if (sortAscending) classHoldings.sortedBy { it.investedAmount } else classHoldings.sortedByDescending { it.investedAmount }
                        }

                        items(sortedClassHoldings, key = { it.id }) { holding ->
                            HoldingCard(
                                holding = holding,
                                onTopUp = { holdingToTopUp = holding },
                                onEdit = { holdingToEdit = holding },
                                onDelete = { holdingToDelete = holding }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Holding")
        }
    }
}

@Composable
fun HoldingCard(holding: HoldingEntity, onTopUp: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val currentVal = holding.unitsHeld * holding.latestPrice
    val gain = currentVal - holding.investedAmount
    val gainPct = if (holding.investedAmount > 0) (gain / holding.investedAmount) * 100 else 0.0
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        holding.name.ifBlank { "${holding.identifier}${holding.exchange?.let { ".$it" } ?: ""}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${holding.instrumentType} • ${holding.unitsHeld} units",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onTopUp) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Top Up", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Invested", style = MaterialTheme.typography.labelSmall)
                    Text(MilestoneCalculator.formatInrExact(holding.investedAmount), fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LTP (Live)", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (holding.latestPrice > 0) MilestoneCalculator.formatInrExact(holding.latestPrice, showDecimals = true) else "-",
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current Value", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (holding.latestPrice > 0) MilestoneCalculator.formatInrExact(currentVal) else "-",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (holding.latestPrice > 0) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                // FIX #22: Show Avg Buy Price — a fundamental metric investors always need
                val avgBuyPrice = if (holding.unitsHeld > 0) holding.investedAmount / holding.unitsHeld else 0.0
                val dayGain = if (holding.previousClosePrice > 0)
                    holding.unitsHeld * (holding.latestPrice - holding.previousClosePrice) else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Avg Buy Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            MilestoneCalculator.formatInrExact(avgBuyPrice, showDecimals = true),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (holding.previousClosePrice > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Day's P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${if (dayGain >= 0) "+" else "-"}${MilestoneCalculator.formatInrExact(kotlin.math.abs(dayGain))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (dayGain >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Last sync: ${sdf.format(Date(holding.lastUpdatedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${if (gain >= 0) "+" else "-"}${MilestoneCalculator.formatInrExact(kotlin.math.abs(gain))} (${"%.2f".format(gainPct)}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (gain >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // FIX #22 / hint: tell user why values are missing instead of silent "-"
                Text(
                    "Prices not yet synced — tap \"Sync Live\" to fetch live prices",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TopUpDialog(
    holding: HoldingEntity,
    onDismiss: () -> Unit,
    onSave: (HoldingEntity) -> Unit
) {
    var newUnitsStr by remember { mutableStateOf("") }
    var newInvestedStr by remember { mutableStateOf("") }
    // FIX #17: Track validation state so user gets clear errors instead of silent nothing
    var saveAttempted by remember { mutableStateOf(false) }
    val unitsError = saveAttempted && (newUnitsStr.toDoubleOrNull() ?: 0.0) <= 0.0
    val investedError = saveAttempted && (newInvestedStr.toDoubleOrNull() ?: 0.0) <= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Top-Up ${holding.name.ifBlank { holding.identifier }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Current Units: ${holding.unitsHeld}\nCurrent Invested: ${MilestoneCalculator.formatInrExact(holding.investedAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newUnitsStr,
                    onValueChange = { newUnitsStr = it; saveAttempted = false },
                    label = { Text("New Units Bought") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    isError = unitsError,
                    supportingText = if (unitsError) {{ Text("Enter a valid number of units greater than 0") }} else null
                )
                OutlinedTextField(
                    value = newInvestedStr,
                    onValueChange = { newInvestedStr = it; saveAttempted = false },
                    label = { Text("New Amount Invested (₹)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    isError = investedError,
                    supportingText = if (investedError) {{ Text("Enter the amount invested in ₹") }} else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saveAttempted = true
                    val newUnits = newUnitsStr.toDoubleOrNull() ?: 0.0
                    val newInv = newInvestedStr.toDoubleOrNull() ?: 0.0
                    if (newUnits > 0 && newInv > 0) {
                        onSave(
                            holding.copy(
                                unitsHeld = holding.unitsHeld + newUnits,
                                investedAmount = holding.investedAmount + newInv
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
