package com.example.kanjiwidget.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_total_study")
data class DailyTotalStudyEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val totalStudyMs: Long,
    val totalOpenCount: Int
)
