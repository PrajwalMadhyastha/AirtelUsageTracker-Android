package com.prajwal.utilities.tools.cricketstats.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bowling_innings",
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
data class BowlingInningsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val ballsBowled: Int,
    val runsConceded: Int,
    val wickets: Int,
    val maidens: Int,
    val wides: Int,
    val noBalls: Int
)
