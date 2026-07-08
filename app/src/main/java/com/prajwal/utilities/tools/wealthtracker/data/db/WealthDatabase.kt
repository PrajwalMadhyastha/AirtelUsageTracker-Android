package com.prajwal.utilities.tools.wealthtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AssetSnapshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WealthDatabase : RoomDatabase() {

    abstract fun wealthDao(): WealthDao

    companion object {
        @Volatile
        private var INSTANCE: WealthDatabase? = null

        fun getDatabase(context: Context): WealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WealthDatabase::class.java,
                    "wealth_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
