package com.prajwal.utilities.tools.crickettoss.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TossDao {
    @Query("SELECT * FROM toss_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentTosses(): Flow<List<TossEntity>>

    @Insert
    suspend fun insertToss(toss: TossEntity)

    @Query("DELETE FROM toss_history WHERE id NOT IN (SELECT id FROM toss_history ORDER BY timestamp DESC LIMIT 10)")
    suspend fun clearOldTosses()
}
