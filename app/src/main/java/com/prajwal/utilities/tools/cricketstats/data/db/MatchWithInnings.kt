package com.prajwal.utilities.tools.cricketstats.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class MatchWithInnings(
    @Embedded val match: MatchEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "matchId"
    )
    val battingInnings: BattingInningsEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "matchId"
    )
    val bowlingInnings: BowlingInningsEntity?
)
