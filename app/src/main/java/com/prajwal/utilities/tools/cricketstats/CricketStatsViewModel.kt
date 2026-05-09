package com.prajwal.utilities.tools.cricketstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwal.utilities.tools.cricketstats.data.CricketStatsRepository
import com.prajwal.utilities.tools.cricketstats.data.db.BattingInningsEntity
import com.prajwal.utilities.tools.cricketstats.data.db.BowlingInningsEntity
import com.prajwal.utilities.tools.cricketstats.data.db.MatchEntity
import com.prajwal.utilities.tools.cricketstats.data.db.MatchWithInnings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CricketStatsViewModel(
    private val repository: CricketStatsRepository
) : ViewModel() {

    val matches: StateFlow<List<MatchWithInnings>> = repository.getAllMatches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveMatch(
        match: MatchEntity,
        battingInnings: BattingInningsEntity?,
        bowlingInnings: BowlingInningsEntity?
    ) {
        viewModelScope.launch {
            repository.insertFullMatch(match, battingInnings, bowlingInnings)
        }
    }

    fun updateMatch(
        match: MatchEntity,
        battingInnings: BattingInningsEntity?,
        bowlingInnings: BowlingInningsEntity?
    ) {
        viewModelScope.launch {
            repository.updateFullMatch(match, battingInnings, bowlingInnings)
        }
    }

    suspend fun getMatchById(matchId: Int): MatchWithInnings? {
        return repository.getMatch(matchId)
    }

    fun deleteMatch(match: MatchEntity) {
        viewModelScope.launch {
            repository.deleteMatch(match)
        }
    }
}

