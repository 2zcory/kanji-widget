package com.example.kanjiwidget.stats

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

class StudyStatsRepository(private val context: Context) {
    fun getDailyChart(days: Int): StudyChartSummary {
        require(days > 0) { "days must be positive" }

        val today = LocalDate.now(ZoneId.systemDefault())
        val points = (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            StudyChartPoint(
                date = date,
                totalMs = StudyTimeTracker.getTotalMs(context, date),
                openCount = StudyTimeTracker.getOpenCount(context, date),
            )
        }

        val totalMs = points.sumOf { it.totalMs }
        val averageMs = if (points.isEmpty()) 0L else totalMs / points.size
        val bestDay = points.maxByOrNull { it.totalMs }?.takeIf { it.totalMs > 0L }

        return StudyChartSummary(
            points = points,
            totalMs = totalMs,
            averageMs = averageMs,
            bestDay = bestDay,
        )
    }
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
)
