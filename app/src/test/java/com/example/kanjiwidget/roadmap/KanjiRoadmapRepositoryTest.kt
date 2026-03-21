package com.example.kanjiwidget.roadmap

import com.example.kanjiwidget.widget.KanjiEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KanjiRoadmapRepositoryTest {

    @Test
    fun roadmapStageIdForJlpt_mapsSupportedLevels() {
        assertEquals(KanjiRoadmapStageId.N5_FOUNDATION, roadmapStageIdForJlpt("N5"))
        assertEquals(KanjiRoadmapStageId.N4_EXPANSION, roadmapStageIdForJlpt("n4"))
        assertEquals(KanjiRoadmapStageId.N1_EXPERT, roadmapStageIdForJlpt("N1"))
        assertEquals(KanjiRoadmapStageId.UNCLASSIFIED, roadmapStageIdForJlpt("N/A"))
    }

    @Test
    fun recommendedBatch_prefersCurrentStageAndSkipsCompleted() {
        val repo = FakeKanjiRoadmapRepository(
            completedKanji = setOf("日", "月"),
            entries = listOf(
                fakeEntry("日", "N5", grade = 1, frequency = 10),
                fakeEntry("月", "N5", grade = 1, frequency = 20),
                fakeEntry("火", "N5", grade = 1, frequency = 30),
                fakeEntry("水", "N5", grade = 2, frequency = 40),
                fakeEntry("木", "N4", grade = 3, frequency = 50),
            ),
        )

        val recommendation = repo.getRecommendedNextBatch(batchSize = 2, entries = repo.entries)

        assertEquals(KanjiRoadmapStageId.N5_FOUNDATION, recommendation.stage?.definition?.id)
        assertEquals(listOf("火", "水"), recommendation.batch.map { it.kanji })
    }

    @Test
    fun buildSnapshot_usesCompletedCountForCurrentStage() {
        val repo = FakeKanjiRoadmapRepository(
            completedKanji = setOf("日"),
            entries = listOf(
                fakeEntry("日", "N5", grade = 1, frequency = 10),
                fakeEntry("月", "N5", grade = 1, frequency = 20),
                fakeEntry("木", "N4", grade = 3, frequency = 50),
            ),
        )

        val snapshot = repo.buildSnapshot(repo.entries)

        assertEquals(KanjiRoadmapStageId.N5_FOUNDATION, snapshot.currentStage?.definition?.id)
        assertEquals(2, snapshot.currentStage?.totalCount)
        assertEquals(1, snapshot.currentStage?.completedCount)
        assertEquals(KanjiRoadmapStageId.N4_EXPANSION, snapshot.nextStage?.definition?.id)
        assertTrue(snapshot.completedKanji.contains("日"))
    }

    private fun fakeEntry(
        kanji: String,
        jlpt: String,
        grade: Int,
        frequency: Int,
    ): KanjiEntry {
        return KanjiEntry(
            kanji = kanji,
            onyomi = "",
            kunyomi = "",
            meaning = kanji,
            example = "",
            jlptLevel = jlpt,
            grade = grade,
            frequency = frequency,
        )
    }

    private class FakeKanjiRoadmapRepository(
        private val completedKanji: Set<String>,
        val entries: List<KanjiEntry>,
    ) {
        fun buildSnapshot(entries: List<KanjiEntry>): KanjiRoadmapSnapshot {
            val stages = DEFAULT_ROADMAP_STAGE_DEFINITIONS.map { definition ->
                val stageEntries = entries.filter { roadmapStageIdForJlpt(it.jlptLevel) == definition.id }
                KanjiRoadmapStageProgress(
                    definition = definition,
                    totalCount = stageEntries.size,
                    completedCount = stageEntries.count { completedKanji.contains(it.kanji) },
                )
            }
            val currentStage = stages.firstOrNull { it.totalCount > 0 && it.completedCount < it.totalCount }
                ?: stages.firstOrNull { it.totalCount > 0 }
            val nextStage = currentStage?.let { current ->
                stages.firstOrNull { it.definition.sortOrder > current.definition.sortOrder && it.totalCount > 0 }
            }
            return KanjiRoadmapSnapshot(stages, currentStage, nextStage, completedKanji)
        }

        fun getRecommendedNextBatch(batchSize: Int, entries: List<KanjiEntry>): KanjiRoadmapRecommendation {
            val snapshot = buildSnapshot(entries)
            val currentStage = snapshot.currentStage ?: return KanjiRoadmapRecommendation(null, emptyList())
            val batch = entries
                .asSequence()
                .filter { roadmapStageIdForJlpt(it.jlptLevel) == currentStage.definition.id }
                .filterNot { snapshot.completedKanji.contains(it.kanji) }
                .sortedWith(
                    compareBy<KanjiEntry> { it.grade ?: Int.MAX_VALUE }
                        .thenBy { it.frequency ?: Int.MAX_VALUE }
                        .thenBy { it.kanji }
                )
                .take(batchSize)
                .toList()
            return KanjiRoadmapRecommendation(currentStage, batch)
        }
    }
}
