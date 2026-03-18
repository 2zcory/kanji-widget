package com.example.kanjiwidget.db

import androidx.room.*

@Dao
interface DailyTotalStudyDao {
    @Query("SELECT * FROM daily_total_study WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getDailyTotals(startDate: String, endDate: String): List<DailyTotalStudyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyTotalStudyEntity)

    @Query("SELECT * FROM daily_total_study WHERE date = :date")
    suspend fun getEntry(date: String): DailyTotalStudyEntity?
    
    @Query("SELECT * FROM daily_total_study")
    suspend fun getAllEntries(): List<DailyTotalStudyEntity>
}
