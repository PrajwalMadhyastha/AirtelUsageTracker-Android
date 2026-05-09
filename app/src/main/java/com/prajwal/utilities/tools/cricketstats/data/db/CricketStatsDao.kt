package com.prajwal.utilities.tools.cricketstats.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CricketStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattingInnings(innings: BattingInningsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBowlingInnings(innings: BowlingInningsEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity): Int

    @Update
    suspend fun updateBattingInnings(innings: BattingInningsEntity): Int

    @Update
    suspend fun updateBowlingInnings(innings: BowlingInningsEntity): Int

    @Delete
    suspend fun deleteMatch(match: MatchEntity): Int

    @Transaction
    @Query("SELECT * FROM matches ORDER BY date DESC")
    fun getAllMatchesWithInnings(): Flow<List<MatchWithInnings>>

    @Transaction
    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatchWithInnings(matchId: Int): MatchWithInnings?

}
