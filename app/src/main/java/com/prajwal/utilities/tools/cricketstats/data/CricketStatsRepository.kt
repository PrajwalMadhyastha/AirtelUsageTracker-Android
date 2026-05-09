package com.prajwal.utilities.tools.cricketstats.data

import com.prajwal.utilities.tools.cricketstats.data.db.BattingInningsEntity
import com.prajwal.utilities.tools.cricketstats.data.db.BowlingInningsEntity
import com.prajwal.utilities.tools.cricketstats.data.db.CricketStatsDao
import com.prajwal.utilities.tools.cricketstats.data.db.MatchEntity
import com.prajwal.utilities.tools.cricketstats.data.db.MatchWithInnings
import kotlinx.coroutines.flow.Flow

class CricketStatsRepository(private val dao: CricketStatsDao) {

    fun getAllMatches(): Flow<List<MatchWithInnings>> {
        return dao.getAllMatchesWithInnings()
    }

    suspend fun getMatch(id: Int): MatchWithInnings? {
        return dao.getMatchWithInnings(id)
    }

    suspend fun insertFullMatch(
        match: MatchEntity,
        battingInnings: BattingInningsEntity?,
        bowlingInnings: BowlingInningsEntity?
    ) {
        val matchId = dao.insertMatch(match).toInt()
        
        battingInnings?.let {
            dao.insertBattingInnings(it.copy(matchId = matchId))
        }
        
        bowlingInnings?.let {
            dao.insertBowlingInnings(it.copy(matchId = matchId))
        }
    }

    suspend fun deleteMatch(match: MatchEntity) {
        dao.deleteMatch(match)
    }
}
