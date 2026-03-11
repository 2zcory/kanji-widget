package com.example.kanjiwidget.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StudyStatsRepositoryTest {
    @Test
    fun buildStudyChartSummary_returnsOrderedPointsAndDerivedInsights() {
        val today = LocalDate.of(2026, 3, 11)
        val totals = mapOf(
            today.minusDays(4) to 45_000L,
            today.minusDays(2) to 120_000L,
            today.minusDays(1) to 90_000L,
            today to 30_000L,
        )
        val opens = mapOf(
            today.minusDays(4) to 1,
            today.minusDays(2) to 2,
            today.minusDays(1) to 1,
            today to 1,
        )

        val summary = buildStudyChartSummary(
            days = 5,
            today = today,
            totalMsForDate = { date -> totals[date] ?: 0L },
            openCountForDate = { date -> opens[date] ?: 0 },
        )

        assertEquals(5, summary.points.size)
        assertEquals(today.minusDays(4), summary.points.first().date)
        assertEquals(today, summary.points.last().date)
        assertEquals(listOf(45_000L, 0L, 120_000L, 90_000L, 30_000L), summary.points.map { it.totalMs })
        assertEquals(285_000L, summary.totalMs)
        assertEquals(57_000L, summary.averageMs)
        assertEquals(4, summary.activeDays)
        assertEquals(3, summary.currentStreakDays)
        assertEquals(today.minusDays(2), summary.bestDay?.date)
        assertEquals(120_000L, summary.bestDay?.totalMs)
    }

    @Test
    fun buildStudyChartSummary_returnsZeroStreakWhenTodayHasNoStudyTime() {
        val today = LocalDate.of(2026, 3, 11)
        val totals = mapOf(
            today.minusDays(2) to 120_000L,
            today.minusDays(1) to 90_000L,
        )

        val summary = buildStudyChartSummary(
            days = 3,
            today = today,
            totalMsForDate = { date -> totals[date] ?: 0L },
            openCountForDate = { 0 },
        )

        assertEquals(2, summary.activeDays)
        assertEquals(0, summary.currentStreakDays)
        assertEquals(today.minusDays(2), summary.points.first().date)
        assertEquals(today, summary.points.last().date)
    }

    @Test
    fun buildStudyChartSummary_returnsNullBestDayWhenRangeHasNoStudyTime() {
        val today = LocalDate.of(2026, 3, 11)

        val summary = buildStudyChartSummary(
            days = 7,
            today = today,
            totalMsForDate = { 0L },
            openCountForDate = { 0 },
        )

        assertEquals(0L, summary.totalMs)
        assertEquals(0L, summary.averageMs)
        assertEquals(0, summary.activeDays)
        assertEquals(0, summary.currentStreakDays)
        assertNull(summary.bestDay)
    }
}
