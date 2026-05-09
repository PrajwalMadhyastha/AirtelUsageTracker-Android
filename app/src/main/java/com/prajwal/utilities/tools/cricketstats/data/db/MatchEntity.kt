package com.prajwal.utilities.tools.cricketstats.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val opponent: String,
    val matchType: String, // e.g., T20, ODI, Test, Box Cricket, etc.
    val notes: String? = null
)
