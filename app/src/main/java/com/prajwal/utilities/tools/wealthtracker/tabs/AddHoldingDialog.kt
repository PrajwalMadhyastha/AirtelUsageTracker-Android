package com.prajwal.utilities.tools.wealthtracker.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.tools.wealthtracker.data.db.HoldingEntity
import com.prajwal.utilities.tools.wealthtracker.data.network.AssetSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHoldingDialog(
    holdingToEdit: HoldingEntity? = null,
    searchResults: List<AssetSearchResult>,
    onSearchQueryChanged: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (HoldingEntity) -> Unit
) {
    var assetClass by remember { mutableStateOf(holdingToEdit?.assetClass ?: "Equity") }
    var instrumentType by remember { mutableStateOf(holdingToEdit?.instrumentType ?: "Stock") }
    var identifier by remember { mutableStateOf(holdingToEdit?.identifier ?: "") }
    var exchange by remember { mutableStateOf(holdingToEdit?.exchange ?: "NSE") }
    
    fun fmt(v: Double) = if (v > 0) { if (v % 1.0 == 0.0) v.toLong().toString() else v.toString() } else ""
    var unitsStr by remember { mutableStateOf(holdingToEdit?.let { fmt(it.unitsHeld) } ?: "") }
    var investedStr by remember { mutableStateOf(holdingToEdit?.let { fmt(it.investedAmount) } ?: "") }

    val assetClasses = listOf("Equity", "Gold", "Debt", "Silver", "REITs")
    val instrumentTypes = listOf("Stock", "MF", "SGB")
    val exchanges = listOf("NSE", "BSE")

    var assetExpanded by remember { mutableStateOf(false) }
    var instrumentExpanded by remember { mutableStateOf(false) }
    var exchangeExpanded by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf(holdingToEdit?.name ?: "") }
    var searchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, instrumentType) {
        onSearchQueryChanged(searchQuery, instrumentType == "MF")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (holdingToEdit != null) "Edit Holding" else "Add Holding") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Asset Class Dropdown
                ExposedDropdownMenuBox(
                    expanded = assetExpanded,
                    onExpandedChange = { assetExpanded = !assetExpanded }
                ) {
                    OutlinedTextField(
                        value = assetClass,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asset Class") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = assetExpanded,
                        onDismissRequest = { assetExpanded = false }
                    ) {
                        assetClasses.forEach { ac ->
                            DropdownMenuItem(
                                text = { Text(ac) },
                                onClick = { assetClass = ac; assetExpanded = false }
                            )
                        }
                    }
                }

                // Instrument Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = instrumentExpanded,
                    onExpandedChange = { instrumentExpanded = !instrumentExpanded }
                ) {
                    OutlinedTextField(
                        value = instrumentType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Instrument Type") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = instrumentExpanded,
                        onDismissRequest = { instrumentExpanded = false }
                    ) {
                        instrumentTypes.forEach { it ->
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = { instrumentType = it; instrumentExpanded = false }
                            )
                        }
                    }
                }

                // Exchange Dropdown (Only for Stock/SGB)
                if (instrumentType != "MF") {
                    ExposedDropdownMenuBox(
                        expanded = exchangeExpanded,
                        onExpandedChange = { exchangeExpanded = !exchangeExpanded }
                    ) {
                        OutlinedTextField(
                            value = exchange,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exchange") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = exchangeExpanded,
                            onDismissRequest = { exchangeExpanded = false }
                        ) {
                            exchanges.forEach { ex ->
                                DropdownMenuItem(
                                    text = { Text(ex) },
                                    onClick = { exchange = ex; exchangeExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Autocomplete Dropdown
                ExposedDropdownMenuBox(
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it }
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; searchExpanded = true },
                        label = { Text(if (instrumentType == "MF") "Search Mutual Fund" else "Search Stock/ETF") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    if (searchResults.isNotEmpty()) {
                        DropdownMenu(
                            expanded = searchExpanded,
                            onDismissRequest = { searchExpanded = false },
                            modifier = Modifier.exposedDropdownSize(),
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                        ) {
                            searchResults.forEach { result ->
                                DropdownMenuItem(
                                    text = { Text(result.name) },
                                    onClick = {
                                        searchQuery = result.name
                                        identifier = result.identifier
                                        if (result.exchange != null) exchange = result.exchange
                                        searchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Units
                    OutlinedTextField(
                        value = unitsStr,
                        onValueChange = { unitsStr = it },
                        label = { Text("Units") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Invested Amount
                    OutlinedTextField(
                        value = investedStr,
                        onValueChange = { investedStr = it },
                        label = { Text("Invested (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val u = unitsStr.toDoubleOrNull() ?: 0.0
                    val i = investedStr.toDoubleOrNull() ?: 0.0
                    
                    val finalIdentifier = if (identifier.isNotBlank()) identifier else searchQuery.trim()
                    
                    if (finalIdentifier.isNotBlank() && u > 0 && i > 0) {
                        val newHolding = HoldingEntity(
                            id = holdingToEdit?.id ?: 0,
                            assetClass = assetClass,
                            instrumentType = instrumentType,
                            name = searchQuery.trim(),
                            identifier = finalIdentifier,
                            exchange = if (instrumentType == "MF") null else exchange,
                            unitsHeld = u,
                            investedAmount = i,
                            latestPrice = holdingToEdit?.latestPrice ?: 0.0,
                            lastUpdatedAt = holdingToEdit?.lastUpdatedAt ?: System.currentTimeMillis()
                        )
                        onSave(newHolding)
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
