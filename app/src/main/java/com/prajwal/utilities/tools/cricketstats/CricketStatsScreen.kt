package com.prajwal.utilities.tools.cricketstats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prajwal.utilities.tools.cricketstats.data.db.MatchWithInnings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketStatsScreen(
    viewModel: CricketStatsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddMatch: () -> Unit,
    onNavigateToEditMatch: (Int) -> Unit = {}
) {
    val matches by viewModel.matches.collectAsState()
    var matchToDelete by remember { mutableStateOf<MatchWithInnings?>(null) }

    // Calculate aggregated stats
    var totalRuns = 0
    var totalBallsFaced = 0
    var timesOut = 0
    var highestScore = 0
    var battingMatches = 0

    var totalWickets = 0
    var totalRunsConceded = 0
    var totalBallsBowled = 0
    var bowlingMatches = 0

    matches.forEach { matchWithInnings ->
        matchWithInnings.battingInnings?.let {
            battingMatches++
            totalRuns += it.runsScored
            totalBallsFaced += it.ballsFaced
            if (it.runsScored > highestScore) highestScore = it.runsScored
            if (it.howOut != "Not out" && it.howOut != "Retired Hurt") {
                timesOut++
            }
        }
        matchWithInnings.bowlingInnings?.let {
            bowlingMatches++
            totalWickets += it.wickets
            totalRunsConceded += it.runsConceded
            totalBallsBowled += it.ballsBowled
        }
    }

    val totalOversStr = run {
        val overs = totalBallsBowled / 6
        val rem = totalBallsBowled % 6
        if (rem > 0) "$overs.$rem" else "$overs"
    }

    val battingAvg = if (timesOut > 0) totalRuns.toFloat() / timesOut else if (totalRuns > 0) totalRuns.toFloat() else 0f
    val strikeRate = if (totalBallsFaced > 0) (totalRuns.toFloat() / totalBallsFaced) * 100 else 0f

    val bowlingAvg = if (totalWickets > 0) totalRunsConceded.toFloat() / totalWickets else 0f
    val economy = if (totalBallsBowled > 0) (totalRunsConceded.toFloat() / totalBallsBowled) * 6 else 0f

    // Delete confirmation dialog
    matchToDelete?.let { mwi ->
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("Delete Match?") },
            text = { Text("vs ${mwi.match.opponent} will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMatch(mwi.match)
                        matchToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cricket Stats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddMatch) {
                        Icon(Icons.Default.Add, contentDescription = "Add Match")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Career Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Batting",
                        stats = listOf(
                            "Matches" to battingMatches.toString(),
                            "Runs" to totalRuns.toString(),
                            "Avg" to String.format("%.2f", battingAvg),
                            "SR" to String.format("%.2f", strikeRate),
                            "HS" to highestScore.toString()
                        )
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Bowling",
                        stats = listOf(
                            "Matches" to bowlingMatches.toString(),
                            "Wickets" to totalWickets.toString(),
                            "Overs" to totalOversStr,
                            "Avg" to String.format("%.2f", bowlingAvg),
                            "Econ" to String.format("%.2f", economy)
                        )
                    )
                }
            }

            item {
                Text(
                    "All Matches (${matches.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (matches.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No matches played yet. Add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(matches, key = { it.match.id }) { matchWithInnings ->
                    MatchCard(
                        matchWithInnings = matchWithInnings,
                        onEditClick = { onNavigateToEditMatch(matchWithInnings.match.id) },
                        onDeleteClick = { matchToDelete = matchWithInnings }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, stats: List<Pair<String, String>>) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    matchWithInnings: MatchWithInnings,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(Date(matchWithInnings.match.date))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "vs ${matchWithInnings.match.opponent}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${matchWithInnings.match.matchType} • $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit match",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete match",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (matchWithInnings.battingInnings != null || matchWithInnings.bowlingInnings != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            matchWithInnings.battingInnings?.let {
                Text("Bat: ${it.runsScored}(${it.ballsFaced}) — ${it.howOut}", style = MaterialTheme.typography.bodyMedium)
            }
            matchWithInnings.bowlingInnings?.let {
                val overs = it.ballsBowled / 6
                val extraBalls = it.ballsBowled % 6
                val oversStr = if (extraBalls > 0) "$overs.$extraBalls" else "$overs"
                Text("Bowl: ${it.wickets}/${it.runsConceded} ($oversStr overs)", style = MaterialTheme.typography.bodyMedium)
            }

            if (matchWithInnings.battingInnings == null && matchWithInnings.bowlingInnings == null) {
                Text("No stats recorded", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
