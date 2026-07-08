package com.prajwal.utilities.tools.wealthtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AssetSnapshotEntity::class, HoldingEntity::class],
    version = 4,
    // FIX #6: exportSchema = true lets Room generate JSON schema files under app/schemas/.
    // These are required for MigrationTestHelper — without them, every DB schema change
    // is unverifiable and risks silent data loss in production.
    exportSchema = true
)
abstract class WealthDatabase : RoomDatabase() {

    abstract fun wealthDao(): WealthDao
    abstract fun holdingsDao(): HoldingsDao

    companion object {
        @Volatile
        private var INSTANCE: WealthDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `holdings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `assetClass` TEXT NOT NULL, 
                        `instrumentType` TEXT NOT NULL, 
                        `identifier` TEXT NOT NULL, 
                        `exchange` TEXT, 
                        `unitsHeld` REAL NOT NULL, 
                        `investedAmount` REAL NOT NULL, 
                        `latestPrice` REAL NOT NULL, 
                        `lastUpdatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `holdings` ADD COLUMN `name` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `holdings` ADD COLUMN `previousClosePrice` REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): WealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WealthDatabase::class.java,
                    "wealth_tracker_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
