package com.airtel.usagetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(usage: UsageEntity)

    @Query("SELECT * FROM usage_records ORDER BY timestamp DESC")
    fun getAllUsage(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestUsage(): UsageEntity?

    @Query("SELECT * FROM usage_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getUsageInRange(startTime: Long, endTime: Long): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getUsageInRangeSync(startTime: Long, endTime: Long): List<UsageEntity>

    @Query("SELECT * FROM usage_records WHERE timestamp >= :timestamp ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstUsageAfter(timestamp: Long): UsageEntity?

    @Query("DELETE FROM usage_records")
    suspend fun clearAll()
}
