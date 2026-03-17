package com.example.kanjiwidget.stats

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

import com.example.kanjiwidget.db.AppDatabase
import kotlinx.coroutines.runBlocking

class StudyStatsRepository(
    private val context: Context,
    private val todayProvider: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) },
) {
    private val db by lazy { AppDatabase.getInstance(context) }

    fun getDailyChart(days: Int): StudyChartSummary = runBlocking {
        val today = todayProvider()
        val startDate = today.minusDays(days.toLong() - 1)
        
        val dailyTotals = db.dailyTotalStudyDao().getDailyTotals(
            startDate = startDate.toString(),
            endDate = today.toString()
        ).associateBy { it.date }

        buildStudyChartSummary(
            days = days,
            today = today,
            totalMsForDate = { date -> dailyTotals[date.toString()]?.totalStudyMs ?: 0L },
            openCountForDate = { date -> dailyTotals[date.toString()]?.totalOpenCount ?: 0 },
        )
    }
}

internal fun buildStudyChartSummary(
    days: Int,
    today: LocalDate,
    totalMsForDate: (LocalDate) -> Long,
    openCountForDate: (LocalDate) -> Int,
): StudyChartSummary {
    require(days > 0) { "days must be positive" }

    val points = (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        StudyChartPoint(
            date = date,
            totalMs = totalMsForDate(date),
            openCount = openCountForDate(date),
        )
    }

    val totalMs = points.sumOf { it.totalMs }
    val averageMs = if (points.isEmpty()) 0L else totalMs / points.size
    val bestDay = points.maxByOrNull { it.totalMs }?.takeIf { it.totalMs > 0L }
    val activeDays = points.count { it.totalMs > 0L }
    val currentStreakDays = points
        .asReversed()
        .takeWhile { it.totalMs > 0L }
        .count()

    return StudyChartSummary(
        points = points,
        totalMs = totalMs,
        averageMs = averageMs,
        bestDay = bestDay,
        activeDays = activeDays,
        currentStreakDays = currentStreakDays,
    )
}

data class StudyChartPoint(
    val date: LocalDate,
    val totalMs: Long,
    val openCount: Int,
)

data class StudyChartSummary(
    val points: List<StudyChartPoint>,
    val totalMs: Long,
    val averageMs: Long,
    val bestDay: StudyChartPoint?,
    val activeDays: Int,
    val currentStreakDays: Int,
)
