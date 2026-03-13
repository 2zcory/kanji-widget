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
            limit = 10,
            today = today,
            metadataProvider = { null },
        )

        assertEquals(listOf("日", "月"), ranking.mostStudied.take(2).map { it.kanji })
        assertEquals(listOf("A", "B"), ranking.leastStudied.take(2).map { it.kanji })
        assertTrue(ranking.mostStudied.any { it.kanji == "日" && it.totalStudyMs == 180_000L })
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
            limit = 10,
            today = today,
            metadataProvider = { null },
        )

        assertEquals(listOf("E"), ranking.mostStudied.map { it.kanji })
        assertEquals(listOf("E"), ranking.leastStudied.map { it.kanji })
    }
}
