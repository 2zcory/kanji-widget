package com.example.kanjiwidget.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailyKanjiStudyEntity::class, DailyTotalStudyEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyKanjiStudyDao(): DailyKanjiStudyDao
    abstract fun dailyTotalStudyDao(): DailyTotalStudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kanji_widget_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
