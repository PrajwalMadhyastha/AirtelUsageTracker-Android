package com.prajwal.utilities.tools.crickettoss.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "toss_history")
data class TossEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val result: String, // "H" or "T"
    val timestamp: Long = System.currentTimeMillis()
)
