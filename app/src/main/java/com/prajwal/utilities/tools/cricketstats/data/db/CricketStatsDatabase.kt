package com.prajwal.utilities.tools.cricketstats.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MatchEntity::class, BattingInningsEntity::class, BowlingInningsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CricketStatsDatabase : RoomDatabase() {
    abstract fun cricketStatsDao(): CricketStatsDao

    companion object {
        @Volatile
        private var INSTANCE: CricketStatsDatabase? = null

        fun getDatabase(context: Context): CricketStatsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketStatsDatabase::class.java,
                    "cricket_stats_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
