package com.prajwal.utilities.tools.cricketstats.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MatchEntity::class, BattingInningsEntity::class, BowlingInningsEntity::class, com.prajwal.utilities.tools.crickettoss.data.db.TossEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CricketStatsDatabase : RoomDatabase() {
    abstract fun cricketStatsDao(): CricketStatsDao
    abstract fun tossDao(): com.prajwal.utilities.tools.crickettoss.data.db.TossDao

    companion object {
        @Volatile
        private var INSTANCE: CricketStatsDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `toss_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `result` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context): CricketStatsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketStatsDatabase::class.java,
                    "cricket_stats_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
