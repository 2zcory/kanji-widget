package com.example.kanjiwidget.widget

data class KanjiEntry(
    val kanji: String,
    val onyomi: String,
    val kunyomi: String,
    val meaning: String,
    val meaningVi: String? = null,
    val example: String,
    val jlptLevel: String,
    val unicode: String? = null,
    val strokeCount: Int? = null,
    val grade: Int? = null,
    val frequency: Int? = null,
    val source: String? = null,
    val lastUpdatedEpochMs: Long? = null,
) {
    companion object {
        const val JLPT_UNKNOWN = "N/A"
        const val MEANING_UNKNOWN = "__no_meaning__"
    }
}
