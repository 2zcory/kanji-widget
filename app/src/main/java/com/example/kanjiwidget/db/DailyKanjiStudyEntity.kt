package com.example.kanjiwidget.db

import androidx.room.Entity

@Entity(tableName = "daily_kanji_study", primaryKeys = ["date", "kanji"])
data class DailyKanjiStudyEntity(
    val date: String, // YYYY-MM-DD
    val kanji: String,
    val studyTimeMs: Long,
    val openCount: Long
)
