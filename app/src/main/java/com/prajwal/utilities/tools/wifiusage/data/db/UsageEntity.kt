package com.prajwal.utilities.tools.wifiusage.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_records")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val txBytes: Long,
    val rxBytes: Long,
    val totalBytes: Long,
    val uptimeSeconds: Long
)
