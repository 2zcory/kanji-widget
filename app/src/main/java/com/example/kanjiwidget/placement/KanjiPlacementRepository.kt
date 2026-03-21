package com.example.kanjiwidget.placement

import android.content.Context
import com.example.kanjiwidget.R
import com.example.kanjiwidget.roadmap.KanjiRoadmapRepository
import com.example.kanjiwidget.roadmap.KanjiRoadmapStageDefinition
import com.example.kanjiwidget.roadmap.roadmapStageIdForJlpt
import com.example.kanjiwidget.widget.KanjiEntry

class KanjiPlacementRepository(
    private val context: Context,
    private val roadmapRepository: KanjiRoadmapRepository = KanjiRoadmapRepository(context),
    private val entryLoader: (() -> List<KanjiEntry>)? = null,
) {

    fun buildSession(): PlacementTestSession {
        val entries = loadKnownEntries()
        val questions = buildQuestions(entries)
        return PlacementTestSession(questions = questions)
    }

    fun evaluateSession(
        session: PlacementTestSession,
        answers: List<PlacementAnswer>,
    ): PlacementResult? {
        if (session.questions.isEmpty()) return null
        val answerMap = answers.associateBy { it.questionId }
        val stageScores = session.questions
            .groupBy { it.stage.id }
            .values
            .map { stageQuestions ->
                val stage = stageQuestions.first().stage
                val correctAnswers = stageQuestions.count { question ->
                    answerMap[question.id]?.selectedAnswer == question.correctAnswer
                }
                PlacementStageScore(
                    stage = stage,
                    totalQuestions = stageQuestions.size,
                    correctAnswers = correctAnswers,
                )
            }
            .sortedBy { it.stage.sortOrder }

        val recommendedStageScore = resolveRecommendedStageScore(stageScores)
            ?: return null

        val totalCorrect = stageScores.sumOf { it.correctAnswers }
        val totalQuestions = stageScores.sumOf { it.totalQuestions }.coerceAtLeast(1)
        val confidenceRatio = totalCorrect.toFloat() / totalQuestions.toFloat()

        return PlacementResult(
            recommendedStage = recommendedStageScore.stage,
            confidenceLabel = resolveConfidenceLabel(confidenceRatio),
            rationale = context.getString(
                R.string.placement_result_rationale,
                recommendedStageScore.correctAnswers,
                recommendedStageScore.totalQuestions,
                recommendedStageScore.stage.jlptLevel,
            ),
            stageScores = stageScores,
            totalCorrectAnswers = totalCorrect,
            totalQuestions = totalQuestions,
        )
    }

    private fun buildQuestions(entries: List<KanjiEntry>): List<PlacementQuestion> {
        val stageDefinitions = roadmapRepository.getStageDefinitions()
            .associateBy { it.id }
        val sanitizedEntries = entries
            .filter { it.kanji.isNotBlank() && it.meaning.isNotBlank() }
            .sortedWith(
                compareBy<KanjiEntry> { it.grade ?: Int.MAX_VALUE }
                    .thenBy { it.frequency ?: Int.MAX_VALUE }
                    .thenBy { it.kanji }
            )

        return ROADMAP_STAGE_ORDER.flatMap { stageId ->
            val definition = stageDefinitions[stageId] ?: return@flatMap emptyList()
            val stageEntries = sanitizedEntries.filter { roadmapStageIdForJlpt(it.jlptLevel) == stageId }
            val seedEntry = stageEntries.firstOrNull() ?: return@flatMap emptyList()
            buildList {
                buildMeaningQuestion(seedEntry, stageEntries, sanitizedEntries, definition)?.let(::add)
                buildReadingQuestion(seedEntry, stageEntries, sanitizedEntries, definition)?.let(::add)
            }
        }
    }

    private fun buildMeaningQuestion(
        entry: KanjiEntry,
        stageEntries: List<KanjiEntry>,
        allEntries: List<KanjiEntry>,
        stage: KanjiRoadmapStageDefinition,
    ): PlacementQuestion? {
        val correctAnswer = entry.meaning.trim()
        if (correctAnswer.isBlank()) return null
        val distractors = (stageEntries + allEntries)
            .asSequence()
            .map { it.meaning.trim() }
            .filter { it.isNotBlank() && !it.equals(correctAnswer, ignoreCase = true) }
            .distinct()
            .take(3)
            .toList()
        if (distractors.size < 3) return null
        return PlacementQuestion(
            id = "${stage.id}-meaning-${entry.kanji}",
            type = PlacementQuestionType.MEANING,
            stage = stage,
            promptKanji = entry.kanji,
            promptText = context.getString(R.string.placement_question_meaning_prompt, entry.kanji),
            options = (distractors + correctAnswer).sorted(),
            correctAnswer = correctAnswer,
            entry = entry,
        )
    }

    private fun buildReadingQuestion(
        entry: KanjiEntry,
        stageEntries: List<KanjiEntry>,
        allEntries: List<KanjiEntry>,
        stage: KanjiRoadmapStageDefinition,
    ): PlacementQuestion? {
        val correctAnswer = primaryReadingForUi(entry) ?: return null
        val distractors = (stageEntries + allEntries)
            .asSequence()
            .mapNotNull(::primaryReadingForUi)
            .filter { !it.equals(correctAnswer, ignoreCase = true) }
            .distinct()
            .take(3)
            .toList()
        if (distractors.size < 3) return null
        return PlacementQuestion(
            id = "${stage.id}-reading-${entry.kanji}",
            type = PlacementQuestionType.READING,
            stage = stage,
            promptKanji = entry.kanji,
            promptText = context.getString(R.string.placement_question_reading_prompt, entry.kanji),
            options = (distractors + correctAnswer).sorted(),
            correctAnswer = correctAnswer,
            entry = entry,
        )
    }

    private fun resolveConfidenceLabel(ratio: Float): String {
        return when {
            ratio >= 0.75f -> context.getString(R.string.placement_result_confidence_high)
            ratio >= 0.45f -> context.getString(R.string.placement_result_confidence_medium)
            else -> context.getString(R.string.placement_result_confidence_low)
        }
    }

    private fun loadKnownEntries(): List<KanjiEntry> {
        return entryLoader?.invoke() ?: roadmapRepository.loadKnownEntries()
    }

    private fun primaryReadingForUi(entry: KanjiEntry): String? {
        val onyomi = entry.onyomi.trim().takeIf { it.isNotBlank() }
        val kunyomi = entry.kunyomi.trim().takeIf { it.isNotBlank() }
        return onyomi ?: kunyomi
    }
}

internal fun resolveRecommendedStageScore(
    stageScores: List<PlacementStageScore>,
): PlacementStageScore? {
    return stageScores
        .sortedBy { it.stage.sortOrder }
        .lastOrNull { it.totalQuestions > 0 && it.correctAnswers * 2 >= it.totalQuestions }
        ?: stageScores.firstOrNull { it.totalQuestions > 0 }
}
