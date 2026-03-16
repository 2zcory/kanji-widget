package com.example.kanjiwidget.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KanjiRankingRepositoryTest {
    @Test
    fun buildRankingFromEntries_allTimeAggregatesAndHonorsTieBreaks() {
        val today = LocalDate.of(2026, 3, 11)
        val entries = mapOf(
            "study_kanji_${today}_日" to 120_000L,
            "study_kanji_${today.minusDays(1)}_日" to 60_000L,
            "study_kanji_${today.minusDays(1)}_月" to 180_000L,
            "study_kanji_${today}_A" to 60_000L,
            "study_kanji_${today}_B" to 60_000L,
        )

        val ranking = buildRankingFromEntries(
            entries = entries,
            scope = RankingScope.ALL_TIME,
            metric = RankingMetric.STUDY_TIME,
            limit = 10,
            today = today,
            metadataProvider = { null },
        )

        assertEquals(listOf("日", "月"), ranking.mostRanked.take(2).map { it.kanji })
        assertEquals(listOf("A", "B"), ranking.leastRanked.take(2).map { it.kanji })
        assertTrue(ranking.mostRanked.any { it.kanji == "日" && it.totalStudyMs == 180_000L })
    }

    @Test
    fun buildRankingFromEntries_last30DaysFiltersOlderAndZeroStudy() {
        val today = LocalDate.of(2026, 3, 11)
        val entries = mapOf(
            "study_kanji_${today.minusDays(31)}_C" to 120_000L,
            "study_kanji_${today.minusDays(5)}_D" to 0L,
            "study_kanji_${today.minusDays(2)}_E" to 1_000L,
        )

        val ranking = buildRankingFromEntries(
            entries = entries,
            scope = RankingScope.LAST_30_DAYS,
            metric = RankingMetric.STUDY_TIME,
            limit = 10,
            today = today,
            metadataProvider = { null },
        )

        assertEquals(listOf("E"), ranking.mostRanked.map { it.kanji })
        assertEquals(listOf("E"), ranking.leastRanked.map { it.kanji })
    }

    @Test
    fun buildRankingFromEntries_openCountMetric() {
        val today = LocalDate.of(2026, 3, 11)
        val entries = mapOf(
            "study_open_kanji_${today}_日" to 5L,
            "study_open_kanji_${today.minusDays(1)}_日" to 2L,
            "study_open_kanji_${today}_月" to 10L,
            "study_kanji_${today}_日" to 1000L, // Time data should be ignored for open count sorting
        )

        val ranking = buildRankingFromEntries(
            entries = entries,
            scope = RankingScope.ALL_TIME,
            metric = RankingMetric.OPEN_COUNT,
            limit = 10,
            today = today,
            metadataProvider = { null },
        )

        assertEquals(listOf("月", "日"), ranking.mostRanked.map { it.kanji })
        assertEquals(7L, ranking.mostRanked.find { it.kanji == "日" }?.openCount)
        assertEquals(10L, ranking.mostRanked.find { it.kanji == "月" }?.openCount)
    }

    @Test
    fun buildRankingFromEntries_last7DaysScope() {
        val today = LocalDate.of(2026, 3, 11)
        val entries = mapOf(
            "study_kanji_${today.minusDays(8)}_OLD" to 1000L,
            "study_kanji_${today.minusDays(6)}_NEW" to 500L,
            "study_kanji_${today}_TODAY" to 200L,
        )

        val ranking = buildRankingFromEntries(
            entries = entries,
            scope = RankingScope.LAST_7_DAYS,
            metric = RankingMetric.STUDY_TIME,
            limit = 10,
            today = today,
            metadataProvider = { null },
        )

        val kanjis = ranking.mostRanked.map { it.kanji }
        assertTrue("NEW" in kanjis)
        assertTrue("TODAY" in kanjis)
        assertTrue("OLD" !in kanjis)
    }
}
