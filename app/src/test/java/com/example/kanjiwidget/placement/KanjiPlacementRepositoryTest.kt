package com.example.kanjiwidget.placement

import com.example.kanjiwidget.roadmap.KanjiRoadmapStageDefinition
import com.example.kanjiwidget.roadmap.KanjiRoadmapStageId
import org.junit.Assert.assertEquals
import org.junit.Test

class KanjiPlacementRepositoryTest {

    @Test
    fun resolveRecommendedStageScore_returnsHighestPassedStage() {
        val scores = listOf(
            stageScore(KanjiRoadmapStageId.N5_FOUNDATION, "N5", 0, 2, 2),
            stageScore(KanjiRoadmapStageId.N4_EXPANSION, "N4", 1, 2, 1),
            stageScore(KanjiRoadmapStageId.N3_CORE, "N3", 2, 2, 0),
        )

        val result = resolveRecommendedStageScore(scores)

        assertEquals("N4", result?.stage?.jlptLevel)
    }

    @Test
    fun resolveRecommendedStageScore_fallsBackToFirstAvailableStage() {
        val scores = listOf(
            stageScore(KanjiRoadmapStageId.N5_FOUNDATION, "N5", 0, 2, 0),
            stageScore(KanjiRoadmapStageId.N4_EXPANSION, "N4", 1, 2, 0),
        )

        val result = resolveRecommendedStageScore(scores)

        assertEquals("N5", result?.stage?.jlptLevel)
    }

    private fun stageScore(
        id: KanjiRoadmapStageId,
        jlpt: String,
        sortOrder: Int,
        total: Int,
        correct: Int,
    ): PlacementStageScore {
        return PlacementStageScore(
            stage = KanjiRoadmapStageDefinition(
                id = id,
                title = jlpt,
                jlptLevel = jlpt,
                gradeBandLabel = "Grades",
                sortOrder = sortOrder,
            ),
            totalQuestions = total,
            correctAnswers = correct,
        )
    }
}
