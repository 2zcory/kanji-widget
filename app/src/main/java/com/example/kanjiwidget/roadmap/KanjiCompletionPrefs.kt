package com.example.kanjiwidget.roadmap

import android.content.Context

object KanjiCompletionPrefs {
    private const val PREF = "kanji_roadmap_pref"
    private const val KEY_COMPLETED_KANJI = "completed_kanji"

    fun getCompletedKanji(context: Context): Set<String> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getStringSet(KEY_COMPLETED_KANJI, emptySet())
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    fun isCompleted(context: Context, kanji: String): Boolean {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return false
        return getCompletedKanji(context).contains(normalizedKanji)
    }

    fun setCompleted(context: Context, kanji: String, completed: Boolean) {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return

        val updated = getCompletedKanji(context).toMutableSet()
        if (completed) {
            updated.add(normalizedKanji)
        } else {
            updated.remove(normalizedKanji)
        }

        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit()
            .putStringSet(KEY_COMPLETED_KANJI, updated)
            .apply()
    }
}
