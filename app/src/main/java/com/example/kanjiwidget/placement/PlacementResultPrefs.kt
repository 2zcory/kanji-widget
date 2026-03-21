package com.example.kanjiwidget.placement

import android.content.Context

data class SavedPlacementResult(
    val jlptLevel: String,
    val stageTitle: String,
    val gradeBand: String,
    val confidenceLabel: String,
    val totalCorrectAnswers: Int,
    val totalQuestions: Int,
)

object PlacementResultPrefs {
    private const val PREF = "placement_result_prefs"
    private const val KEY_JLPT = "last_jlpt"
    private const val KEY_TITLE = "last_title"
    private const val KEY_GRADE_BAND = "last_grade_band"
    private const val KEY_CONFIDENCE = "last_confidence"
    private const val KEY_TOTAL_CORRECT = "last_total_correct"
    private const val KEY_TOTAL_QUESTIONS = "last_total_questions"

    fun save(context: Context, result: SavedPlacementResult) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JLPT, result.jlptLevel)
            .putString(KEY_TITLE, result.stageTitle)
            .putString(KEY_GRADE_BAND, result.gradeBand)
            .putString(KEY_CONFIDENCE, result.confidenceLabel)
            .putInt(KEY_TOTAL_CORRECT, result.totalCorrectAnswers)
            .putInt(KEY_TOTAL_QUESTIONS, result.totalQuestions)
            .apply()
    }

    fun load(context: Context): SavedPlacementResult? {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val jlptLevel = prefs.getString(KEY_JLPT, null) ?: return null
        val stageTitle = prefs.getString(KEY_TITLE, null).orEmpty()
        val gradeBand = prefs.getString(KEY_GRADE_BAND, null).orEmpty()
        val confidenceLabel = prefs.getString(KEY_CONFIDENCE, null).orEmpty()
        val totalQuestions = prefs.getInt(KEY_TOTAL_QUESTIONS, 0)
        return SavedPlacementResult(
            jlptLevel = jlptLevel,
            stageTitle = stageTitle,
            gradeBand = gradeBand,
            confidenceLabel = confidenceLabel,
            totalCorrectAnswers = prefs.getInt(KEY_TOTAL_CORRECT, 0),
            totalQuestions = totalQuestions,
        )
    }
}
