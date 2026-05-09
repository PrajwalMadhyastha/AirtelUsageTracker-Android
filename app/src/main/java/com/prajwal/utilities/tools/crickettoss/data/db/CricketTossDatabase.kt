package com.prajwal.utilities.tools.crickettoss.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TossEntity::class], version = 1, exportSchema = false)
abstract class CricketTossDatabase : RoomDatabase() {
    abstract fun tossDao(): TossDao

    companion object {
        @Volatile
        private var INSTANCE: CricketTossDatabase? = null

        fun getDatabase(context: Context): CricketTossDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketTossDatabase::class.java,
                    "cricket_toss_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
