package com.example.kanjiwidget.db

import androidx.room.*

@Dao
interface DailyKanjiStudyDao {
    @Query("""
        SELECT kanji, SUM(studyTimeMs) as totalStudyTimeMs, SUM(openCount) as totalOpenCount, MAX(date) as lastActivityDate
        FROM daily_kanji_study
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY kanji
    """)
    suspend fun getRanking(startDate: String, endDate: String): List<KanjiRankingResult>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyKanjiStudyEntity)

    @Query("SELECT * FROM daily_kanji_study WHERE date = :date AND kanji = :kanji")
    suspend fun getEntry(date: String, kanji: String): DailyKanjiStudyEntity?
    
    @Query("SELECT * FROM daily_kanji_study")
    suspend fun getAllEntries(): List<DailyKanjiStudyEntity>
}

data class KanjiRankingResult(
    val kanji: String,
    val totalStudyTimeMs: Long,
    val totalOpenCount: Long,
    val lastActivityDate: String
)
