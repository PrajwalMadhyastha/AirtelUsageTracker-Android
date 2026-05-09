package com.prajwal.utilities.tools.cricketstats.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "batting_innings",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("matchId")]
)
data class BattingInningsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val runsScored: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val howOut: String // e.g., "Not out", "Bowled", "Caught", "LBW", "Run out"
)
