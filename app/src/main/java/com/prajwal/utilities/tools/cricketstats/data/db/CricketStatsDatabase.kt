package com.prajwal.utilities.tools.cricketstats.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MatchEntity::class, BattingInningsEntity::class, BowlingInningsEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CricketStatsDatabase : RoomDatabase() {
    abstract fun cricketStatsDao(): CricketStatsDao

    companion object {
        @Volatile
        private var INSTANCE: CricketStatsDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `toss_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `result` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `toss_history`")
            }
        }

        fun getDatabase(context: Context): CricketStatsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketStatsDatabase::class.java,
                    "cricket_stats_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

