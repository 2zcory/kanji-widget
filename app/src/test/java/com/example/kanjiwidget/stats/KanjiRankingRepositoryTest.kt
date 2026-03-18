package com.example.kanjiwidget.stats

import com.example.kanjiwidget.db.KanjiRankingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KanjiRankingRepositoryTest {
    @Test
    fun buildRankingFromResults_allTimeAggregatesAndHonorsTieBreaks() {
        val results = listOf(
            KanjiRankingResult("日", 180_000L, 10L, "2026-03-11"),
            KanjiRankingResult("月", 180_000L, 5L, "2026-03-10"),
            KanjiRankingResult("A", 60_000L, 2L, "2026-03-11"),
            KanjiRankingResult("B", 60_000L, 2L, "2026-03-11"),
        )

        val ranking = buildRankingFromResults(
            results = results,
            scope = RankingScope.ALL_TIME,
            metric = RankingMetric.STUDY_TIME,
            limit = 10,
            metadataProvider = { null },
        )

        // "日" should be first due to more recent activity than "月" (tie break on time)
        assertEquals(listOf("日", "月"), ranking.mostRanked.take(2).map { it.kanji })
        // "A" should be before "B" in least ranked due to alphabetical tie break
        assertEquals(listOf("A", "B"), ranking.leastRanked.take(2).map { it.kanji })
    }

    @Test
    fun buildRankingFromResults_openCountMetric() {
        val results = listOf(
            KanjiRankingResult("日", 1000L, 7L, "2026-03-11"),
            KanjiRankingResult("月", 2000L, 10L, "2026-03-11"),
        )

        val ranking = buildRankingFromResults(
            results = results,
            scope = RankingScope.ALL_TIME,
            metric = RankingMetric.OPEN_COUNT,
            limit = 10,
            metadataProvider = { null },
        )

        assertEquals(listOf("月", "日"), ranking.mostRanked.map { it.kanji })
        assertEquals(10L, ranking.mostRanked.find { it.kanji == "月" }?.openCount)
        assertEquals(7L, ranking.mostRanked.find { it.kanji == "日" }?.openCount)
    }

    @Test
    fun buildRankingFromResults_filtersZeroPrimaryMetric() {
        val results = listOf(
            KanjiRankingResult("ZERO", 0L, 0L, "2026-03-11"),
            KanjiRankingResult("ACTIVE", 100L, 1L, "2026-03-11"),
        )

        val ranking = buildRankingFromResults(
            results = results,
            scope = RankingScope.ALL_TIME,
            metric = RankingMetric.STUDY_TIME,
            limit = 10,
            metadataProvider = { null },
        )

        val kanjis = ranking.mostRanked.map { it.kanji }
        assertTrue("ACTIVE" in kanjis)
        assertTrue("ZERO" !in kanjis)
    }
}
