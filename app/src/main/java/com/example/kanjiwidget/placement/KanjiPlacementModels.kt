package com.example.kanjiwidget.placement

import com.example.kanjiwidget.roadmap.KanjiRoadmapStageDefinition
import com.example.kanjiwidget.roadmap.KanjiRoadmapStageId
import com.example.kanjiwidget.widget.KanjiEntry

enum class PlacementQuestionType {
    MEANING,
    READING,
}

data class PlacementQuestion(
    val id: String,
    val type: PlacementQuestionType,
    val stage: KanjiRoadmapStageDefinition,
    val promptKanji: String,
    val promptText: String,
    val options: List<String>,
    val correctAnswer: String,
    val entry: KanjiEntry,
)

data class PlacementAnswer(
    val questionId: String,
    val selectedAnswer: String,
)

data class PlacementTestSession(
    val questions: List<PlacementQuestion>,
)

data class PlacementStageScore(
    val stage: KanjiRoadmapStageDefinition,
    val totalQuestions: Int,
    val correctAnswers: Int,
)

data class PlacementResult(
    val recommendedStage: KanjiRoadmapStageDefinition,
    val confidenceLabel: String,
    val rationale: String,
    val stageScores: List<PlacementStageScore>,
    val totalCorrectAnswers: Int,
    val totalQuestions: Int,
)

internal val ROADMAP_STAGE_ORDER: List<KanjiRoadmapStageId> = listOf(
    KanjiRoadmapStageId.N5_FOUNDATION,
    KanjiRoadmapStageId.N4_EXPANSION,
    KanjiRoadmapStageId.N3_CORE,
    KanjiRoadmapStageId.N2_ADVANCED,
    KanjiRoadmapStageId.N1_EXPERT,
)
