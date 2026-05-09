package com.prajwal.utilities.tools.cricketstats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.tools.cricketstats.data.db.BattingInningsEntity
import com.prajwal.utilities.tools.cricketstats.data.db.BowlingInningsEntity
import com.prajwal.utilities.tools.cricketstats.data.db.MatchEntity
import com.prajwal.utilities.tools.cricketstats.data.db.MatchWithInnings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMatchScreen(
    onNavigateBack: () -> Unit,
    onSaveMatch: (MatchEntity, BattingInningsEntity?, BowlingInningsEntity?) -> Unit,
    // Edit mode: when non-null, the form is pre-populated and onUpdateMatch is invoked on save
    initialData: MatchWithInnings? = null,
    onUpdateMatch: ((MatchEntity, BattingInningsEntity?, BowlingInningsEntity?) -> Unit)? = null
) {
    val isEditMode = initialData != null
    val existingMatch = initialData?.match

    var opponent by remember { mutableStateOf(existingMatch?.opponent ?: "") }
    var matchType by remember { mutableStateOf(existingMatch?.matchType ?: "T20") }
    var notes by remember { mutableStateOf(existingMatch?.notes ?: "") }

    // Date State
    var selectedDateMillis by remember { mutableStateOf(existingMatch?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateDisplayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Batting State
    val existingBatting = initialData?.battingInnings
    var didBat by remember { mutableStateOf(existingBatting != null) }
    var runsScored by remember { mutableStateOf(existingBatting?.runsScored?.toString() ?: "") }
    var ballsFaced by remember { mutableStateOf(existingBatting?.ballsFaced?.toString() ?: "") }
    var fours by remember { mutableStateOf(existingBatting?.fours?.toString() ?: "") }
    var sixes by remember { mutableStateOf(existingBatting?.sixes?.toString() ?: "") }
    var howOut by remember { mutableStateOf(existingBatting?.howOut ?: "Not out") }
    var showHowOutMenu by remember { mutableStateOf(false) }
    val dismissalTypes = listOf("Not out", "Bowled", "Caught", "LBW", "Run out", "Stumped", "Retired Hurt")

    // Bowling State
    val existingBowling = initialData?.bowlingInnings
    var didBowl by remember { mutableStateOf(existingBowling != null) }
    var ballsBowled by remember { mutableStateOf(existingBowling?.ballsBowled?.toString() ?: "") }
    var runsConceded by remember { mutableStateOf(existingBowling?.runsConceded?.toString() ?: "") }
    var wickets by remember { mutableStateOf(existingBowling?.wickets?.toString() ?: "") }
    var maidens by remember { mutableStateOf(existingBowling?.maidens?.toString() ?: "") }
    var wides by remember { mutableStateOf(existingBowling?.wides?.toString() ?: "") }
    var noBalls by remember { mutableStateOf(existingBowling?.noBalls?.toString() ?: "") }

    fun buildAndSave() {
        val match = MatchEntity(
            id = existingMatch?.id ?: 0,
            date = selectedDateMillis,
            opponent = opponent.ifBlank { "Unknown" },
            matchType = matchType,
            notes = notes.takeIf { it.isNotBlank() }
        )
        val batting = if (didBat) {
            BattingInningsEntity(
                id = existingBatting?.id ?: 0,
                matchId = match.id,
                runsScored = runsScored.toIntOrNull() ?: 0,
                ballsFaced = ballsFaced.toIntOrNull() ?: 0,
                fours = fours.toIntOrNull() ?: 0,
                sixes = sixes.toIntOrNull() ?: 0,
                howOut = howOut
            )
        } else null
        val bowling = if (didBowl) {
            BowlingInningsEntity(
                id = existingBowling?.id ?: 0,
                matchId = match.id,
                ballsBowled = ballsBowled.toIntOrNull() ?: 0,
                runsConceded = runsConceded.toIntOrNull() ?: 0,
                wickets = wickets.toIntOrNull() ?: 0,
                maidens = maidens.toIntOrNull() ?: 0,
                wides = wides.toIntOrNull() ?: 0,
                noBalls = noBalls.toIntOrNull() ?: 0
            )
        } else null

        if (isEditMode) {
            onUpdateMatch?.invoke(match, batting, bowling)
        } else {
            onSaveMatch(match, batting, bowling)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Match" else "Add Match") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        buildAndSave()
                    }) {
                        Text(if (isEditMode) "Update" else "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Info
            Text("Match Details", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = dateDisplayFormat.format(Date(selectedDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Match Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = opponent,
                onValueChange = { opponent = it },
                label = { Text("Opponent Team") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = matchType,
                onValueChange = { matchType = it },
                label = { Text("Match Type (e.g., T20, ODI)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            HorizontalDivider()

            // Batting Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Batting", style = MaterialTheme.typography.titleMedium)
                Switch(checked = didBat, onCheckedChange = { didBat = it })
            }
            if (didBat) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = runsScored,
                        onValueChange = { runsScored = it },
                        label = { Text("Runs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ballsFaced,
                        onValueChange = { ballsFaced = it },
                        label = { Text("Balls") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fours,
                        onValueChange = { fours = it },
                        label = { Text("4s") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sixes,
                        onValueChange = { sixes = it },
                        label = { Text("6s") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // How Out dropdown
                ExposedDropdownMenuBox(
                    expanded = showHowOutMenu,
                    onExpandedChange = { showHowOutMenu = !showHowOutMenu }
                ) {
                    OutlinedTextField(
                        value = howOut,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("How Out") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showHowOutMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showHowOutMenu,
                        onDismissRequest = { showHowOutMenu = false }
                    ) {
                        dismissalTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    howOut = type
                                    showHowOutMenu = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Bowling Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bowling", style = MaterialTheme.typography.titleMedium)
                Switch(checked = didBowl, onCheckedChange = { didBowl = it })
            }
            if (didBowl) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ballsBowled,
                        onValueChange = { ballsBowled = it },
                        label = { Text("Total Balls") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = runsConceded,
                        onValueChange = { runsConceded = it },
                        label = { Text("Runs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wickets,
                        onValueChange = { wickets = it },
                        label = { Text("Wickets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maidens,
                        onValueChange = { maidens = it },
                        label = { Text("Maidens") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wides,
                        onValueChange = { wides = it },
                        label = { Text("Wides") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = noBalls,
                        onValueChange = { noBalls = it },
                        label = { Text("No Balls") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
