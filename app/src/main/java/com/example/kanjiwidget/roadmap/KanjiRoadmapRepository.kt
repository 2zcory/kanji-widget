package com.example.kanjiwidget.roadmap

import android.content.Context
import com.example.kanjiwidget.widget.KanjiEntry
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

class KanjiRoadmapRepository(
    private val context: Context,
    private val stageDefinitions: List<KanjiRoadmapStageDefinition> = DEFAULT_ROADMAP_STAGE_DEFINITIONS,
) {

    fun getStageDefinitions(): List<KanjiRoadmapStageDefinition> = stageDefinitions

    fun getCompletedKanji(): Set<String> = KanjiCompletionPrefs.getCompletedKanji(context)

    fun buildSnapshot(entries: List<KanjiEntry> = loadKnownEntries()): KanjiRoadmapSnapshot {
        val completedKanji = getCompletedKanji()
        val stages = stageDefinitions.map { definition ->
            val stageEntries = entries.filter { entry ->
                roadmapStageIdForJlpt(entry.jlptLevel) == definition.id
            }
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
        return KanjiRoadmapSnapshot(
            stages = stages,
            currentStage = currentStage,
            nextStage = nextStage,
            completedKanji = completedKanji,
        )
    }

    fun getRecommendedNextBatch(
        batchSize: Int = 5,
        entries: List<KanjiEntry> = loadKnownEntries(),
    ): KanjiRoadmapRecommendation {
        val snapshot = buildSnapshot(entries)
        val currentStage = snapshot.currentStage
        if (currentStage == null) {
            return KanjiRoadmapRecommendation(stage = null, batch = emptyList())
        }

        val batch = entries
            .asSequence()
            .filter { roadmapStageIdForJlpt(it.jlptLevel) == currentStage.definition.id }
            .filterNot { snapshot.completedKanji.contains(it.kanji) }
            .sortedWith(
                compareBy<KanjiEntry> { it.grade ?: Int.MAX_VALUE }
                    .thenBy { it.frequency ?: Int.MAX_VALUE }
                    .thenBy { it.kanji }
            )
            .take(batchSize.coerceAtLeast(1))
            .toList()

        return KanjiRoadmapRecommendation(
            stage = currentStage,
            batch = batch,
        )
    }

    fun loadKnownEntries(): List<KanjiEntry> {
        val catalog = KanjiWidgetPrefs.getKanjiCatalog(context)
        return catalog.mapNotNull { kanji ->
            KanjiWidgetPrefs.getRemoteEntry(context, kanji)
        }
    }
}
